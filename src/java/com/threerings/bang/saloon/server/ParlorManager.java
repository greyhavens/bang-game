//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashSet;
import java.util.logging.Level;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;

import com.threerings.media.util.MathUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.saloon.client.ParlorService;
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
    }

    /**
     * Ratifies the entry of the supplied player. Throws an invocation
     * exception explaining the reason for rejection if they do not meet the
     * entry requirements.
     */
    public void ratifyEntry (PlayerObject user, String password)
        throws InvocationException
    {
        // if this player is the creator, or an admin/support, let 'em in regardless
        if (user.handle.equals(_parobj.info.creator) || user.tokens.isSupport()) {
            return;
        }

        // make sure the password matches if we have a password
        if (_parobj.info.passwordProtected &&
            !password.equalsIgnoreCase(_password)) {
            throw new InvocationException(INCORRECT_PASSWORD);
        }

        // make sure they're a pardner of the creator if that is required
        if (_parobj.info.pardnersOnly) {
            PlayerObject creator =
                BangServer.lookupPlayer(_parobj.info.creator);
            if (creator == null) {
                throw new InvocationException(CREATOR_NOT_ONLINE);
            }
            if (!creator.pardners.containsKey(user.handle)) {
                throw new InvocationException(NOT_PARDNER);
            }
        }
    }

    // documentation inherited from interface ParlorProvider
    public void updateParlorConfig (
        ClientObject caller, ParlorInfo info, boolean onlyCreatorStart)
    {
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator)) {
            _parobj.startTransaction();
            try {
                info.creator = _parobj.info.creator;
                info.occupants = _parobj.occupantInfo.size();
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

    // documentation inherited from interface ParlorProvider
    public void updateParlorPassword (ClientObject caller, String password)
    {
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator)) {
            _password = password;
        }
    }

    // documentation inherited from interface ParlorProvider
    public void updateGameConfig (ClientObject caller, ParlorGameConfig game)
    {
        // if we're already matchmaking, reject any config updates
        if (_parobj.playerOids != null) {
            return;
        }

        // otherwise just make sure they have the necessary privileges
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator) ||
            !_parobj.onlyCreatorStart) {
            _parobj.setGame(game);
        }
    }

    // documentation inherited from interface ParlorProvider
    public void startMatchMaking (
        ClientObject caller, ParlorGameConfig game, byte[] bdata,
        ParlorService.InvocationListener listener)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }
        PlayerObject user = (PlayerObject)caller;

        // if we've already started, then just turn this into a join
        if (_parobj.playerOids != null) {
            joinMatch(caller);

        } else {
            // sanity check the configuration
            int minPlayers = (game.tinCans > 0) ? 1 : 2;
            game.players = MathUtil.bound(
                minPlayers, game.players, GameCodes.MAX_PLAYERS);
            game.rounds = MathUtil.bound(
                1, game.rounds, GameCodes.MAX_ROUNDS);
            game.teamSize = MathUtil.bound(
                1, game.teamSize, GameCodes.MAX_TEAM_SIZE);
            game.tinCans = MathUtil.bound(
                0, game.tinCans, GameCodes.MAX_PLAYERS - game.players);
            if (game.scenarios == null || game.scenarios.length == 0) {
                game.scenarios = ScenarioInfo.getScenarioIds(
                    ServerConfig.townId, false);
            }

            // update the game config with the desired config
            _parobj.setGame(game);

            // if this player is an admin, allow the board data
            if (user.tokens.isAdmin()) {
                _bdata = bdata;
            }

            // create a playerOids array and stick the starter in slot zero
            int[] playerOids = new int[game.players];
            playerOids[0] = caller.getOid();
            _parobj.setPlayerOids(playerOids);

            // start a "start the game" timer if we're ready to go
            checkStart();
        }
    }

    // documentation inherited from interface ParlorProvider
    public void joinMatch (ClientObject caller)
    {
        // make sure the match wasn't cancelled
        if (_parobj.playerOids == null) {
            return;
        }

        // look for a spot, and make sure they're not already joined
        PlayerObject user = (PlayerObject)caller;
        int idx = -1;
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (idx == -1 && _parobj.playerOids[ii] == 0) {
                idx = ii;
            }
            if (_parobj.playerOids[ii] == user.getOid()) {
                // abort!
                return;
            }
        }

        if (idx != -1) {
            _parobj.playerOids[idx] = user.getOid();
            _parobj.setPlayerOids(_parobj.playerOids);
        }

        // start a "start the game" timer if we're ready to go
        checkStart();
    }

    // documentation inherited from interface ParlorProvider
    public void leaveMatch (ClientObject caller)
    {
        clearPlayer(caller.getOid());
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new ParlorObject();
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // one minute idle period
        return 1 * 60 * 1000L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _parobj = (ParlorObject)_plobj;
        _parobj.setService((ParlorMarshaller)
                           BangServer.invmgr.registerDispatcher(
                               new ParlorDispatcher(this), false));
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // let the saloon manager know that we're audi
        _salmgr.parlorDidShutdown(this);

        log.info("Parlor shutdown " + _parobj.info + ".");

        // clear out our invocation service
        if (_parobj != null) {
            BangServer.invmgr.clearDispatcher(_parobj.service);
            _parobj = null;
        }
    }

    @Override // documentation inherited
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // clear this player out of the game in case they were in it
        clearPlayer(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    protected void publishOccupants ()
    {
        if (!_throttle.throttleOp()) {
            _parobj.info.occupants = _parobj.occupantInfo.size();
            _salmgr.parlorUpdated(_parobj.info);
        }
    }

    protected void clearPlayer (int playerOid)
    {
        // make sure the match wasn't already cancelled
        if (_parobj.playerOids == null) {
            return;
        }

        // clear this player out of the list
        boolean cleared = false;
        int remain = 0;
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (_parobj.playerOids[ii] == playerOid) {
                _parobj.playerOids[ii] = 0;
                cleared = true;
            } else if (_parobj.playerOids[ii] > 0) {
                remain++;
            }
        }

        // if we didn't find this player in the list, stop here
        if (!cleared) {
            return;
        }

        // either remove this player or cancel the match, depending
        if (remain == 0) {
            _parobj.setPlayerOids(null);
        } else {
            _parobj.setPlayerOids(_parobj.playerOids);
        }

        // cancel our game start timer
        if (_starter != null) {
            _parobj.setStarting(false);
            _starter.cancel();
            _starter = null;
        }
    }

    protected void checkStart ()
    {
        if (readyToStart() && _starter == null) {
            _parobj.setStarting(true);
            _starter = new Interval(BangServer.omgr) {
                public void expired () {
                    if (_starter != this) {
                        return;
                    }
                    _starter = null;
                    if (readyToStart()) {
                        log.info("Starting " + _parobj.game + ".");
                        startMatch();
                    }
                }
            };
            _starter.schedule(SaloonManager.START_DELAY);
        }
    }

    protected boolean readyToStart ()
    {
        return (IntListUtil.indexOf(_parobj.playerOids, 0) == -1);
    }

    protected void startMatch ()
    {
        BangConfig config = new BangConfig();

        // we can use these values directly as we sanity checked them earlier
        config.init(_parobj.game.players + _parobj.game.tinCans, _parobj.game.teamSize);
        config.players = new Handle[config.plist.size()];
        config.duration = _parobj.game.duration;
        config.speed = _parobj.game.speed;
        config.rated = false; // back parlor games are never rated

        // configure our rounds
        for (int ii = 0; ii < _parobj.game.rounds; ii++) {
            config.addRound(RandomUtil.pickRandom(_parobj.game.scenarios), null, _bdata);
        }

        // fill in the human players
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            PlayerObject user = (PlayerObject)BangServer.omgr.getObject(_parobj.playerOids[ii]);
            if (user == null) {
                log.warning("Zoiks! Missing player for parlor match [game=" + _parobj.game +
                            ", oid=" + _parobj.playerOids[ii] + "].");
                return; // abandon ship
            }
            config.players[ii] = user.handle;
        }

        // add our ais (if any)
        config.ais = new BangAI[config.players.length];
        HashSet<String> names = new HashSet<String>();
        for (int ii = _parobj.playerOids.length; ii < config.ais.length; ii++) {
            // TODO: sort out personality and skill
            BangAI ai = BangAI.createAI(1, 50, names);
            config.ais[ii] = ai;
            config.players[ii] = ai.handle;
        }

        try {
            BangManager mgr = (BangManager)BangServer.plreg.createPlace(config);
            PlaceObject gameobj = mgr.getPlaceObject();
            _activeGames.add(gameobj.getOid());
            gameobj.addListener(_gameOverListener);
        } catch (Exception e) {
            log.log(Level.WARNING, "Choked creating game " + config + ".", e);
        }

        // and clear out our parlor bits
        _parobj.setStarting(false);
        _parobj.setPlayerOids(null);
        _parobj.setGame(null);
        _bdata = null;
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

    @Override // documentation inherited
    protected boolean shouldDeclareEmpty (OccupantInfo leaver)
    {
        return super.shouldDeclareEmpty(leaver) && _activeGames.size() == 0;
    }


    protected ParlorObject _parobj;
    protected SaloonManager _salmgr;
    protected byte[] _bdata;
    protected String _password;
    protected Interval _starter;
    protected Throttle _throttle = new Throttle(1, 10);
    protected ArrayIntSet _activeGames = new ArrayIntSet();

    protected ObjectDeathListener _gameOverListener =
       new ObjectDeathListener() {
           public void objectDestroyed (ObjectDestroyedEvent event) {
               _activeGames.remove(event.getTargetOid());
               maybeShutdown();
           }
       }; 
}
