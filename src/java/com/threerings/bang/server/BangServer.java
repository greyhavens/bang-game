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
import com.threerings.crowd.server.CrowdClient;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

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

    /**
     * Initializes all of the server services and prepares for operation.
     */
    public void init ()
        throws Exception
    {
        // do the base server initialization
        super.init();

//         // configure the client manager to use the appropriate client class
//         clmgr.setClientClass(ToyBoxClient.class);

//         // configure the client manager to use our resolver
//         clmgr.setClientResolverClass(ToyBoxClientResolver.class);

//         // configure the dobject manager with our access controller
//         omgr.setDefaultAccessController(ToyBoxObjectAccess.DEFAULT);

//         // create our database connection provider
//         conprov = new StaticConnectionProvider(ToyBoxConfig.getJDBCConfig());

//         // set up our authenticator
//         Authenticator auth = ToyBoxConfig.getAuthenticator();
//         if (auth != null) {
//             conmgr.setAuthenticator(auth);
//         }

        // initialize our managers
        parmgr.init(invmgr, plreg);

        // TODO: create a lobby

        log.info("Bang server initialized.");
    }

//     /**
//      * Returns the port on which the connection manager will listen for
//      * client connections.
//      */
//     protected int getListenPort ()
//     {
//         return ToyBoxConfig.getServerPort();
//     }

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
