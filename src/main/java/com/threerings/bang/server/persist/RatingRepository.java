//
// $Id$

package com.threerings.bang.server.persist;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntTuple;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Rating;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.saloon.data.TopRankedList;
import com.threerings.bang.server.ServerConfig;

/**
 * Responsible for the persistent storage of per-player ratings.
 */
@Singleton
public class RatingRepository extends SimpleRepository
{
    /** Keeps track of the rating levels for each rank for a given rating type. */
    public static class RankLevels
    {
        public String type;
        public int[] levels = new int[RANK_PERCENTAGES.length];

        public int getRank (int rating)
        {
            int rank = levels.length;
            while (rank > 0 && rating < levels[rank-1]) {
                rank -= 1;
            }
            return rank;
        }

        protected RankLevels (String type)
        {
            this.type = type;
        }
    }

    /** Identifies a score tracker by scenario and number of players. */
    public static class TrackerKey
    {
        public String scenario;
        public int players;

        public TrackerKey (String scenario, int players)
        {
            this.scenario = scenario;
            this.players = players;
        }

        public int hashCode ()
        {
            return scenario.hashCode() + players;
        }

        public boolean equals (Object other)
        {
            TrackerKey okey = (TrackerKey)other;
            return scenario.equals(okey.scenario) && players == okey.players;
        }
    }

    public static String whereWeek (Date week)
    {
        return "WEEK " + (week == null ? "IS NULL" : " ='" + week + "'");
    }

    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>ratingdb</code>.
     */
    public static final String RATING_DB_IDENT = "ratingdb";

    /**
     * Constructs a new ratings repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    @Inject public RatingRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, RATING_DB_IDENT);
    }

    /**
     * Loads the ratings for the specified player.
     */
    public HashMap<String, Rating> loadRatings (final int playerId, final Date week)
        throws PersistenceException
    {
        final HashMap<String, Rating> rats = new HashMap<String, Rating>();
        final String query = "select SCENARIO, RATING, EXPERIENCE " +
            "from RATINGS where PLAYER_ID = " + playerId + " and " + whereWeek(week);
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        Rating rat = new Rating();
                        rat.scenario = rs.getString(1);
                        rat.rating = rs.getInt(2);
                        rat.week = week;
                        rat.experience = rs.getInt(3);
                        rats.put(rat.scenario, rat);
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return rats;
    }

    /**
     * Deletes the ratings for the specified player.
     */
    public void deleteRatings (final int playerId)
        throws PersistenceException
    {
        update("delete from RATINGS where PLAYER_ID = " + playerId);
    }

    /**
     * Deletes the ratings for weeks before the specified date.
     */
    public void deleteRatings (Date week)
        throws PersistenceException
    {
        update("delete from RATINGS where WEEK < '" + week + "'");
        update("delete from RANKS where WEEK < '" + week + "'");
    }

