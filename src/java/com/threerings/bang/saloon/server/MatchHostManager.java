//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.util.Interval;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;

import com.threerings.bang.game.data.BangConfig;

import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Base manager for places that host matched games.
 */
public abstract class MatchHostManager extends ShopManager
    implements SaloonCodes
{
    /**
     * Finds or creates a match with the specified criteria.
     */
    public void findMatch (ClientObject caller, Criterion criterion,
                          final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(NEW_GAMES_DISABLED);
        }

        // sanity check the criterion
        checkCriterion(criterion);

        // look for an existing match that is compatible
        for (Match match : _matches.values()) {
            // don't allow players to join matches that are about to start
            if (match.matchobj.starting) {
                continue;
            }

            if (match.join(user, criterion)) {
                listener.requestProcessed(match.matchobj.getOid());
                checkReadiness(match);
                return;
            }
        }

        // otherwise we need to create a new match
        Match match = createMatch(user, criterion);
        match.setObject(BangServer.omgr.registerObject(new MatchObject()));
        match.matchobj.setSpeakService(
            (SpeakMarshaller)BangServer.invmgr.registerDispatcher(
                new SpeakDispatcher(new SpeakProvider(match.matchobj, null))));
        _matches.put(match.matchobj.getOid(), match);
        BangServer.adminmgr.statobj.setPendingMatches(_matches.size());
        listener.requestProcessed(match.matchobj.getOid());
        checkReadiness(match);
    }

    /**
     * Leaves the identified match.
     */
    public void leaveMatch (ClientObject caller, int matchOid)
    {
        Match match = _matches.get(matchOid);
        if (match != null) {
            clearPlayerFromMatch(match, caller.getOid());
            checkReadiness(match);
        }
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

    /**
     * Enforces sanity checks on the given criterion.
     */
    protected void checkCriterion (Criterion criterion)
    {
        // force at least 2 players, 1 round, and match zero AIs if nothing was selected
        if (criterion.players == 0) {
            criterion.players = 1;
        }
        if (criterion.rounds == 0) {
            criterion.rounds = 1;
        }
        if (criterion.allowAIs == 0) {
            criterion.allowAIs = 1;
        }
    }

    /**
     * Creates a match object for the given user and criterion.
     */
    protected Match createMatch (PlayerObject user, Criterion criterion)
    {
        return new Match(user, criterion);
    }

    /**
     * Checks whether a match is ready.
     */
    protected void checkReadiness (final Match match)
    {
        // check to see if this match is ready to go
        switch (match.checkReady()) {
        case COULD_START:
            // the match may already be queued up for an eventual start, but we just added a
            // player, so let's reset the timer (we may end up in the game faster if two players
            // join in rapid succession)
            if (match.starter != null) {
                match.starter.cancel();
            }
            match.starter = new Interval(BangServer.omgr) {
                public void expired () {
                    match.starter = null;
                    if (match.checkReady() != Match.Readiness.NOT_READY) {
                        log.info("Starting " + match + ".");
                        startMatch(match);
                    }
                }
            };
            match.starter.schedule(match.getWaitForOpponentsDelay());
            break;

        case START_NOW:
            startMatch(match);
            break;

        case NOT_READY:
            // if the match is queued to be started and is no longer ready, cancel its starter
            match.setStarting(false);
        }
    }

    /**
     * Called when a match should be started. Marks the match as such and waits a few second before
     * actually starting the match to give participants a chance to see who the last joiner was and
     * potentially balk.
     */
    protected void startMatch (final Match match)
    {
        // cancel any "could_start" starter as we will replace it with a "starting" starter
        if (match.starter != null) {
            match.starter.cancel();
        }

        match.setStarting(true);
        match.starter = new Interval(BangServer.omgr) {
            public void expired () {
                // make sure the match is still ready (this shouldn't happen as we cancel matches
                // that become non-ready, but there are cases where we might not get canceled
                // in time)
                if (match.checkReady() == Match.Readiness.NOT_READY) {
                    match.setStarting(false);
                    return;
                }
                // go like the wind!
                BangConfig config = match.createConfig();
                try {
                    BangServer.plreg.createPlace(config);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Choked creating game " + config + ".", e);
                }
                clearMatch(match);
            }
        };
        match.starter.schedule(START_DELAY);
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
        if (_matches.remove(moid) == null) {
            return; // don't doubly clear a match
        }
        if (match.matchobj.speakService != null) {
            BangServer.invmgr.clearDispatcher(match.matchobj.speakService);
        }
        BangServer.omgr.destroyObject(moid);
        BangServer.adminmgr.statobj.setPendingMatches(_matches.size());
    }

    protected HashMap<Integer,Match> _matches = new HashMap<Integer,Match>();

    /** The delay between reporting that we're going to start a match and the
     * time that we actually start it. */
    protected static final long START_DELAY = 5000L;
}
