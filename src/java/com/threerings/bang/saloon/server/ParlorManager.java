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
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator) ||
            !_parobj.onlyCreatorStart) {
            _parobj.setGame(game);
        }
    }

    // documentation inherited from interface ParlorProvider
    public void startMatchMaking (ClientObject caller, ParlorGameConfig game)
    {
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
