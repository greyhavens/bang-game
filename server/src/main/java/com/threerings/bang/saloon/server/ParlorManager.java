//
// $Id$

package com.threerings.bang.saloon.server;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Throttle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.chat.server.SpeakHandler;
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.bang.chat.server.BangChatManager;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorMarshaller;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Manages a back parlor room.
 */
public class ParlorManager extends PlaceManager
    implements SaloonCodes, ParlorProvider
{
    /**
     * Called by the {@link SaloonManager} after creating this back parlor.
     */
    public void init (SaloonManager salmgr, ParlorInfo info, String password)
    {
        _salmgr = salmgr;
        _parobj.setInfo(info);
        _password = password;
        log.info("Parlor created " + info + ".");

        // don't let server parlors shutdown
        if (info.server) {
            cancelShutdowner();
        }

        // create a table manager if we're not matched
        if (!info.matched) {
            _tmgr = new TableGameManager() {
                public void updateGameConfig (PlayerObject caller, ParlorGameConfig game)
                {
                    if (_parobj.onlyCreatorStart && !caller.handle.equals(_parobj.info.creator)) {
                        return;
                    }
                    super.updateGameConfig(caller, game);
                }
                public void startMatchMaking (PlayerObject caller, ParlorGameConfig game,
                                              byte[] bdata,
                                              InvocationService.ConfirmListener listener)
                    throws InvocationException
                {
                    if (_parobj.onlyCreatorStart && !caller.handle.equals(_parobj.info.creator)) {
                        throw new InvocationException(SaloonCodes.CREATOR_ONLY);
                    }
                    super.startMatchMaking(caller, game, bdata, listener);
                }
                public void startingGame (PlaceObject gameobj) {
                    ParlorManager.this.startingGame(gameobj);
                }
            };
            _parobj.setTableOid(_tmgr.getTableGameObject().getOid());
        }
    }

    /**
     * Ratifies the entry of the supplied player. Throws an invocation exception explaining the
     * reason for rejection if they do not meet the entry requirements.
     */
    public void ratifyEntry (PlayerObject user, String password)
        throws InvocationException
    {
        // if the parlor is shutting down or not yet initialized, fail
        if (_parobj == null || _parobj.info == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // if they're a power user, they're always allowed in
        if (_parobj.info.powerUser(user)) {
            return;
        }

        if (_bootSet.contains(user.playerId)) {
            throw new InvocationException(BOOTED);
        }

        switch (_parobj.info.type) {
        case PASSWORD:
            // make sure the password matches if we have a password
            if (password == null || !password.equalsIgnoreCase(_password)) {
                throw new InvocationException(INCORRECT_PASSWORD);
            }
            break;

        case PARDNERS_ONLY:
            // make sure they're a pardner of the creator if that is required
            PlayerObject creator = BangServer.locator.lookupPlayer(_parobj.info.creator);
            if (creator == null) {
                throw new InvocationException(CREATOR_NOT_ONLINE);
            }
            if (!creator.pardners.containsKey(user.handle)) {
                throw new InvocationException(NOT_PARDNER);
            }
            break;

        default:
            break; // nada
        }
    }

    // from interface ParlorProvider
    public void updateParlorConfig (PlayerObject user, ParlorInfo info, boolean onlyCreatorStart)
    {
        if (user.handle.equals(_parobj.info.creator) && info.type != ParlorInfo.Type.RECRUITING) {
            _parobj.startTransaction();
            try {
                info.creator = _parobj.info.creator;
                info.occupants = _parobj.occupantInfo.size();
                info.matched = _parobj.info.matched;
                if (!_parobj.info.equals(info)) {
                    _parobj.setInfo(info);
                    _salmgr.parlorUpdated(info);
                }
                _parobj.setOnlyCreatorStart(onlyCreatorStart);
            } finally {
                _parobj.commitTransaction();
            }
        }
    }

    // from interface ParlorProvider
    public void updateParlorPassword (PlayerObject user, String password)
    {
        if (user.handle.equals(_parobj.info.creator)) {
            _password = password;
        }
    }

    // from interface ParlorProvider
    public void findSaloonMatch (PlayerObject caller, Criterion criterion,
                                 ParlorService.ResultListener listener)
        throws InvocationException
    {
        _salmgr.findMatch(caller, criterion, listener);
    }

    // documentation inhertied from interface ParlorProvider
    public void leaveSaloonMatch (PlayerObject caller, int matchOid)
    {
        _salmgr.leaveMatch(caller, matchOid);
    }

    // from interface ParlorProvider
    public void bootPlayer (PlayerObject user, int bodyOid)
    {
        // only power users can boot
        if (!_parobj.info.powerUser(user)) {
            return;
        }

        BangOccupantInfo boi = (BangOccupantInfo)_occInfo.get(bodyOid);
        if (boi == null) {
            return;
        }

        PlayerObject other = BangServer.locator.lookupPlayer(boi.playerId);
        // and you can't boot a power user
        if (other == null || _parobj.info.powerUser(other)) {
            return;
        }

        SpeakUtil.sendAttention(other, SALOON_MSGS, "m.booted");
        BangServer.locman.moveBody(other, _salmgr.getLocation());
        _bootSet.add(boi.playerId);
    }

    /**
     * Called when a game is started from the parlor, so the parlor can keep itself alive
     * until the game finishes.
     */
    public void startingGame (PlaceObject gameobj) {
        if (_activeGames.add(gameobj.getOid())) {
            gameobj.addListener(_gameOverListener);

            // if the creator is still around, mark them as having hosted a game
            PlayerObject creator = BangServer.locator.lookupPlayer(_parobj.info.creator);
            if (creator != null) {
                creator.stats.incrementStat(StatType.GAMES_HOSTED, 1);
            }
        }
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new ParlorObject();
    }

    @Override // from PlaceManager
    protected long idleUnloadPeriod ()
    {
        // one minute idle period
        return 1 * 60 * 1000L;
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        _parobj = (ParlorObject)_plobj;
        _parobj.setService(BangServer.invmgr.registerProvider(this, ParlorMarshaller.class));
        _parobj.addListener(BangServer.playmgr.receivedChatListener);
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();

        // let the saloon manager know that we're audi
        _salmgr.parlorDidShutdown(this);

        log.info("Parlor shutdown " + _parobj.info + ".");

        // shutdown out table manager if we have one
        if (_tmgr != null) {
            _tmgr.shutdown();
            _tmgr = null;
        }

        // clear out our invocation service
        if (_parobj != null) {
            BangServer.invmgr.clearDispatcher(_parobj.service);
            _parobj.removeListener(BangServer.playmgr.receivedChatListener);
            _parobj = null;
        }
    }

    @Override // from PlaceManager
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // clear this player out of the game in case they were in it
        if (_tmgr != null) {
            _tmgr.clearPlayer(bodyOid);
        }

        _salmgr.clearPlayer(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    @Override // from PlaceManager
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // if a player disconnects during the matchmaking phase, remove them
        // from their pending match
        if (info.status == OccupantInfo.DISCONNECTED) {
            if (_tmgr != null) {
                _tmgr.clearPlayer(info.bodyOid);
            }

            _salmgr.clearPlayer(info.bodyOid);
        }
    }

    @Override // from PlaceManager
    protected SpeakHandler createSpeakHandler (PlaceObject plobj)
    {
        return new SpeakHandler(plobj, this) {
            @Override public void speak (ClientObject caller, String message, byte mode) {
                if (_chatmgr.validateChat(caller, message)) {
                    super.speak(caller, message, mode);
                }
            }
        };
    }

    @Override // from PlaceManager
    protected boolean shouldDeclareEmpty (OccupantInfo leaver)
    {
        return super.shouldDeclareEmpty(leaver) && !_parobj.info.server &&
            _activeGames.size() == 0;
    }

    protected void publishOccupants ()
    {
        if (!_throttle.throttleOp()) {
            _parobj.info.occupants = _parobj.occupantInfo.size();
            _salmgr.parlorUpdated(_parobj.info);
        }
    }

    /**
     * Called to check if we should shutdown.
     */
    protected void maybeShutdown ()
    {
        if (shouldDeclareEmpty(null)) {
            placeBecameEmpty();
        }
    }

    protected ObjectDeathListener _gameOverListener = new ObjectDeathListener() {
        public void objectDestroyed (ObjectDestroyedEvent event) {
            _activeGames.remove(event.getTargetOid());
            maybeShutdown();
        }
    };

    protected TableGameManager _tmgr;
    protected ParlorObject _parobj;
    protected SaloonManager _salmgr;
    protected String _password;
    protected Throttle _throttle = new Throttle(1, 10);
    protected ArrayIntSet _activeGames = new ArrayIntSet();
    protected ArrayIntSet _bootSet = new ArrayIntSet();

    @Inject protected BangChatManager _chatmgr;
}
