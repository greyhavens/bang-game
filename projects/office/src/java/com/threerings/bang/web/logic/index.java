//
// $Id$

package com.threerings.bang.web.logic;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.samskivert.util.Tuple;
import com.samskivert.velocity.InvocationContext;

import com.threerings.user.OOOUser;

import com.threerings.bang.web.OfficeApp;

/**
 * Displays summary statistics on Bang! Howdy usage.
 */
public class index extends AdminLogic
{
    // from AdminLogic
    public void invoke (OfficeApp app, InvocationContext ctx, OOOUser user)
        throws Exception
    {
        regenerateReports(app);
        ctx.put("sessions", _lastSessionSummary);
    }

    protected synchronized void regenerateReports (OfficeApp app)
        throws Exception
    {
        long now = System.currentTimeMillis();
        if (now - _lastReportGen < REPORT_REGEN_INTERVAL) {
            return;
        }
        _lastReportGen = now;

        // obtain the week and year number of this week and the previous three
        Calendar cal = Calendar.getInstance();
        int[] weeks = new int[4], years = new int[4];
        for (int ii = 0; ii < 4; ii++) {
            weeks[ii] = cal.get(Calendar.WEEK_OF_YEAR);
            years[ii] = cal.get(Calendar.YEAR);
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }

        // obtain our per-day summary and consolidate it such that this week is accumulated daily,
        // the previous three accumulated weekly and the rest accumulated monthly
        TreeMap<String,Integer> map = new TreeMap<String,Integer>(Collections.reverseOrder());
        for (Tuple<Date,Integer> entry : app.getPlayerRepository().summarizeLastSessions()) {
            cal.setTime(entry.left);
            int week = cal.get(Calendar.WEEK_OF_YEAR);
            int year = cal.get(Calendar.YEAR);

            // see if it's in the last week
            if (now - entry.left.getTime() < ONE_WEEK_MILLIS) {
                map.put(entry.left.toString(), entry.right);
            }

            // see if it's in the last month
            boolean thisMonth = false;
            for (int ii = 0; ii < weeks.length; ii++) {
                if (weeks[ii] == week && years[ii] == year) {
                    thisMonth = true;
                    break;
                }
            }
            if (thisMonth) {
                String byweek = toWeek(entry.left);
                Integer ovalue = map.get(byweek);
                map.put(byweek, (ovalue == null) ? entry.right : (entry.right + ovalue));
            }

            // also accumulate monthly
            String bymonth = toMonth(entry.left);
            Integer ovalue = map.get(bymonth);
            map.put(bymonth, (ovalue == null) ? entry.right : (entry.right + ovalue));
        }

        // convert the map to a list of tuples
        _lastSessionSummary = new ArrayList<Tuple<String,Integer>>();
        for (Map.Entry<String,Integer> entry : map.entrySet()) {
            _lastSessionSummary.add(new Tuple<String,Integer>(entry.getKey(), entry.getValue()));
        }
    }

    protected String toWeek (Date date)
    {
        return convert(date, Calendar.DAY_OF_WEEK, Calendar.MONDAY) + " week";
    }

    protected String toMonth (Date date)
    {
        return convert(date, Calendar.DAY_OF_MONTH, 1) + " month";
    }

    protected Date convert (Date date, int period, int value)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(period, value);
        return new Date(cal.getTimeInMillis());
    }

    protected static long _lastReportGen;
    protected static ArrayList<Tuple<String,Integer>> _lastSessionSummary;

    protected static final long REPORT_REGEN_INTERVAL = 60 * 1000L;
    protected static final long ONE_WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L;
}
