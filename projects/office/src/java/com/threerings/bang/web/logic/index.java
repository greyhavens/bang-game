//
// $Id$

package com.threerings.bang.web.logic;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

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
        ctx.put("sessbyday", _lastSessionsByDay);
        ctx.put("sessbyweek", _lastSessionsByWeek);
        ctx.put("sessbymonth", _lastSessionsByMonth);
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
        _lastSessionsByDay = new TreeMap<Date,Integer>(Collections.reverseOrder());
        _lastSessionsByWeek = new TreeMap<Date,Integer>(Collections.reverseOrder());
        _lastSessionsByMonth = new TreeMap<Date,Integer>(Collections.reverseOrder());
        for (Map.Entry<Date,Integer> entry :
                 app.getPlayerRepository().summarizeLastSessions().entrySet()) {
            cal.setTime(entry.getKey());
            int week = cal.get(Calendar.WEEK_OF_YEAR);
            int year = cal.get(Calendar.YEAR);

            // see if it's in the last week
            if (now - entry.getKey().getTime() < ONE_WEEK_MILLIS) {
                _lastSessionsByDay.put(entry.getKey(), entry.getValue());
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
                Date wkey = toWeek(entry.getKey());
                Integer ovalue = _lastSessionsByWeek.get(wkey);
                int nvalue = (ovalue == null) ? entry.getValue() : (entry.getValue() + ovalue);
                _lastSessionsByWeek.put(wkey, nvalue);
            }

            // also accumulate monthly
            Date mkey = toMonth(entry.getKey());
            Integer ovalue = _lastSessionsByMonth.get(mkey);
            int nvalue = (ovalue == null) ? entry.getValue() : (entry.getValue() + ovalue);
            _lastSessionsByMonth.put(mkey, nvalue);
        }
    }

    protected Date toWeek (Date date)
    {
        return convert(date, Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    }

    protected Date toMonth (Date date)
    {
        return convert(date, Calendar.DAY_OF_MONTH, 1);
    }

    protected Date convert (Date date, int period, int value)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(period, value);
        return new Date(cal.getTimeInMillis());
    }

    protected static long _lastReportGen;
    protected static Map<Date,Integer> _lastSessionsByDay;
    protected static Map<Date,Integer> _lastSessionsByWeek;
    protected static Map<Date,Integer> _lastSessionsByMonth;

    protected static final long REPORT_REGEN_INTERVAL = 60 * 1000L;
    protected static final long ONE_WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L;
}
