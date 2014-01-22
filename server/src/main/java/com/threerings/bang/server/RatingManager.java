//
// $Id$

package com.threerings.bang.server;

import java.sql.Date;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.server.persist.RatingRepository.RankLevels;
import com.threerings.bang.server.persist.RatingRepository.TrackerKey;
import com.threerings.bang.server.persist.RatingRepository;

import static com.threerings.bang.Log.*;

/**
 * Manages rating bits.
 */
@Singleton
public class RatingManager
    implements Lifecycle.ShutdownComponent
{
    @Inject public RatingManager (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    /**
     * Prepares the rating manager for operation.
     */
    public void init ()
        throws PersistenceException
    {
        // load up our scoring percentile trackers
        _trackers = new HashMap<TrackerKey, Percentiler>();
        _ratingrepo.loadScoreTrackers(_trackers);

        // if we're a town server, queue up an interval to periodically grind our ratings tables
        // and one to sync our score trackers
        if (ServerConfig.isTownServer) {
            createRankRecalculateInterval();
            createTrackerSyncInterval();
            if (BangCodes.FRONTIER_TOWN.equals(ServerConfig.townId)) {
                createRatingsPurgeInterval();
            }
        }

        // create the interval to reload the ranks for all rating types
        createRankReloadInterval();
    }

    /**
     * Given a numeric rating, returns its rank level.  This method must be thread-safe, as it is
     * called from both the dobj and the invoker thread.
     */
    public int getRank (String type, int rating)
    {
        RankLevels levels = _rankLevels.get(type);
        return (levels == null) ? 0 : levels.getRank(rating);
    }

    /**
     * Returns the percentile occupied by the specified score value in the specified scenario with
     * the given number of players.
     *
     * @param record if true, the score will be recorded to the percentiler as
     * a data point.
     */
    public int getPercentile (
        String scenario, int players, int score, boolean record)
    {
        TrackerKey tkey = new TrackerKey(scenario, players);
        Percentiler tracker = _trackers.get(tkey);
        if (tracker == null) {
            tracker = new Percentiler();
            _trackers.put(tkey, tracker);
        }

        int pct = tracker.getPercentile(score);
        if (record) {
            tracker.recordValue(score);
        }
        return pct;
    }

    // from Lifecycle.ShutdownComponent
    public void shutdown ()
    {
        log.info("Rating manager shutting down.");
        syncTrackers();
    }

    /**
     * Creates the interval that regrinds our ratings table and produces the
     * rank distributions every six hours.
     */
    protected void createRankRecalculateInterval ()
    {
        final Invoker.Unit grinder = new Invoker.Unit("rankGrinder") {
            public boolean invoke () {
                try {
                    log.info("Recalculating rankings...");
                    _ratingrepo.calculateRanks(null);
                } catch (PersistenceException pe) {
                    log.warning("Failed to recalculate ranks.", pe);
                }
                return false;
            }
        };

//         final Invoker.Unit weekGrinder = new Invoker.Unit("rankGrinder") {
//             public boolean invoke () {
//                 Date week = getGrindWeek();
//                 try {
//                     log.info("Recalculating weekly rankings...");
//                     _ratingrepo.calculateRanks(week);
//                 } catch (PersistenceException pe) {
//                     log.warning("Failed to recalculate weekly ranks", "week", week, pe);
//                 }
//                 return false;
//             }
//         };

        // regrind 5 minutes after reboot and then every six hours
        new Interval(BangServer.omgr) {
            public void expired () {
                BangServer.invoker.postUnit(grinder);
            }
        }.schedule(5 * 60 * 1000L, 6 * 60 * 60 * 1000L);
    }

    /**
     * Creates the interval that purges old weekly ratings and rank information.
     */
    protected void createRatingsPurgeInterval ()
    {
        final Invoker.Unit purger = new Invoker.Unit("ratingPurger") {
            public boolean invoke () {
                Date week = getPurgeWeek();
                try {
                    _ratingrepo.deleteRatings(week);
                } catch (PersistenceException pe) {
                    log.warning("Failed to purge weekly ratings", "week", week, pe);
                }
                return false;
            }
        };

        // purge 20 minutes after reboot then once a week
        new Interval(BangServer.omgr) {
            public void expired () {
                BangServer.invoker.postUnit(purger);
            }
        }.schedule(20 * 60 * 1000L, 7 * 24 * 60 * 60 * 1000L);
    }

    /**
     * Returns the date used when grinding a weeks rankings.
     */
    protected Date getGrindWeek ()
    {
        // find the first Sunday before today
        Calendar week = Calendar.getInstance();
        week.setFirstDayOfWeek(Calendar.SUNDAY);
        week.add(Calendar.DAY_OF_WEEK, -1);
        week.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return new Date(week.getTimeInMillis());
    }

    /**
     * Returns the date used when purging a weeks rankings.
     */
    protected Date getPurgeWeek ()
    {
        // find the first Sunday before today
        Calendar week = Calendar.getInstance();
        week.setFirstDayOfWeek(Calendar.SUNDAY);
        week.add(Calendar.MONTH, -1);
        week.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return new Date(week.getTimeInMillis());
    }

    /**
     * Creates the interval that reloads the rank levels for all rating types every hour.
     */
    public void createRankReloadInterval ()
    {
        // reload soon after startup, then every hour
        new Interval(BangServer.omgr) {
            public void expired () {
                reloadRanks();
            }
        }.schedule(1000L, 60 * 60 * 1000L);
    }

    /**
     * (Re)loads the rank data for all rating types.
     */
    protected void reloadRanks ()
    {
        BangServer.invoker.postUnit(new Invoker.Unit("rankLoader") {
            public boolean invoke()  {
                HashMap<String, RankLevels> newMap = new HashMap<String, RankLevels>();
                try {
                    for (RankLevels levels : _ratingrepo.loadRanks(null)) {
                        newMap.put(levels.type, levels);
                    }
                    _rankLevels = newMap;
                } catch (PersistenceException pe) {
                    log.warning("Failure while reloading rank data", pe);
                }
                return false;
            }
        });
    }

    /**
     * Creates the interval that synchronizes our score trackers with the
     * database every hour.
     */
    protected void createTrackerSyncInterval ()
    {
        // resync every hour
        new Interval(BangServer.omgr) {
            public void expired () {
                syncTrackers();
            }
        }.schedule(60 * 60 * 1000L);
    }

    /**
     * Stores the trackers in the database.
     */
    protected void syncTrackers ()
    {
        final int tcount = _trackers.size();
        final TrackerKey[] keys = new TrackerKey[tcount];
        final Percentiler[] tilers = new Percentiler[tcount];
        int ii = 0;
        for (Map.Entry<TrackerKey, Percentiler> entry : _trackers.entrySet()) {
            keys[ii] = entry.getKey();
            tilers[ii++] = entry.getValue();
        }

        // write out the performance distributions
        BangServer.invoker.postUnit(new Invoker.Unit("storeScoreTrackers") {
            public boolean invoke () {
                for (int ii = 0; ii < tcount; ii++) {
                    try {
                        _ratingrepo.storeScoreTracker(keys[ii], tilers[ii]);
                    } catch (PersistenceException pe) {
                        log.warning("Error storing perf dist", "scenario", keys[ii].scenario,
                                    "players", keys[ii].players, "error", pe);
                    }
                }
                return false;
            }
        });
    }

    /** Provides access to the rating database. */
    @Inject protected RatingRepository _ratingrepo;

    /** Score percentile trackers. */
    protected HashMap<TrackerKey, Percentiler> _trackers;

    /** A map of rating types to rank levels, reloaded every so often */
    protected volatile HashMap<String, RankLevels> _rankLevels = new HashMap<String, RankLevels>();

    /** When we should next reload our rank levels */
    protected long _nextRankReload;
}