    /**
     * Updates the supplied ratings for the specified player.
     */
    public void updateRatings (final int playerId, final ArrayList<Rating> rats)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String uquery = "update RATINGS set RATING=?, EXPERIENCE=? " +
                    "where PLAYER_ID=? and SCENARIO=? and WEEK";
                PreparedStatement ustmt = conn.prepareStatement(uquery + "=?");
                PreparedStatement nstmt = conn.prepareStatement(uquery + " IS NULL");
                String iquery = "insert into RATINGS (PLAYER_ID, SCENARIO, WEEK, " +
                    "RATING, EXPERIENCE) values (?, ?, ?, ?, ?)";
                PreparedStatement istmt = null;
                try {
                    for (Rating rating : rats) {
                        // first try updating
                        PreparedStatement stmt = (rating.week == null ? nstmt : ustmt);
                        stmt.setInt(1, rating.rating);
                        stmt.setInt(2, rating.experience);
                        stmt.setInt(3, playerId);
                        stmt.setString(4, rating.scenario);
                        if (rating.week != null) {
                            stmt.setDate(5, rating.week);
                        }
                        if (stmt.executeUpdate() > 0) {
                            continue;
                        }

                        // if that didn't work, insert
                        if (istmt == null) {
                            istmt = conn.prepareStatement(iquery);
                        }
                        istmt.setInt(1, playerId);
                        istmt.setString(2, rating.scenario);
                        istmt.setDate(3, rating.week);
                        istmt.setInt(4, rating.rating);
                        istmt.setInt(5, rating.experience);
                        JDBCUtil.warnedUpdate(istmt, 1);
                    }

                } finally {
                    JDBCUtil.close(ustmt);
                    JDBCUtil.close(istmt);
                }
                return null;
            }
        });
    }

    /**
     * Loads the top-ranked players in each of the supplied scenario types.
     *
     * @param join an additional table to join (e.g., "GANG_MEMBERS")
     * @param where additions to the where clause (e.g., "RATINGS.PLAYER_ID =
     * GANG_MEMBERS.PLAYER_ID and GANG_ID = 32"), or <code>null</code> for none
     */
    public ArrayList<TopRankedList> loadTopRanked (final String[] scenarios, final String join,
            final String where, final int count, final Date week)
        throws PersistenceException
    {
        final ArrayList<TopRankedList> lists = new ArrayList<TopRankedList>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "select RATINGS.PLAYER_ID, HANDLE " +
                    "from RATINGS, PLAYERS" +
                    (join == null ? "" : (", " + join)) +
                    " where RATINGS.SCENARIO = ? and " + whereWeek(week) + " " +
                    (where == null ? "" : ("and " + where + " ")) +
                    (week == null ? ("and LAST_PLAYED > " + STALE_DATE + " ") :
                                     "and EXPERIENCE > 10 ") +
                    "and RATINGS.PLAYER_ID = PLAYERS.PLAYER_ID " +
                    "order by RATING desc limit " + count;
                PreparedStatement stmt = conn.prepareStatement(query);

                try {
                    for (String scenario : scenarios) {
                        // load the info from the database
                        ArrayList<Handle> handles = new ArrayList<Handle>();
                        ArrayList<Integer> ids = new ArrayList<Integer>();
                        stmt.setString(1, scenario);
                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            ids.add(rs.getInt(1));
                            handles.add(new Handle(rs.getString(2)));
                        }
                        if (ids.isEmpty()) {
                            continue;
                        }

                        // convert it into a TopRankedList object
                        TopRankedList list = new TopRankedList();
                        list.week = week;
                        list.criterion = scenario;
                        list.playerIds = new int[ids.size()];
                        list.players = new Handle[handles.size()];
                        for (int ii = 0; ii < list.playerIds.length; ii++) {
                            list.playerIds[ii] = ids.get(ii);
                            list.players[ii] = handles.get(ii);
                        }
                        lists.add(list);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return lists;
    }

    /**
     * Loads and returns rank levels for each rating type.
     */
    public List<RankLevels> loadRanks (final Date week)
        throws PersistenceException
    {
        final List<RankLevels> levelList = new ArrayList<RankLevels>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ResultSet rs = conn.prepareStatement(
                    "   select RATING_TYPE, RANK, LEVEL " +
                    "     from RANKS where " + whereWeek(week) +
                    " order by RATING_TYPE, RANK ").executeQuery();
                RankLevels currentLevels = null;
                while (rs.next()) {
                    String type = rs.getString(1);
                    int rank = rs.getInt(2);
                    int level = rs.getInt(3);
                    if (currentLevels == null ||
                            !currentLevels.type.equals(type)) {
                        currentLevels = new RankLevels(type);
                        levelList.add(currentLevels);
                    }
                    currentLevels.levels[rank] = level;
                }
                return null;
            }
        });
        return levelList;
    }

    /**
     * Calculate the ranks for each scenario handled by this server;
     * let the Frontier Town server handle the Overall pseudoscenario.
     */
    public void calculateRanks (Date week)
        throws PersistenceException
    {
        for (ScenarioInfo info :
                ScenarioInfo.getScenarios(ServerConfig.townId, false)) {
            calculateRanks(info.getIdent(), week);
        }
        if (BangCodes.FRONTIER_TOWN.equals(ServerConfig.townId)) {
            calculateRanks(ScenarioInfo.OVERALL_IDENT, week);
        }
    }

    /**
     * Scans RATINGS for the given rating type and calculates which rating is
     * is required to reach which rank. Each ranks corresponds to a certain
     * percentile relative the entire player/gang population.
     *
     * When calculations complete, the results are dumped to the RANKS table,
     * which is first cleared of the current rating type. The {@link Metrics}
     * return value holds the calculated rank levels along with a few extra
     * values collected during the computation.
     *
     * This class was originally derived from Yohoho's GenerateStandings and
     * some core logic from there still remains.
     */
    public Metrics calculateRanks (final String type, final Date week)
        throws PersistenceException
    {
        final String what = "RATING from RATINGS where SCENARIO = " + JDBCUtil.escape(type) +
                            " and " + whereWeek(week) +
                            " and LAST_PLAYED > " + STALE_DATE;

        // sort each row for this type into a histogram
        final SparseHistogram histo = new SparseHistogram();
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "select " + what;
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        histo.addValue(rs.getInt(1));
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });

        // now calculate rank levels from the sorted ratings
        int userCount = histo.count;
        IntTuple[] buckets = histo.getFilledBuckets();
        int bucketCount = buckets.length;
        int sidx = 0, sum = 0;

        final Metrics met = new Metrics(type);
        met.totalUsers = userCount;

        for (int bidx = 0; bidx < bucketCount &&
             sidx < RANK_PERCENTAGES.length; bidx++) {
            sum += buckets[bidx].right;
            int pctusers = (int)((sum / (float)userCount) * 100);
            while (sidx < RANK_PERCENTAGES.length &&
                    RANK_PERCENTAGES[sidx] <= pctusers) {
                met.accumUsers[sidx] = sum;
                met.levels[sidx++] = buckets[bidx].left + 1;
            }
        }

        // don't write reports to STDOUT, but show that we can
        if (false) {
            met.generateReport(System.out);
        }

        // finally, clear RANKS and dump our data back into it
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
            throws SQLException, PersistenceException
            {
                PreparedStatement clear = null, insert = null;

                try {
                    // clear the table
                    clear = conn.prepareStatement(
                            "delete from RANKS" +
                            "      where RATING_TYPE = ? and " + whereWeek(week));
                    clear.setString(1, type);
                    clear.execute();
                    JDBCUtil.close(clear);

                    // then fill it
                    insert = conn.prepareStatement(
                        "insert into RANKS " +
                        "        set RATING_TYPE = ?, " +
                        "            RANK = ?, " +
                        "            WEEK = ?, " +
                        "            LEVEL = ? ");
                    int[] levels = met.levels;
                    for (int rank = 0; rank < levels.length; rank ++) {
                        insert.setString(1, type);
                        insert.setInt(2, rank);
                        insert.setDate(3, week);
                        insert.setInt(4, levels[rank]);
                        insert.execute();
                    }
                    return null;

                } finally {
                    JDBCUtil.close(clear);
                    JDBCUtil.close(insert);
                }
            }
        });
        return met;
    }

    /**
     * Loads up and returns the score percentile trackers for all scenarios
     * and player counts.  The percentilers are stored into the supplied hash
     * map, indexed on the scenario id/player count for which they are
     * applicable.
     *
     * @exception PersistenceException thrown if an error occurs
     * communicating with the underlying persistence facilities.
     */
    public void loadScoreTrackers (final HashMap<TrackerKey, Percentiler> map)
        throws PersistenceException
    {
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    // build the statement
                    String query =
                        "select SCENARIO, PLAYERS, DATA from SCORE_TRACKERS";
                    stmt = conn.prepareStatement(query);

                    // read the results
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String scenario = rs.getString(1);
                        int players = rs.getInt(2);
                        byte[] data = (byte[])rs.getObject(3);
                        // create a score tracker from this data
                        Percentiler pt = new Percentiler(data);
                        // insert it into the table
                        map.put(new TrackerKey(scenario, players), pt);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });
    }

    /**
     * Stores the specified score tracker into the database.
     *
     * @exception PersistenceException thrown if a problem occurrs in the
     * underlying persistence services.
     */
    public void storeScoreTracker (final TrackerKey key, Percentiler tracker)
        throws PersistenceException
    {
        // convert the tracker to its serialized representation
        final byte[] data = tracker.toBytes();

        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;

                // first try updating
                try {
                    String query = "update SCORE_TRACKERS" +
                        " set DATA = ? where SCENARIO = ? and PLAYERS = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setObject(1, data);
                    stmt.setString(2, key.scenario);
                    stmt.setInt(3, key.players);

                    // if we modified something, we're done
                    if (stmt.executeUpdate() == 1) {
                        return null;
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                // if that modified zero rows, do the insert
                try {
                    String query = "insert into SCORE_TRACKERS" +
                        " (SCENARIO, PLAYERS, DATA) values (?, ?, ?)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, key.scenario);
                    stmt.setInt(2, key.players);
                    stmt.setObject(3, data);

                    JDBCUtil.checkedUpdate(stmt, 1);
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "RATINGS", new String[] {
            "PLAYER_ID INTEGER NOT NULL",
            "SCENARIO VARCHAR(2) NOT NULL",
            "WEEK DATE NULL DEFAULT NULL",
            "RATING SMALLINT NOT NULL",
            "EXPERIENCE INTEGER NOT NULL",
            "LAST_PLAYED TIMESTAMP NOT NULL",
            "UNIQUE INDEX (PLAYER_ID, SCENARIO, WEEK)",
        }, "");
        JDBCUtil.createTableIfMissing(conn, "RANKS", new String[] {
            "RATING_TYPE VARCHAR(2) NOT NULL",
            "RANK SMALLINT NOT NULL",
            "WEEK DATE NULL DEFAULT NULL",
            "LEVEL INTEGER NOT NULL",
            "UNIQUE INDEX (RATING_TYPE, RANK, WEEK)",
        }, "");
        JDBCUtil.createTableIfMissing(conn, "SCORE_TRACKERS", new String[] {
            "SCENARIO VARCHAR(2) NOT NULL",
            "PLAYERS INTEGER NOT NULL",
            "DATA BLOB NOT NULL",
            "PRIMARY KEY (SCENARIO, PLAYERS)",
        }, "");

        // TEMP: add our LAST_PLAYED column, change RANKS to support non-scenario
        // ratings
        if (!JDBCUtil.tableContainsColumn(conn, "RATINGS", "LAST_PLAYED")) {
            JDBCUtil.addColumn(
                conn, "RATINGS", "LAST_PLAYED", "TIMESTAMP NOT NULL", null);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("update RATINGS set LAST_PLAYED = " +
                               "DATE_SUB(NOW(), INTERVAL 1 WEEK)");
            stmt.close();
        }
        if (!JDBCUtil.tableContainsColumn(conn, "RANKS", "RATING_TYPE")) {
            JDBCUtil.changeColumn(conn, "RANKS", "SCENARIO", "RATING_TYPE VARCHAR(2) NOT NULL");
            JDBCUtil.changeColumn(conn, "RANKS", "LEVEL", "LEVEL INTEGER NOT NULL");
        }
        if (!JDBCUtil.tableContainsColumn(conn, "RATINGS", "WEEK")) {
            JDBCUtil.addColumn(conn, "RATINGS", "WEEK", "DATE NULL DEFAULT NULL", "SCENARIO");
            JDBCUtil.dropPrimaryKey(conn, "RATINGS");
            Statement stmt = conn.createStatement();
            stmt.execute("alter table RATINGS add UNIQUE INDEX (PLAYER_ID, SCENARIO, WEEK)");
            stmt.close();
        }
        if (!JDBCUtil.tableContainsColumn(conn, "RANKS", "WEEK")) {
            JDBCUtil.addColumn(conn, "RANKS", "WEEK", "DATE NULL DEFAULT NULL", "RANK");
            JDBCUtil.dropPrimaryKey(conn, "RANKS");
            Statement stmt = conn.createStatement();
            stmt.execute("alter table RANKS add UNIQUE INDEX (RATING_TYPE, RANK, WEEK)");
            stmt.close();
        }
        // END TEMP
    }

    /** Extends {@link RankLevels} with data collected using calculations */
    protected static class Metrics extends RankLevels
    {
        public int[] accumUsers = new int[RANK_PERCENTAGES.length];
        public int totalUsers;

        public int getPercent (int sidx)
        {
            return (int)((accumUsers[sidx] / (float)totalUsers) * 100);
        }

        /** Convenience function for displaying metrics for a rating type. */
        public void generateReport (PrintStream stream)
        {
            String name;
            if (ScenarioInfo.OVERALL_IDENT.equals(type)) {
                name = "OVERALL";
            } else {
                name = ScenarioInfo.getScenarioInfo(type).getName();
            }
            stream.println(StringUtil.pad(name, 26));
            stream.println("tgt% act% users total rating");
            for (int sidx = 0; sidx < RANK_PERCENTAGES.length; sidx++) {
                StringBuilder line = new StringBuilder();
                String val = "" + RANK_PERCENTAGES[sidx];
                line.append(StringUtil.prepad(val, 4));
                val = "" + getPercent(sidx);
                line.append(" ").append(StringUtil.prepad(val, 4));
                val = "" + accumUsers[sidx];
                line.append(" ").append(StringUtil.prepad(val, 5));
                val = "" + totalUsers;
                line.append(" ").append(StringUtil.prepad(val, 5));
                val = "" + levels[sidx];
                line.append(" ").append(StringUtil.prepad(val, 6));
                stream.println(line);
            }
        }

        protected Metrics (String type)
        {
            super(type);
        }
    }

    /**
     * A sparse histogram with buckets of size 1 backed by an {@link IntIntMap}.
     */
    protected static class SparseHistogram
    {
        /** The minimum value tracked by this histogram. */
        public int minValue = Integer.MAX_VALUE;

        /** The maximum value tracked by this histogram. */
        public int maxValue = Integer.MIN_VALUE;

        /** The total number of values. */
        public int count;

        /**
         * Registers a value with this histogram.
         */
        public void addValue (int value)
        {
            _buckets.increment(value, 1);
            count++;

            if (value < minValue) {
                minValue = value;
            }
            if (value > maxValue) {
                maxValue = value;
            }
        }

        /**
         * Returns an array containing the bucket counts for all buckets between
         * minValue and maxValue (inclusive).
         */
        public int[] getBuckets ()
        {
            int[] buckets = new int[(maxValue - minValue) + 1];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = Math.max(_buckets.get(minValue + i), 0);
            }
            return buckets;
        }

        /**
         * Returns an array of tuples containing the values (left) and counts
         * (right) for all buckets with a count of at least one.  The array will
         * be sorted by the values.
         */
        public IntTuple[] getFilledBuckets ()
        {
            IntTuple[] buckets = new IntTuple[_buckets.size()];
            int[] keys = _buckets.getKeys(), values = _buckets.getValues();
            for (int i = 0; i < keys.length; i++) {
                buckets[i] = new IntTuple(keys[i], values[i]);
            }
            QuickSort.sort(buckets);
            return buckets;
        }

        /**
         * Returns a string representation of this histogram.
         */
        public String toString ()
        {
            return "[min=" + minValue + ", max=" + maxValue +
                ", count=" + count + ", buckets=" + _buckets + "]";
        }

        /** The histogram buckets. */
        protected IntIntMap _buckets = new IntIntMap();
    }

    /**
     * The percentage of users that must have a lower rating than you
     * in order for you to be a part of a given rank.
     */
    protected static final int[] RANK_PERCENTAGES = {
        50, 65, 75, 85, 90, 95, 99 };

    /** The cutoff after which a rating is considered stale and no longer
     * considered when calculating standings or for top scores. */
    protected static final String STALE_DATE =
        "DATE_SUB(NOW(), INTERVAL 2 WEEK)";
}
