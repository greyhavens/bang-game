//
// $Id$

package com.threerings.bang.server.persist;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntTuple;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Rating;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.saloon.data.TopRankedList;

import static com.threerings.bang.Log.log;

/**
 * Responsible for the persistent storage of per-player ratings.
 */
public class RatingRepository extends SimpleRepository
{
    /** Keeps track of the rating levels for each rank for a given scenario */
    public static class RankLevels
    {
        public String scenario;
        public int[] levels = new int[RANK_PERCENTAGES.length];

        public int getRank (int rating)
        {
            int rank = levels.length;
            while (rank > 0 && rating < levels[rank-1]) {
                rank -= 1;
            }
            return rank;
        }

        protected RankLevels (String scenario)
        {
            this.scenario = scenario;
        }
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
    public RatingRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, RATING_DB_IDENT);
    }

    /**
     * Loads the ratings for the specified player.
     */
    public ArrayList<Rating> loadRatings (final int playerId)
        throws PersistenceException
    {
        final ArrayList<Rating> rats = new ArrayList<Rating>();
        final String query = "select SCENARIO, RATING, EXPERIENCE " +
            "from RATINGS where PLAYER_ID = " + playerId;
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
                        rat.experience = rs.getInt(3);
                        rats.add(rat);
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
                    "where PLAYER_ID=? and SCENARIO=?";
                PreparedStatement ustmt = conn.prepareStatement(uquery);
                String iquery = "insert into RATINGS (PLAYER_ID, SCENARIO, " +
                    "RATING, EXPERIENCE) values (?, ?, ?, ?)";
                PreparedStatement istmt = null;
                try {
                    for (Rating rating : rats) {
                        // first try updating
                        ustmt.setInt(1, rating.rating);
                        ustmt.setInt(2, rating.experience);
                        ustmt.setInt(3, playerId);
                        ustmt.setString(4, rating.scenario);
                        if (ustmt.executeUpdate() > 0) {
                            continue;
                        }

                        // if that didn't work, insert
                        if (istmt == null) {
                            istmt = conn.prepareStatement(iquery);
                        }
                        istmt.setInt(1, playerId);
                        istmt.setString(2, rating.scenario);
                        istmt.setInt(3, rating.rating);
                        istmt.setInt(4, rating.experience);
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
     */
    public ArrayList<TopRankedList> loadTopRanked (
        final String[] scenarios, final int count)
        throws PersistenceException
    {
        final ArrayList<TopRankedList> lists = new ArrayList<TopRankedList>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "select RATINGS.PLAYER_ID, HANDLE " +
                    "from RATINGS, PLAYERS " +
                    "where RATINGS.SCENARIO = ? " +
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

                        // convert it into a TopRankedList object
                        TopRankedList list = new TopRankedList();
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
     * Loads and returns rank levels for each scenario.
     */
    public List<RankLevels> loadRanks ()
        throws PersistenceException
    {
        final List<RankLevels> levelList = new ArrayList<RankLevels>();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ResultSet rs = conn.prepareStatement(
                    "   select SCENARIO, RANK, LEVEL " +
                    "     from RANKS " +
                    " order by SCENARIO, RANK ").executeQuery();
                RankLevels currentLevels = null;
                while (rs.next()) {
                    String scenario = rs.getString(1);
                    int rank = rs.getInt(2);
                    int level = rs.getInt(3);
                    if (currentLevels == null ||
                            !currentLevels.scenario.equals(scenario)) {
                        currentLevels = new RankLevels(scenario);
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
     * Performs a full table scan of RATINGS and calculates for each scenario
     * which rating is required to reach which rank. Each ranks corresponds
     * to a certain percentile relative the entire player population.
     *
     * When calculations complete, the results are dumped to the RANKS table,
     * which is first cleared. The return value maps {@link ScenarioInfo} ID's
     * to {@Link Metrics} instances, which hold the calculated rank levels
     * along with some additional metrics.
     *
     * This class was originally derived from Yohoho's GenerateStandings and
     * some core logic from there still remains.
     */
    public List<Metrics> calculateRanks ()
        throws PersistenceException
    {
        // sort each row from RATINGS into the right histogram
        final Map<String, SparseHistogram> hists =
            new HashMap<String, SparseHistogram>();
        execute(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    String query =
                        "select SCENARIO, RATING from RATINGS";
                    stmt = conn.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String scenario = rs.getString(1);
                        int rating = rs.getInt(2);
                        // retrieve or create a histogram for this scenario
                        SparseHistogram histo = hists.get(scenario);
                        if (histo == null) {
                            histo = new SparseHistogram();
                            hists.put(scenario, histo);
                        }
                        histo.addValue(rating);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }

                return null;
            }
        });

        // now calculate rank levels from the sorted ratings
        final Map<String, Metrics> metrics = new HashMap<String, Metrics>();
        for (Entry<String, SparseHistogram> entry : hists.entrySet()) {
            String scenario = entry.getKey();
            SparseHistogram histo = entry.getValue();

            int userCount = histo.count;
            IntTuple[] buckets = histo.getFilledBuckets();
            int bucketCount = buckets.length;
            int sidx = 0, sum = 0;

            Metrics met = new Metrics(scenario);
            metrics.put(scenario, met);
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
        }

        // don't write reports to STDOUT, but show that we can
        if (false) {
            for (Metrics met : metrics.values()) {
                met.generateReport(System.out);
            }
        }

        // finally, clear RANKS and dump our data back into it
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
            throws SQLException, PersistenceException
            {
                // clear the table
                conn.prepareStatement("delete from RANKS").execute();
                // then fill it
                PreparedStatement insert = conn.prepareStatement(
                    "insert into RANKS " +
                    "        set SCENARIO = ?, " +
                    "            RANK = ?, " +
                "            LEVEL = ? ");
                for (Entry<String, Metrics> entry : metrics.entrySet()) {
                    String scenario = entry.getKey();
                    int[] levels = entry.getValue().levels;
                    for (int rank = 0; rank < levels.length; rank ++) {
                        insert.setString(1, scenario);
                        insert.setInt(2, rank);
                        insert.setInt(3, levels[rank]);
                        insert.execute();
                    }
                }
                return null;
            }
        });
        return new ArrayList<Metrics>(metrics.values());
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
            JDBCUtil.createTableIfMissing(conn, "RATINGS", new String[] {
                "PLAYER_ID INTEGER NOT NULL",
                "SCENARIO VARCHAR(2) NOT NULL",
                "RATING SMALLINT NOT NULL",
                "EXPERIENCE INTEGER NOT NULL",
                "PRIMARY KEY (PLAYER_ID, SCENARIO)",
            }, "");
            JDBCUtil.createTableIfMissing(conn, "RANKS", new String[] {
                "SCENARIO VARCHAR(2) NOT NULL",
                "RANK SMALLINT NOT NULL",
                "LEVEL SMALLINT NOT NULL",
                "PRIMARY KEY (SCENARIO, RANK)",
            }, "");
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

        /** Convenience function for displaying metrics for a scenario */
        public void generateReport (PrintStream stream)
        {
            String name = ScenarioInfo.OVERALL_IDENT.equals(scenario) ?
                "OVERALL" :
                ScenarioInfo.getScenarioInfo(scenario).getName();
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

        protected Metrics (String scenario)
        {
            super(scenario);
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
    protected static final int[] RANK_PERCENTAGES =
        { 50, 65, 75, 85, 90, 95, 99 };
}
