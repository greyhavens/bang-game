//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.bundle.BundledComponentRepository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.crowd.peer.server.CrowdPeerManager;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.user.AccountActionRepository;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberConfig;
import com.threerings.bang.avatar.server.BarberManager;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.admin.data.StatusObject;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.bank.data.BankConfig;
import com.threerings.bang.bank.server.BankManager;
import com.threerings.bang.ranch.data.RanchConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.saloon.data.SaloonConfig;
import com.threerings.bang.saloon.server.SaloonManager;
import com.threerings.bang.store.data.StoreConfig;
import com.threerings.bang.store.server.StoreManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TownObject;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.RatingRepository;
import com.threerings.bang.server.persist.StatRepository;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Creates and manages all the services needed on the bang server.
 */
public class BangServer extends CrowdServer
{
    /** The connection provider used to obtain access to our JDBC
     * databases. */
    public static ConnectionProvider conprov;

    /** A resource manager with which we can load resources in the same manner
     * that the client does (for resources that are used on both the server and
     * client). */
    public static ResourceManager rsrcmgr;

    /** Maintains a registry of runtime configuration information. */
    public static ConfigRegistry confreg;

    /** Provides information on our character components. */
    public static ComponentRepository comprepo;

    /** Handles the heavy lifting relating to avatar looks and articles. */
    public static AvatarLogic alogic;

    /** Communicates with the other servers in our cluster. */
    public static CrowdPeerManager peermgr;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    /** Handles the processing of account actions. */
    public static AccountActionManager actionmgr;

    /** Manages global player related bits. */
    public static PlayerManager playmgr;

    /** Manages the persistent repository of player data. */
    public static PlayerRepository playrepo;

    /** Manages the persistent repository of items. */
    public static ItemRepository itemrepo;

    /** Manages the persistent repository of stats. */
    public static StatRepository statrepo;

    /** Manages the persistent repository of ratings. */
    public static RatingRepository ratingrepo;

    /** Manages the persistent repository of avatar looks. */
    public static LookRepository lookrepo;

    /** Provides micropayment services. (This will need to be turned into a
     * pluggable interface to support third party micropayment systems.) */
    public static BangCoinManager coinmgr;

    /** Manages the market for exchange between scrips and coins. */
    public static BangCoinExchangeManager coinexmgr;

    /** Keeps an eye on the Ranch, a good man to have around. */
    public static RanchManager ranchmgr;

    /** Manages the Saloon and match-making. */
    public static SaloonManager saloonmgr;

    /** Manages the General Store and item purchase. */
    public static StoreManager storemgr;

    /** Manages the Bank and the coin exchange. */
    public static BankManager bankmgr;

    /** Manages the Barber and avatar customization. */
    public static BarberManager barbermgr;

    /** Manages our selection of game boards. */
    public static BoardManager boardmgr = new BoardManager();

    /** Contains server status information published to admins. */
    public static StatusObject statobj;

    /** Contains information about the whole town. */
    public static TownObject townobj;

    @Override // documentation inherited
    public void init ()
        throws Exception
    {
        // create out database connection provider 
        // this must be done before calling super.init()
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());

        // do the base server initialization
        super.init();

        // configure the client manager to use the appropriate client class
        clmgr.setClientFactory(new ClientFactory() {
            public PresentsClient createClient (AuthRequest areq) {
                return new BangClient();
            }
            public ClientResolver createClientResolver (Name username) {
                return new BangClientResolver();
            }
        });

        // create our resource manager and other resource bits
        rsrcmgr = new ResourceManager("rsrc");
        rsrcmgr.initBundles(null, "config/resource/manager.properties", null);

        // create our avatar related bits
        comprepo = new BundledComponentRepository(
            rsrcmgr, null, AvatarCodes.AVATAR_RSRC_SET);
        alogic = new AvatarLogic(rsrcmgr, comprepo);

        // create our repositories
        playrepo = new PlayerRepository(conprov);
        itemrepo = new ItemRepository(conprov);
        statrepo = new StatRepository(conprov);
        ratingrepo = new RatingRepository(conprov);
        lookrepo = new LookRepository(conprov);
        AccountActionRepository actionrepo =
            new AccountActionRepository(conprov);

        // create our various supporting managers
        playmgr = new PlayerManager();
        coinmgr = new BangCoinManager(conprov, actionrepo);
        coinexmgr = new BangCoinExchangeManager(conprov);
        actionmgr = new AccountActionManager(omgr, actionrepo);

        // if we have a shared secret, assume we're running in a cluster
        String node = System.getProperty("node");
        if (node != null && ServerConfig.sharedSecret != null) {
            log.info("Running in cluster mode as node '" +
                     ServerConfig.serverName + "'.");
            peermgr = new CrowdPeerManager(conprov, invoker);
        }

        // create and set up our configuration registry and admin service
        confreg = new DatabaseConfigRegistry(conprov, invoker);
        AdminProvider.init(invmgr, confreg);

        // now initialize our runtime configuration, postponing the remaining
        // server initialization until our configuration objects are available
        RuntimeConfig.init(omgr);
        omgr.postRunnable(new Runnable () {
            public void run () {
                try {
                    finishInit();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Server initialization failed.", e);
                    System.exit(-1);
                }
            }
        });
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        super.shutdown();

