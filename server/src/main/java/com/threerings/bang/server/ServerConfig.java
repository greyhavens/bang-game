//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.threerings.presents.server.Authenticator;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular Bang! server installation are accessed via
 * this class.
 */
public class ServerConfig
{
    /** The name assigned to this server node. */
    public static String nodename;

    /** The back-channel DNS name of the host on which this server is running. */
    public static String hostname;

    /** The publicly accessible DNS name of the host on which this server is running. */
    public static String publicHostname;

    /** The id of the town this server is handling. */
    public static String townId;

    /** The index of the town this server is handling. */
    public static int townIndex;

    /** Eventually we'll have servers for the "town" and servers for games started from that
     * town. For now all servers are town servers and games are played on the town server. */
    public static boolean isTownServer = true;

    /** The secret used to authenticate other servers in our cluster. */
    public static String sharedSecret;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** Provides access to our config properties. <em>Do not</em> modify these properties! */
    public static Config config = loadConfig();

    /**
     * Loads the baked {@code server.properties} defaults, then overlays environment-variable
     * overrides so container images can stay environment-agnostic (12-factor). For any configured
     * key {@code foo.bar}, an env var {@code BANG_FOO_BAR} (key upper-cased, {@code .} and {@code -}
     * mapped to {@code _}) wins over the baked default. This is how secrets (DB passwords, the
     * cluster {@code server_secret}, the TLS keystore password) and per-node hosts are injected in
     * production without baking them into the image.
     */
    protected static Config loadConfig ()
    {
        // load the baked defaults exactly as before, then copy them into a mutable Properties
        Config base = new Config("server");
        Properties props = new Properties();
        for (Iterator<String> iter = base.keys(); iter.hasNext(); ) {
            String key = iter.next();
            String value = base.getValue(key, (String)null);
            if (value != null) {
                props.setProperty(key, value);
            }
        }

        // overlay env-var overrides (env wins over the baked default)
        for (String key : props.stringPropertyNames()) {
            String env = System.getenv("BANG_" + key.toUpperCase().replace('.', '_').replace('-', '_'));
            if (env != null) {
                props.setProperty(key, env);
            }
        }
        return new Config(props);
    }

    /**
     * Returns the JDBC configuration.
     */
    public static Properties getJDBCConfig ()
    {
        return config.getSubProperties("db");
    }

    /**
     * Instantiates and returns the authenticator that the server will use to authenticate client
     * connections.
     */
    public static Class<? extends Authenticator> getAuthenticator ()
    {
        String aclass = config.getValue("server_auth", "");
        if (StringUtil.isBlank(aclass)) {
            // Fail fast with an actionable message rather than returning null (which would reach
            // Guice's bind(Authenticator.class).to(null) in BangServer$Module.configure and NPE far
            // from the cause). A blank value almost always means local config is missing.
            throw new IllegalStateException(
                "server_auth is not configured. Copy etc/*.dist to etc/test/ " +
                "(see README / docs/running-the-game.md), or set server_auth in " +
                "etc/test/server.properties.");
        }
        try {
            @SuppressWarnings("unchecked") Class<? extends Authenticator> clazz =
                (Class<? extends Authenticator>)Class.forName(aclass);
            return clazz;
        } catch (Exception e) {
            // Don't swallow this and return null; rethrow with the class name and original cause.
            throw new IllegalStateException(
                "Failed to load authenticator class '" + aclass + "' (configured as server_auth).", e);
        }
    }

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        sharedSecret = config.getValue("server_secret", (String)null);

        // if we're a server node (not the webapp or a tool) do some extra stuff
        if (Boolean.getBoolean("is_node")) {
            // our server name and hostname is our node identifier and comes from a system property
            // passed by our startup scripts
            nodename = System.getProperty("node");
            if (StringUtil.isBlank(nodename)) {
                log.warning("Missing 'node' system property. Cannot start.");
            }
            hostname = System.getProperty("hostname");
            if (StringUtil.isBlank(hostname)) {
                log.warning("Missing 'hostname' system property. Cannot start.");
            }
            if (StringUtil.isBlank(nodename) || StringUtil.isBlank(hostname)) {
                System.exit(-1);
            }

            // fill in our node-specific properties
            publicHostname = config.getValue(nodename + ".server_host", hostname);
            townId = config.getValue(nodename + ".town_id", BangCodes.FRONTIER_TOWN);
            townIndex = BangUtil.getTownIndex(townId);
        }
    }
}
