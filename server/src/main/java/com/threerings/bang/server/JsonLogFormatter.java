//
// $Id$

package com.threerings.bang.server;

import java.time.Instant;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A minimal JSON-lines {@link java.util.logging.Formatter} for the server: one flat JSON object
 * per line ({@code {"ts","level","logger","thread","msg"[,"exception"]}}), which log-aggregation
 * pipelines (Loki, ELK, CloudWatch, …) parse directly off the container's stdout stream. Selected
 * via {@code server.log_format = json} (see {@code BangServer.configureLog}); the default is the
 * human-readable one-line format.
 */
public class JsonLogFormatter extends Formatter
{
    @Override
    public String format (LogRecord record)
    {
        StringBuilder b = new StringBuilder(256).append('{');
        field(b, "ts", Instant.ofEpochMilli(record.getMillis()).toString());
        field(b, "level", record.getLevel().getName());
        field(b, "logger", record.getLoggerName());
        field(b, "thread", Thread.currentThread().getName());
        field(b, "msg", formatMessage(record));
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            field(b, "exception", sw.toString());
        }
        // trim the trailing comma from the last field and close
        if (b.charAt(b.length() - 1) == ',') {
            b.setLength(b.length() - 1);
        }
        return b.append("}\n").toString();
    }

    /** Appends {@code "name":"value",} with the value JSON-escaped. */
    protected static void field (StringBuilder b, String name, String value)
    {
        b.append('"').append(name).append("\":\"");
        escape(b, value == null ? "" : value);
        b.append("\",");
    }

    /** Escapes a string for inclusion in a JSON double-quoted value. */
    protected static void escape (StringBuilder b, String s)
    {
        for (int ii = 0, nn = s.length(); ii < nn; ii++) {
            char c = s.charAt(ii);
            switch (c) {
            case '"':  b.append("\\\""); break;
            case '\\': b.append("\\\\"); break;
            case '\n': b.append("\\n"); break;
            case '\r': b.append("\\r"); break;
            case '\t': b.append("\\t"); break;
            default:
                if (c < 0x20) {
                    b.append(String.format("\\u%04x", (int)c));
                } else {
                    b.append(c);
                }
            }
        }
    }
}