        // logoff of our peer nodes
        if (peermgr != null) {
            peermgr.shutdown();
        }

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
        _plog.close();
        BangCoinManager.coinlog.close();
    }

    @Override // documentation inherited
    protected BodyLocator createBodyLocator ()
    {
        return new BodyLocator() {
            public BodyObject get (Name visibleName) {
                return _players.get(visibleName);
            }
        };
    }

    /**
     * This is called once our runtime configuration is available.
     */
    protected void finishInit ()
        throws Exception
    {
        // initialize our managers
        parmgr.init(invmgr, plreg);
        boardmgr.init(conprov);
        playmgr.init(conprov);
        coinexmgr.init();
        if (peermgr != null) {
            peermgr.init(ServerConfig.serverName, ServerConfig.sharedSecret,
                         ServerConfig.hostname, getListenPorts()[0]);
        }

        // create our managers
        PlaceRegistry.CreationObserver crobs =
            new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                if (pmgr instanceof SaloonManager) {
                    saloonmgr = (SaloonManager)pmgr;
                } else if (pmgr instanceof StoreManager) {
                    storemgr = (StoreManager)pmgr;
                } else if (pmgr instanceof BankManager) {
                    bankmgr = (BankManager)pmgr;
                } else if (pmgr instanceof RanchManager) {
                    ranchmgr = (RanchManager)pmgr;
                } else if (pmgr instanceof BarberManager) {
                    barbermgr = (BarberManager)pmgr;
                }
            }
        };
        plreg.createPlace(new SaloonConfig(), crobs);
        plreg.createPlace(new StoreConfig(), crobs);
        plreg.createPlace(new BankConfig(), crobs);
        plreg.createPlace(new RanchConfig(), crobs);
        plreg.createPlace(new BarberConfig(), crobs);

        // create our server status object
        omgr.createObject(StatusObject.class, new Subscriber<StatusObject>() {
            public void objectAvailable (StatusObject object) {
                statobj = object;
                statobj.serverStartTime = System.currentTimeMillis();
                // start up our connection manager stat monitor
                _conmgrStatsUpdater.schedule(5000L, true);
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Unable to create status object: " + cause + ".");
            }
        });

        // create the town object and an interval to keep it up-to-date
        omgr.createObject(TownObject.class, new Subscriber<TownObject>() {
            public void objectAvailable (TownObject object) {
                townobj = object;
                createTownObjectUpdateInterval();
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to create town object: " + cause + ".");
            }
        });

        log.info("Bang server v" + DeploymentConfig.getVersion() +
                 " initialized.");
    }

    /**
     * Creates the interval that updates the town object's population
     * once every thirty seconds.
     */
    protected void createTownObjectUpdateInterval ()
    {
        new Interval(omgr) {
            public void expired () {
                int npop = _players.size();
                if (npop != townobj.population) {
                    townobj.setPopulation(npop);
                }
            }
        }.schedule(30000L, true);
    }

    /**
     * Returns the player object for the specified user if they are online
     * currently, null otherwise. This should only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupPlayer (Handle handle)
    {
        return _players.get(handle);
    }

    /**
     * Returns the player object for the specified id if they are online
     * currently, null otherwise. This should only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupPlayer (int playerId)
    {
        return _playerIds.get(playerId);
    }

    /**
     * Returns the player object for the specified user if they are online
     * currently, null otherwise. This should only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupByAccountName (Name accountName)
    {
        return (PlayerObject)clmgr.getClientObject(accountName);
    }

    /**
     * Called when a player starts their session (or after they choose a handle
     * for players on their first session) to associate the handle with the
     * player's distributed object.
     */
    public static void registerPlayer (PlayerObject player)
    {
        _players.put(player.handle, player);
        _playerIds.put(player.playerId, player);

        // update our players online count in the status object
        statobj.setPlayersOnline(clmgr.getClientCount());
    }

    /**
     * Called when a player ends their session to clear their handle to player
     * object mapping.
     */
    public static void clearPlayer (PlayerObject player)
    {
        _players.remove(player.handle);
        _playerIds.remove(player.playerId);

        // update our players online count in the status object
        statobj.setPlayersOnline(clmgr.getClientCount());
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
     * Loads a message to the client performance log.
     */
    public static void perfLog (String message)
    {
        _plog.log(message);
    }

    /**
     * Creates an audit log with the specified name (which should includ the
     * <code>.log</code> suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        // qualify our log file with the hostname to avoid collisions
        logname = logname + "_" + ServerConfig.hostname;
        return new AuditLogger(_logdir, logname + ".log");
    }

    @Override // documentation inherited
    protected Authenticator createAuthenticator ()
    {
        // set up our authenticator
        Authenticator auth = ServerConfig.getAuthenticator();
        if (auth != null) {
            return auth;
        }
        return super.createAuthenticator();
    }

    @Override // documentation inherited
    protected int[] getListenPorts ()
    {
        return ServerConfig.serverPorts;
    }

    @Override // documentation inherited
    protected void logReport (String report)
    {
        _stlog.log(report);
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
            log.log(Level.WARNING, "Server initialization failed.", e);
            System.exit(-1);
        }
    }

    /** This reads the status from the connection manager and stuffs it
     * into our server status object every 5 seconds. Because it reads
     * synchronized data and then just posts an event, it's OK that it
     * runs directly on the Interval dispatch thread. */
    protected Interval _conmgrStatsUpdater = new Interval() {
        public void expired () {
            statobj.setConnStats(conmgr.getStats());
        }
    };

    protected static HashMap<Handle,PlayerObject> _players =
        new HashMap<Handle,PlayerObject>();
    protected static HashIntMap<PlayerObject> _playerIds =
        new HashIntMap<PlayerObject>();

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
    protected static AuditLogger _plog = createAuditLog("perf");
}
