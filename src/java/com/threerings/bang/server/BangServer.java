//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.bang.lobby.data.LobbyConfig;

import static com.threerings.bang.Log.log;

/**
 * Creates and manages all the services needed on the bang server.
 */
public class BangServer extends CrowdServer
{
    /** The connection provider used to obtain access to our JDBC
     * databases. */
    public static ConnectionProvider conprov;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    @Override // documentation inherited
    public void init ()
        throws Exception
    {
        // do the base server initialization
        super.init();

        // configure the client manager to use the appropriate client class
        clmgr.setClientClass(BangClient.class);

        // configure the client manager to use our resolver
        clmgr.setClientResolverClass(BangClientResolver.class);

        // create our database connection provider
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        // set up our authenticator
        Authenticator auth = ServerConfig.getAuthenticator();
        if (auth != null) {
            conmgr.setAuthenticator(auth);
        }

        // initialize our managers
        parmgr.init(invmgr, plreg);

        // create a lobby
        plreg.createPlace(new LobbyConfig(),
                          new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                log.info("Created " + pmgr.where() + ".");
            }
        });

        log.info("Bang server initialized.");
    }

    /**
     * Loads a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        // TODO: log this message to an audit log
    }

    /**
     * Loads a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        // TODO: log this message to an audit log
    }

    @Override // documentation inherited
    protected int getListenPort ()
    {
        return ServerConfig.getServerPort();
    }

    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        BangServer server = new BangServer();
        try {
            server.init();
            server.run();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server.", e);
        }
    }
}
