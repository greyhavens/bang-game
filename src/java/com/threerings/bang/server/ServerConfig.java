//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.server.Authenticator;

import com.threerings.bang.data.BangCodes;

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

    /** The back-channel DNS name of the host on which this server is
     * running. */
    public static String hostname;

    /** The publicly accessible DNS name of the host on which this server is
     * running. */
    public static String publicHostname;

    /** The id of the town this server is handling. */
    public static String townId;

    /** Eventually we'll have servers for the "town" and servers for games
     * started from that town. For now all servers are town servers and games
     * are played on the town server. */
    public static boolean isTownServer = true;

    /** The secret used to authenticate other servers in our cluster. */
    public static String sharedSecret;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** Provides access to our config properties. <em>Do not</em> modify
     * these properties! */
    public static Config config;

    /**
     * Returns the JDBC configuration.
     */
    public static Properties getJDBCConfig ()
    {
        return config.getSubProperties("db");
    }

    /**
     * Instantiates and returns the authenticator that the server will use
     * to authenticate client connections.
     */
    public static Authenticator getAuthenticator ()
    {
        String aclass = config.getValue("server_auth", "");
        try {
            if (!StringUtil.isBlank(aclass)) {
                return (Authenticator)Class.forName(aclass).newInstance();
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to instantiate authenticator " +
                    "[class=" + aclass + "]", e);
        }
        return null;
    }

    /**
     * Configures the install config with the path to our installation
     * properties file. This method is called automatically.
     */
    protected static void init (String propPath)
    {
        Properties props = new Properties();
        try {
            if (propPath != null) {
                propPath = propPath + File.separator + "server.properties";
                props.load(new FileInputStream(propPath));
            } else {
                propPath = "server.properties";
                props.load(ServerConfig.class.getClassLoader().
                           getResourceAsStream(propPath));
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load install properties " +
                    "[path=" + propPath + "].", e);
        }
        config = new Config("server", props);

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        sharedSecret = config.getValue("server_secret", (String)null);

        // if we're not a server node (we're the webapp or a tool) stop here
        if (!Boolean.getBoolean("is_node")) {
            return;
        }

        // our server name and hostname is our node identifier and comes from a
        // system property passed by our startup scripts
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
        townId = config.getValue(
            nodename + ".town_id", BangCodes.FRONTIER_TOWN);
    }

    static {
        init(System.getProperty("bang.home"));
    }
}
