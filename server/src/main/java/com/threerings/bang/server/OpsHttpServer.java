//
// $Id$

package com.threerings.bang.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.threerings.nio.conman.ConMgrStats;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.net.PresentsConnectionManager;

import static com.threerings.bang.Log.log;

/**
 * A zero-dependency management HTTP server (Epic 3 Phase 6) exposing:
 * <ul>
 *   <li>{@code /health} — 200 {@code ok} while the connection manager thread is running, 503
 *       otherwise; intended for container {@code HEALTHCHECK} and orchestrator probes.</li>
 *   <li>{@code /metrics} — Prometheus text exposition (JVM memory, uptime, session/connection
 *       counts and the {@link ConMgrStats} network counters), all labeled with the node name.</li>
 * </ul>
 *
 * Served by the JDK's built-in {@link HttpServer} — no new dependencies. Configured by
 * {@code server_mgmt_port} (0/absent = disabled; per-node override {@code <node>.server_mgmt_port}
 * for multi-node hosts like the dev cluster) and {@code server_mgmt_host} (default
 * {@code 127.0.0.1}; set to {@code 0.0.0.0} only where the port is network-isolated, e.g. inside a
 * container — the endpoint is unauthenticated by design and must not be exposed publicly).
 *
 * <p>Thread safety: handlers run on this server's own daemon threads, so they may only touch
 * state that is safe off the dobj thread. {@code ConnectionManager.getStats()} is synchronized
 * and returns a snapshot clone, and {@code ClientManager.getClientCount()} synchronizes
 * internally; nothing here reads distributed objects.
 */
public class OpsHttpServer
{
    /**
     * Starts the management server, or returns null if {@code port} is zero or negative
     * (disabled). Failure to bind is logged and returns null rather than aborting server startup —
     * losing metrics should not take down the game.
     */
    public static OpsHttpServer start (String host, int port, String node,
                                       PresentsConnectionManager conmgr, ClientManager clmgr)
    {
        if (port <= 0) {
            return null;
        }
        try {
            OpsHttpServer server = new OpsHttpServer(host, port, node, conmgr, clmgr);
            log.info("Management HTTP server running.", "host", host, "port", port);
            return server;
        } catch (Exception e) {
            // not just IOException: a misconfigured port (> 65535, e.g. a typo'd
            // BANG_SERVER_MGMT_PORT) throws an unchecked IllegalArgumentException from
            // InetSocketAddress, and that must not take down the game server either
            log.warning("Failed to start management HTTP server.",
                        "host", host, "port", port, e);
            return null;
        }
    }

    /**
     * Stops the management server (called during server shutdown).
     */
    public void shutdown ()
    {
        _httpd.stop(0);
        // HttpServer.stop does NOT shut down a custom executor; do it ourselves or the worker
        // threads linger blocked on the task queue
        _exec.shutdownNow();
    }

    protected OpsHttpServer (String host, int port, String node,
                             PresentsConnectionManager conmgr, ClientManager clmgr)
        throws IOException
    {
        // escape the node name per the Prometheus text format so a hostile or odd value (it can
        // arrive via the BANG_* env bridge) can't break out of the label position
        _node = node.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        _conmgr = conmgr;
        _clmgr = clmgr;

        _httpd = HttpServer.create(new InetSocketAddress(host, port), 0);
        _httpd.createContext("/health", this::handleHealth);
        _httpd.createContext("/metrics", this::handleMetrics);
        // two workers so a slow or stalled /metrics scrape can't head-of-line-block the /health
        // probe that drives container liveness decisions
        _exec = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "OpsHttpServer");
            thread.setDaemon(true);
            return thread;
        });
        _httpd.setExecutor(_exec);
        _httpd.start();
    }

    protected void handleHealth (HttpExchange exchange)
        throws IOException
    {
        if (_conmgr.isRunning()) {
            respond(exchange, 200, "text/plain; charset=utf-8", "ok\n");
        } else {
            respond(exchange, 503, "text/plain; charset=utf-8", "unhealthy\n");
        }
    }

    protected void handleMetrics (HttpExchange exchange)
        throws IOException
    {
        ConMgrStats stats = _conmgr.getStats();
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory(), free = rt.freeMemory();
        StringBuilder b = new StringBuilder(1024);

        gauge(b, "bang_up", "1 while the connection manager thread is running",
              _conmgr.isRunning() ? 1 : 0);
        gauge(b, "bang_uptime_seconds", "JVM uptime in seconds",
              ManagementFactory.getRuntimeMXBean().getUptime() / 1000d);
        gauge(b, "bang_sessions", "client sessions (some may be disconnected)",
              _clmgr.getClientCount());
        gauge(b, "bang_connections", "open network connections", stats.connectionCount);

        gauge(b, "jvm_memory_used_bytes", "heap in use", total - free);
        gauge(b, "jvm_memory_total_bytes", "current heap size", total);
        gauge(b, "jvm_memory_max_bytes", "maximum heap size", rt.maxMemory());

        counter(b, "bang_conmgr_connects_total", "accepted connections", stats.connects);
        counter(b, "bang_conmgr_disconnects_total", "connections lost to network errors",
                stats.disconnects);
        counter(b, "bang_conmgr_closes_total", "orderly connection closes", stats.closes);
        counter(b, "bang_conmgr_events_total", "connection manager event loops", stats.eventCount);
        counter(b, "bang_conmgr_bytes_in_total", "bytes read", stats.bytesIn);
        counter(b, "bang_conmgr_bytes_out_total", "bytes written", stats.bytesOut);
        counter(b, "bang_conmgr_msgs_in_total", "messages read", stats.msgsIn);
        counter(b, "bang_conmgr_msgs_out_total", "messages written", stats.msgsOut);
        gauge(b, "bang_conmgr_out_queue", "outgoing message queue depth", stats.outQueueSize);
        gauge(b, "bang_conmgr_death_queue", "connections awaiting cleanup", stats.deathQueueSize);
        gauge(b, "bang_conmgr_overflow_queue", "messages in per-connection overflow queues",
              stats.overQueueSize);

        respond(exchange, 200, "text/plain; version=0.0.4; charset=utf-8", b.toString());
    }

    protected void gauge (StringBuilder b, String name, String help, double value)
    {
        metric(b, name, "gauge", help, value);
    }

    protected void counter (StringBuilder b, String name, String help, double value)
    {
        metric(b, name, "counter", help, value);
    }

    protected void metric (StringBuilder b, String name, String type, String help, double value)
    {
        b.append("# HELP ").append(name).append(' ').append(help).append('\n');
        b.append("# TYPE ").append(name).append(' ').append(type).append('\n');
        b.append(name).append("{node=\"").append(_node).append("\"} ");
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            b.append((long)value);
        } else {
            b.append(String.format(Locale.ROOT, "%.3f", value));
        }
        b.append('\n');
    }

    protected static void respond (HttpExchange exchange, int code, String contentType, String body)
        throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    protected final String _node;
    protected final PresentsConnectionManager _conmgr;
    protected final ClientManager _clmgr;
    protected final HttpServer _httpd;
    protected final ExecutorService _exec;
}
