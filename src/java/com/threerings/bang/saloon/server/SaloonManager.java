//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.ParlorConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonMarshaller;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankedList;

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

        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(NEW_GAMES_DISABLED);
        }

        // sanity check the criterion, force at least 2 players, 1 round, and
        // match zero AIs if nothing was selected
        if (criterion.players == 0) {
            criterion.players = 1;
        }
        if (criterion.rounds == 0) {
            criterion.rounds = 1;
        }
        if (criterion.allowAIs == 0) {
            criterion.allowAIs = 1;
        }

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
        Match match = new Match(user, criterion);
        match.setObject(BangServer.omgr.registerObject(new MatchObject()));
        match.matchobj.setSpeakService((SpeakMarshaller)
            BangServer.invmgr.registerDispatcher(
                new SpeakDispatcher(new SpeakProvider(match.matchobj, null)),
                false));
        _matches.put(match.matchobj.getOid(), match);
        BangServer.adminmgr.statobj.setPendingMatches(_matches.size());
        listener.requestProcessed(match.matchobj.getOid());
        checkReadiness(match);
    }

    // documentation inherited from interface SaloonProvider
    public void leaveMatch (ClientObject caller, int matchOid)
    {
        Match match = _matches.get(matchOid);
        if (match != null) {
            clearPlayerFromMatch(match, caller.getOid());

            // if the match is queued to be started and is no longer ready,
            // cancel its starter interval
            if (match.checkReady() == Match.Readiness.NOT_READY &&
                match.starter != null) {
                match.starter.cancel();
                match.starter = null;
                match.matchobj.setStarting(false);
            }
        }
    }

    // documentation inherited from interface SaloonProvider
    public void createParlor (
        ClientObject caller, boolean pardnersOnly, final String password,
        final SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        // make sure this player doesn't already have a parlor created
        if (_parlors.containsKey(user.handle)) {
            throw new InvocationException(ALREADY_HAVE_PARLOR);
        }

        // create the new parlor
        final ParlorInfo info = new ParlorInfo();
        info.creator = user.handle;
        info.pardnersOnly = pardnersOnly;
        info.passwordProtected = !StringUtil.isBlank(password);

        try {
            ParlorManager parmgr = (ParlorManager)
                BangServer.plreg.createPlace(new ParlorConfig());
            ParlorObject parobj = (ParlorObject)parmgr.getPlaceObject();
            parmgr.init(SaloonManager.this, info, password);
            _parlors.put(info.creator, parmgr);
            _salobj.addToParlors(info);
            rl.requestProcessed(parobj.getOid());

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create parlor " + info + ".", e);
            rl.requestFailed(INTERNAL_ERROR);
        }
    }

    // documentation inherited from interface SaloonProvider
    public void joinParlor (ClientObject caller, Handle creator,
                            String password, SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        // locate the parlor in question
        ParlorManager parmgr = _parlors.get(creator);
        if (parmgr == null) {
            throw new InvocationException(NO_SUCH_PARLOR);
        }

        // make sure they meet the entry requirements
        parmgr.ratifyEntry(user, password);

        // they've run the gauntlet, let 'em in
        rl.requestProcessed(parmgr.getPlaceObject().getOid());
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new SaloonObject();
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

        // start up our top-ranked list refresher interval
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked();
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // clear out our invocation service
        if (_salobj != null) {
            BangServer.invmgr.clearDispatcher(_salobj.service);
            _salobj = null;
        }

        // stop our top-ranked list refresher
        if (_rankval != null) {
            _rankval.cancel();
            _rankval = null;
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
     * Checks whether a match is ready.
     */
    protected void checkReadiness (final Match match)
    {
        // check to see if this match is ready to go
        switch (match.checkReady()) {
        case COULD_START:
            // the match may already be queued up for an eventual start, but we
            // just added a player, so let's reset the timer (we may end up in
            // the game faster if two players join in rapid succession)
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
        }
    }

    /**
     * Called when a match should be started. Marks the match as such and waits
     * a few second before actually starting the match to give participants a
     * chance to see who the last joiner was and potentially balk.
     */
    protected void startMatch (final Match match)
    {
        // cancel any "could_start" starter as we will replace it with a
        // "starting" starter
        if (match.starter != null) {
            match.starter.cancel();
        }
        match.matchobj.setStarting(true);
        match.starter = new Interval(BangServer.omgr) {
            public void expired () {
                // make sure the match is still ready (this shouldn't happen as
                // we cancel matches that become non-ready, but there are edge
                // cases where we might not get canceled in time)
                if (match.checkReady() == Match.Readiness.NOT_READY) {
                    match.starter = null;
                    match.matchobj.setStarting(false);
                    return;
                }
                // go like the wind!
                BangConfig config = match.createConfig();
                try {
                    BangManager mgr = (BangManager)
                        BangServer.plreg.createPlace(config);
                    mgr.setPriorLocation("saloon", _salobj.getOid());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Choked creating game " +
                            "[config=" + config + "].", e);
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

    protected void parlorUpdated (ParlorInfo info)
    {
        _salobj.updateParlors(info);
    }

    protected void parlorDidShutdown (ParlorManager parmgr)
    {
        ParlorObject parobj = (ParlorObject)parmgr.getPlaceObject();
        Handle creator = parobj.info.creator;
        _parlors.remove(creator);
        _salobj.removeFromParlors(creator);
    }

    protected void refreshTopRanked ()
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                ArrayList<String> scens = new ArrayList<String>();
                CollectionUtil.addAll(
                    scens, ScenarioInfo.getScenarioIds(
                        ServerConfig.townId, false));
                scens.add(0, ScenarioInfo.OVERALL_IDENT);

                try {
                    _lists = BangServer.ratingrepo.loadTopRanked(
                        scens.toArray(new String[scens.size()]),
                        TOP_RANKED_LIST_SIZE);
                    return true;

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load top-ranked " +
                            "players.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (_salobj == null) {
                    return;
                }
                for (TopRankedList list : _lists) {
                    commitTopRanked(list);
                }
            }

            protected ArrayList<TopRankedList> _lists;
        });
    }

    protected void commitTopRanked (final TopRankedList list)
    {
        list.criterion = MessageBundle.qualify(
            GameCodes.GAME_MSGS, "m.scenario_" + list.criterion);
        int topRankId = (list.playerIds == null ||
                         list.playerIds.length == 0) ? 0 : list.playerIds[0];
        BangServer.barbermgr.getSnapshot(
            topRankId, new ResultListener<int[]>() {
            public void requestCompleted (int[] snapshot) {
                list.topDogSnapshot = snapshot;
                commitList();
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to obtain top-ranked player " +
                        "snapshot [list=" + list + "].", cause);
                // ah well, we'll have no avatar
                commitList();
            }
            protected void commitList () {
                if (_salobj.topRanked.containsKey(list.criterion)) {
                    _salobj.updateTopRanked(list);
                } else {
                    _salobj.addToTopRanked(list);
                }
            }
        });
    }

    protected SaloonObject _salobj;
    protected HashMap<Integer,Match> _matches = new HashMap<Integer,Match>();
    protected Interval _rankval;

    protected HashMap<Handle,ParlorManager> _parlors =
        new HashMap<Handle,ParlorManager>();

    /** The delay between reporting that we're going to start a match and the
     * time that we actually start it. */
    protected static final long START_DELAY = 5000L;

    /** The frequency with which we update the top-ranked player lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked player lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
