//
// $Id$

package com.threerings.bang.client.util;

import com.samskivert.util.Interval;

import com.threerings.media.timer.NanoTimer;
import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Used to monitor performance in various niggling places.
 */
public class PerfMonitor
{
    public static void init (BasicContext ctx)
    {
        _ctx = ctx;

        // start up an interval that dumps performance output every N seconds
        new Interval(ctx.getApp()) {
            public void expired () {
                perfReport();
            }
        }.schedule(PERF_REPORT_INTERVAL, true);
    }

    public static void setReportToChat (boolean reportToChat)
    {
        _reportToChat = reportToChat;
    }

    /**
     * Returns a microsecond accurate timestamp.
     */
    public static long getCurrentMicros ()
    {
        return _timer.getElapsedMicros();
    }

    /**
     * Records that a model was loaded starting at the specified starting
     * timestamp and loading the specified number of bytes.
     */
    public static void recordModelLoad (long startMicros, int bytesLoaded)
    {
        long now = _timer.getElapsedMicros(), elapsed = now - startMicros;
//         log.info("Loaded model", "bytes", bytesLoaded, "micros", elapsed,
//                  "kbps", (1000*bytesLoaded/elapsed));

        synchronized (_models) {
            _models[0]++;
            _models[1] += bytesLoaded;
            _models[2] += elapsed;
        }
    }

    protected static void perfReport ()
    {
        synchronized (_models) {
            if (_models[0] > 0) {
                int kb = (int)(_models[1] / 1024);
                int kbps = (int)(1000 * _models[1] / _models[2]);
                int millis = (int)(_models[2] / 1000);
                log.info("Model loader report", "loaded", _models[0], "kb", kb, "ms", millis,
                         "kbps", kbps);
                if (_reportToChat && _ctx instanceof BangContext) {
                    String msg = "Models: #" + _models[0] + " " + kb +
                        "k " + millis + "ms " + kbps + "kbps";
                    ((BangContext)_ctx).getChatDirector().displayInfo(
                        null, MessageBundle.taint(msg));
                }
                _models[0] = _models[1] = _models[2] = 0;
            }
        }
    }

    protected static BasicContext _ctx;
    protected static boolean _reportToChat;
    protected static NanoTimer _timer = new NanoTimer();

    protected static long _lastReport;
    protected static long[] _models = new long[3];

    /** We potentially report every five seconds. */
    protected static final long PERF_REPORT_INTERVAL = 5000L;
}
