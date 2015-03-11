//
// $Id$

package com.threerings.bang.tools;

import java.sql.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.TreeSet;

import com.samskivert.depot.StaticConnectionProvider;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.util.CountHashMap;
import com.samskivert.util.StringUtil;

import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StringSetStat;

import com.threerings.bang.data.StatType;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BangStatRepository;

/**
 * Dumps information on a particular statistic.
 */
public class DumpStat
{
    public interface Grinder extends BangStatRepository.Processor
    {
        public void printSummary ();
    }

    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 2) {
            String opts = StringUtil.join(EnumSet.allOf(Action.class).toArray(), "|").toLowerCase();
            System.err.println("Usage: DumpStat [" + opts + "] stat");
            System.exit(-1);
        }

        Action action;
        try {
            action = Enum.valueOf(Action.class, args[0].toUpperCase());
        } catch (Exception e) {
            System.err.println("Usage: DumpStat [dump|summary] stat");
            System.exit(-1);
            return; // help the compiler be smart
        }

        StatType type;
        try {
            type = Enum.valueOf(StatType.class, args[1].toUpperCase());
        } catch (Exception e) {
            System.err.println("Unknown stat " + args[1] + ".");
            System.exit(-1);
            return; // help the compiler be smart
        }

        BangStatRepository statrepo = new BangStatRepository(
            new PersistenceContext(
                "bangdb", new StaticConnectionProvider(ServerConfig.getJDBCConfig()), null));

        Grinder grinder = null;
        switch (action) {
        case DUMP: grinder = new DumpGrinder(); break;
        case DATE_COUNT: grinder = new MemberDateSummarizer(); break;
        case SUMMARY: grinder = getSummaryGrinder(type); break;
        case DATE_SUMMARY: grinder = getDateSummaryGrinder(type); break;
        }

        if (grinder != null) {
            statrepo.processStats(grinder, type);
            grinder.printSummary();
        } else {
            System.err.println("Have no " + action + " grinder for " +
                               type.newStat().getClass().getName() + ".");
            System.exit(-1);
        }
    }

    protected static Grinder getSummaryGrinder (StatType type)
    {
        Stat stat = type.newStat();
        if (stat instanceof StringSetStat) {
            return new StringSetSummarizer();
        } else {
            return null;
        }
    }

    protected static Grinder getDateSummaryGrinder (StatType type)
    {
        Stat stat = type.newStat();
        if (stat instanceof StringSetStat) {
            return new StringSetDateSummarizer();
        } else if (stat instanceof IntStat) {
            return new IntHistoSummarizer();
        } else {
            return null;
        }
    }

    protected static class DumpGrinder implements Grinder
    {
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat) {
            System.out.println(playerId + " " + accountName + " " + created + " " +
                               sessionMinutes + " " + stat.valueToString());
        }

        public void printSummary () {
            // nada
        }
    }

    protected static class MemberDateSummarizer implements Grinder
    {
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat) {
            _data.incrementCount(created, 1);
        }

        public void printSummary () {
            System.out.println("date count");
            for (Date date : new TreeSet<Date>(_data.keySet())) {
                System.out.println(date + " " + _data.getCount(date));
            }
        }

        protected CountHashMap<Date> _data = new CountHashMap<Date>();
    }

    protected static class DateCountSummarizer implements Grinder
    {
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat) {
            int value = ((IntStat)stat).getValue();
            if (value > 0) {
                _data.incrementCount(created, 1);
            }
        }

        public void printSummary () {
            System.out.println("date count");
            for (Date date : new TreeSet<Date>(_data.keySet())) {
                System.out.println(date + " " + _data.getCount(date));
            }
        }

        protected CountHashMap<Date> _data = new CountHashMap<Date>();
    }

    protected static class IntHistoSummarizer implements Grinder
    {
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat) {
            int value = ((IntStat)stat).getValue();

            int bucket = BUCKETS.length;
            for (int ii = 0; ii < BUCKETS.length; ii++) {
                if (value <= BUCKETS[ii]) {
                    bucket = ii;
                    break;
                }
            }

            int[] buckets = _data.get(created);
            if (buckets == null) {
                _data.put(created, buckets = new int[BUCKETS.length+1]);
            }
            buckets[bucket]++;
        }

        public void printSummary () {
            System.out.print("date");
            for (int ii = 0; ii <= BUCKETS.length; ii++) {
                System.out.print(" ");
                if (ii == BUCKETS.length) {
                    System.out.println(BUCKETS[ii-1] + "+");
                } else if (ii == 0) {
                    System.out.print(BUCKETS[ii]);
                } else {
                    System.out.print((BUCKETS[ii-1]+1) + "<>" + BUCKETS[ii]);
                }
            }
            for (Date date : new TreeSet<Date>(_data.keySet())) {
                int[] buckets = _data.get(date);
                System.out.print(date);
                for (int ii = 0; ii < buckets.length; ii++) {
                    System.out.print(" " + buckets[ii]);
                }
                System.out.println("");
            }
        }

        protected HashMap<Date,int[]> _data = new HashMap<Date,int[]>();

        protected static final int[] BUCKETS = { 0, 1, 2, 3, 4, 5, 10, 20, 30, 40, 50 };
    }

    protected static abstract class StringSetProcessor implements Grinder
    {
        public void process (int playerId, String accountName, String handle,
                             Date created, int sessionMinutes, Stat stat) {
            StringSetStat ssstat = (StringSetStat)stat;
            CountHashMap<String> counts = getMap(created, sessionMinutes);
            for (String value : ssstat.values()) {
                counts.incrementCount(value, 1);
            }
        }

        protected abstract CountHashMap<String> getMap (Date created, int sessionMintues);
    }

    protected static class StringSetSummarizer extends StringSetProcessor
    {
        public void printSummary () {
            for (String key : _counts.keySet()) {
                System.out.println(key + " " + _counts.getCount(key));
            }
        }

        protected CountHashMap<String> getMap (Date created, int sessionMintues)
        {
            return _counts;
        }

        protected CountHashMap<String> _counts = new CountHashMap<String>();
    }

    protected static class StringSetDateSummarizer extends StringSetProcessor
    {
        public void printSummary () {
            // enumerate all stat keys
            TreeSet<String> allKeys = new TreeSet<String>();
            for (Date date : _counts.keySet()) {
                allKeys.addAll(_counts.get(date).keySet());
            }

            // print the headers
            System.out.print("date");
            for (String key : allKeys) {
                System.out.print(" " + key);
            }
            System.out.println("");

            // now iterate over the sorted date set
            for (Date date : new TreeSet<Date>(_counts.keySet())) {
                System.out.print(date);
                CountHashMap<String> map = _counts.get(date);
                for (String key : allKeys) {
                    System.out.print(" " + map.getCount(key));
                }
                System.out.println("");
            }
        }

        protected CountHashMap<String> getMap (Date created, int sessionMintues)
        {
            CountHashMap<String> map = _counts.get(created);
            if (map == null) {
                _counts.put(created, map = new CountHashMap<String>());
            }
            return map;
        }

        protected HashMap<Date,CountHashMap<String>> _counts =
            new HashMap<Date,CountHashMap<String>>();
    }

    protected enum Action { DUMP, DATE_COUNT, SUMMARY, DATE_SUMMARY };
}
