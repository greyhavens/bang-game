//
// $Id$

package com.threerings.bang.web.logic;

import java.sql.Date;
import java.util.ArrayList;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.IntTuple;
import com.samskivert.velocity.InvocationContext;

import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;

import com.threerings.parlor.rating.util.Percentiler;
import com.threerings.user.OOOUser;

import com.threerings.bang.data.StatType;
import com.threerings.bang.server.persist.BangStatRepository;

import com.threerings.bang.web.OfficeApp;

/**
 * Displays graphs and summary information on statistics accumulated by players
 * in game.
 */
public class player_stats extends AdminLogic
{
    // documentation inherited
    public void invoke (OfficeApp app, InvocationContext ctx, OOOUser user)
        throws Exception
    {
        ComparableArrayList<StatType> types = new ComparableArrayList<StatType>();
        for (StatType type : StatType.values()) {
            if (type.isPersistent()) {
                types.insertSorted(type);
            }
        }
        ctx.put("types", types);

        // if they specified a type, look it up
        StatType type = (StatType)Stat.getType(
            ParameterUtil.getIntParameter(ctx.getRequest(), "type", 0, "error.invalid_type"));
        if (type == null) {
            return;
        }

        final ComparableArrayList<StatRecord> stats = new ComparableArrayList<StatRecord>();
        final Percentiler tiler = new Percentiler();
        BangStatRepository.Processor proc = new BangStatRepository.Processor() {
            public void process (int playerId, String accountName, String handle,
                                 Date created, int sessionMinutes, Stat stat) {
                StatRecord record = new StatRecord();
                record.playerId = playerId;
                record.accountName = accountName;
                record.handle = handle;
                if (stat instanceof IntStat) {
                    record.intValue = ((IntStat)stat).getValue();
                    tiler.recordValue(record.intValue, false);
                } else {
                    record.stringValue = stat.valueToString();
                }
                stats.insertSorted(record);
            }
        };
        app.getStatRepository().processStats(proc, type);
        ctx.put("stats", stats);

        tiler.recomputePercentiles();
        if (tiler.getMaxScore() > 1) {
            ArrayList<IntTuple> pctiles = new ArrayList<IntTuple>();
            for (int pctile = 0; pctile <= 100; pctile += 10) {
                pctiles.add(new IntTuple(pctile,
                                         (int)tiler.getRequiredScore(pctile)));
            }
            ctx.put("pctiles", pctiles);
        }
    }

    protected static class StatRecord implements Comparable<StatRecord>
    {
        public int playerId;
        public String accountName;
        public String handle;
        public int intValue;
        public String stringValue;

        public String toString () {
            return (stringValue == null) ?
                String.valueOf(intValue) : stringValue;
        }

        public int compareTo (StatRecord other) {
            if (stringValue == null) {
                return other.intValue - intValue;
            } else {
                return stringValue.compareTo(other.stringValue);
            }
        }
    }
}
