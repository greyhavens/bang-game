//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashMap;
import java.util.logging.Level;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonMarshaller;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Implements the server side of the Saloon.
 */
public class SaloonManager extends PlaceManager
    implements SaloonCodes, SaloonProvider
{
    // documentation inherited from interface SaloonProvider
    public void findMatch (ClientObject caller, Criterion criterion,
                          final SaloonService.ResultListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // look for an existing match that is compatible
        for (Match match : _matches.values()) {
            if (match.join(user, criterion)) {
                listener.requestProcessed(match.matchobj.getOid());

                // check to see if this match is ready to go
                switch (match.checkReady()) {
                case COULD_START:
                    // TODO: start the match after a short timeout
                    break;

                case START_NOW:
                    BangConfig config = match.createConfig();
                    try {
                        BangServer.plreg.createPlace(config, null);
                        // TODO: set a field in the match object indicating
                        // that the game is starting?
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Choked creating game " +
                                "[config=" + config + "].", e);
                    }
                    clearMatch(match);
                    break;
                }
                return;
            }
        }

        // otherwise we need to create a new match
        final Match match = new Match(user, criterion);
        BangServer.omgr.createObject(MatchObject.class, new Subscriber() {
            public void objectAvailable (DObject object) {
                match.setObject((MatchObject)object);
                log.info("Created match " + object.getOid());
                _matches.put(object.getOid(), match);
                listener.requestProcessed(object.getOid());
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to create match object " + cause + ".");
                listener.requestFailed(INTERNAL_ERROR);
            }
        });
    }

    // documentation inherited from interface SaloonProvider
    public void leaveMatch (ClientObject caller, int matchOid)
    {
        Match match = _matches.get(matchOid);
        if (match != null) {
            clearPlayerFromMatch(match, caller.getOid());
        }
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return SaloonObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _salobj = (SaloonObject)_plobj;
        _salobj.setService((SaloonMarshaller)
                           BangServer.invmgr.registerDispatcher(
                               new SaloonDispatcher(this), false));
    }

    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // clear this player out of any match they might have been in
        for (Match match : _matches.values()) {
            if (clearPlayerFromMatch(match, bodyOid)) {
                break;
            }
        }
    }

    @Override // documentation inherited
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        log.info("Updated " + info);

        // if a player disconnects during the matchmaking phase, remove them
        // from their pending match
        if (info.status == OccupantInfo.DISCONNECTED) {
            for (Match match : _matches.values()) {
                if (clearPlayerFromMatch(match, info.bodyOid)) {
                    break;
                }
            }
        }
    }

    protected boolean clearPlayerFromMatch (Match match, int playerOid)
    {
        if (match.remove(playerOid)) {
            if (match.getPlayerCount() == 0) {
                clearMatch(match);
            }
            return true;
        }
        return false;
    }

    protected void clearMatch (Match match)
    {
        int moid = match.matchobj.getOid();
        BangServer.omgr.destroyObject(moid);
        _matches.remove(moid);
        log.info("Cleared match " + moid);
    }

    protected SaloonObject _salobj;
    protected HashMap<Integer,Match> _matches = new HashMap<Integer,Match>();
}
