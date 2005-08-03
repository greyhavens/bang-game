//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.presents.server.Authenticator;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.coin.server.persist.CoinRepository;
import com.threerings.user.AccountActionRepository;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.lobby.data.LobbyConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;

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

    /** Provides visibility into global OOO account actions. */
    public static AccountActionRepository actionrepo;

    /** Manages the persistent repository of player data. */
    public static PlayerRepository playrepo;

    /** Manages the persistent repository of items. */
    public static ItemRepository itemrepo;

    /** Provides micropayment services. (This will need to be turned into
     * a pluggable interface to support third party micropayment
     * systems.) */
    public static CoinRepository coinrepo;

    /** Keeps an eye on the ranch, a good man to have around. */
    public static RanchManager ranchmgr = new RanchManager();

    /** Manages our selection of game boards. */
    public static BoardManager boardmgr = new BoardManager();

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

        // create our database connection provider and repositories
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        actionrepo = new AccountActionRepository(conprov);
        playrepo = new PlayerRepository(conprov);
        itemrepo = new ItemRepository(conprov);
        coinrepo = new CoinRepository(
            conprov, ServerConfig.serverName, _clog, actionrepo);

        // set up our authenticator
        Authenticator auth = ServerConfig.getAuthenticator();
        if (auth != null) {
            conmgr.setAuthenticator(auth);
        }

        // initialize our managers
        parmgr.init(invmgr, plreg);
        ranchmgr.init(invmgr);
        boardmgr.init(conprov);

        // create a lobby (TODO: redo all this)
        LobbyConfig lconfig = new LobbyConfig();
        lconfig.townId = BangCodes.FRONTIER_TOWN;
        plreg.createPlace(lconfig, new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                log.info("Created " + pmgr.where() + ".");
            }
        });

        log.info("Bang server initialized.");
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        super.shutdown();

        // close our audit logs
        _glog.close();
        _ilog.close();
        _clog.close();
    }

    /**
     * Loads a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Loads a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        _ilog.log(message);
    }

    @Override // documentation inherited
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
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

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = new AuditLogger(_logdir, "server.log");
    protected static AuditLogger _ilog = new AuditLogger(_logdir, "item.log");
    protected static AuditLogger _clog = new AuditLogger(_logdir, "coin.log");
}
