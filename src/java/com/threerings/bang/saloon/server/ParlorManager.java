//
// $Id$

package com.threerings.bang.saloon.server;

import com.samskivert.util.StringUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.game.data.BangConfig;

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
        // if this player is the creator, or an admin, let 'em in regardless
        if (user.handle.equals(_parobj.info.creator) ||
            user.tokens.isAdmin()) {
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
    public void startMatchMaking (ClientObject caller, ParlorGameConfig game)
    {
        // if we've already started, then just turn this into a join
        if (_parobj.playerOids != null) {
            joinMatch(caller);

        } else {
            // update the game config with the desired config
            _parobj.setGame(game);

            // create a playerOids array and stick the starter in slot zero
            int[] playerOids = new int[game.players];
            playerOids[0] = caller.getOid();
            _parobj.setPlayerOids(playerOids);
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
            if (_parobj.playerOids[ii] == 0) {
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

        // TODO: start a "start the game" timer if we're ready to go
    }

    // documentation inherited from interface ParlorProvider
    public void leaveMatch (ClientObject caller)
    {
        // make sure the match wasn't already cancelled
        if (_parobj.playerOids == null) {
            return;
        }

        // clear this player out of the list
        PlayerObject user = (PlayerObject)caller;
        int remain = 0;
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (_parobj.playerOids[ii] == user.getOid()) {
                _parobj.playerOids[ii] = 0;
            } else if (_parobj.playerOids[ii] > 0) {
                remain++;
            }
        }

        // either remove this player or cancel the match, depending
        if (remain == 0) {
            _parobj.setPlayerOids(null);
        } else {
            _parobj.setPlayerOids(_parobj.playerOids);
        }

        // TODO: cancel any game start timer
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return ParlorObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        return 5 * 1000L;
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

    protected ParlorObject _parobj;
    protected SaloonManager _salmgr;
    protected String _password;
}
