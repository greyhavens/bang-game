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

import com.threerings.user.AccountActionRepository;

import com.threerings.bang.bank.data.BankConfig;
import com.threerings.bang.bank.server.BankManager;
import com.threerings.bang.lobby.data.LobbyConfig;
import com.threerings.bang.lobby.server.LobbyManager;
import com.threerings.bang.ranch.data.RanchConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.store.data.StoreConfig;
import com.threerings.bang.store.server.StoreManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.StatRepository;

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

    /** Manages the persistent repository of stats. */
    public static StatRepository statrepo;

    /** Provides micropayment services. (This will need to be turned into a
     * pluggable interface to support third party micropayment systems.) */
    public static BangCoinManager coinmgr;

    /** Manages the market for exchange between scrips and coins. */
    public static BangCoinExchangeManager coinexmgr;

    /** Keeps an eye on the Ranch, a good man to have around. */
    public static RanchManager ranchmgr;

    /** Manages the Saloon and match-making. */
    public static LobbyManager saloonmgr;

    /** Manages the General Store and item purchase. */
    public static StoreManager storemgr;

    /** Manages the Bank and the coin exchange. */
    public static BankManager bankmgr;

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
        statrepo = new StatRepository(conprov);
        coinmgr = new BangCoinManager(conprov);
        coinexmgr = new BangCoinExchangeManager(conprov);

        // set up our authenticator
        Authenticator auth = ServerConfig.getAuthenticator();
        if (auth != null) {
            conmgr.setAuthenticator(auth);
        }

        // initialize our managers
        parmgr.init(invmgr, plreg);
        boardmgr.init(conprov);
        coinexmgr.init();

        // create the saloon manager
        LobbyConfig lconfig = new LobbyConfig();
        lconfig.townId = ServerConfig.getTownId();
        plreg.createPlace(lconfig, new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                saloonmgr = (LobbyManager)pmgr;
            }
        });

        // create the general store manager
        StoreConfig sconfig = new StoreConfig();
        plreg.createPlace(sconfig, new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                storemgr = (StoreManager)pmgr;
            }
        });

        // create the bank manager
        BankConfig bconfig = new BankConfig();
        plreg.createPlace(bconfig, new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                bankmgr = (BankManager)pmgr;
            }
        });

        // create the ranch manager
        RanchConfig rconfig = new RanchConfig();
        plreg.createPlace(rconfig, new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                ranchmgr = (RanchManager)pmgr;
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

    /**
     * Creates an audit log with the specified name (which should includ the
     * <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        return new AuditLogger(_logdir, logname);
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
    protected static AuditLogger _glog = createAuditLog("server.log");
    protected static AuditLogger _ilog = createAuditLog("item.log");
}
