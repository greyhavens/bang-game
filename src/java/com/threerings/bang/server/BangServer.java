//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.TransitionRepository;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.util.AuditLogger;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.ObserverList;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.Tuple;

import com.threerings.admin.server.AdminProvider;
import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.bundle.BundledComponentRepository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.user.AccountActionRepository;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberConfig;
import com.threerings.bang.avatar.server.BarberManager;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.bank.data.BankConfig;
import com.threerings.bang.bank.server.BankManager;
import com.threerings.bang.bounty.data.OfficeConfig;
import com.threerings.bang.bounty.server.OfficeManager;
import com.threerings.bang.bounty.server.persist.BountyRepository;
import com.threerings.bang.chat.server.BangChatProvider;
import com.threerings.bang.gang.data.HideoutConfig;
import com.threerings.bang.gang.server.GangManager;
import com.threerings.bang.gang.server.HideoutManager;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.ranch.data.RanchConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.saloon.data.SaloonConfig;
import com.threerings.bang.saloon.server.SaloonManager;
import com.threerings.bang.station.data.StationConfig;
import com.threerings.bang.station.server.StationManager;
import com.threerings.bang.store.data.StoreConfig;
import com.threerings.bang.store.server.StoreManager;
import com.threerings.bang.tourney.server.BangTourniesManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TownObject;
import com.threerings.bang.server.persist.BangStatRepository;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.RatingRepository;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Creates and manages all the services needed on the bang server.
 */
public class BangServer extends CrowdServer
{
    /**
     * Implemented by objects that wish to be notified when players log on and off,
     * or changes their handle.
     */
    public static interface PlayerObserver
    {
        /**
         * Called when a player logs on.
         */
        public void playerLoggedOn (PlayerObject user);

        /**
         * Called when a player logs off.
         */
        public void playerLoggedOff (PlayerObject user);

        /**
         * Called when a player changes their handle.
         */
        public void playerChangedHandle (PlayerObject user, Handle oldHandle);
    }

    /** The connection provider used to obtain access to our JDBC databases. */
    public static ConnectionProvider conprov;

    /** Used to provide database access to our Depot repositories. */
    public static PersistenceContext perCtx;

    /** Used to coordinate transitions to persistent data. */
    public static TransitionRepository transitrepo;

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

    /** Any database actions that involve the authentication database <em>must</em> be run on this
     * invoker to avoid blocking normal game database actions. */
    public static Invoker authInvoker;

    /** A reference to the authenticator in use by the server. */
    public static BangAuthenticator author;

    /** Communicates with the other servers in our cluster. */
    public static BangPeerManager peermgr;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    /** Handles the processing of account actions. */
    public static AccountActionManager actionmgr;

    /** Manages global player related bits. */
    public static PlayerManager playmgr;

    /** Manages gangs. */
    public static GangManager gangmgr;

    /** Manages tournaments. */
    public static BangTourniesManager tournmgr;

    /** Manages rating bits. */
    public static RatingManager ratingmgr;

    /** Manages the persistent repository of player data. */
    public static PlayerRepository playrepo;

    /** Manages the persistent repository of gang data. */
    public static GangRepository gangrepo;

    /** Manages the persistent repository of items. */
    public static ItemRepository itemrepo;

    /** Manages the persistent repository of stats. */
    public static BangStatRepository statrepo;

    /** Manages the persistent repository of ratings. */
    public static RatingRepository ratingrepo;

    /** Manages the persistent repository of avatar looks. */
    public static LookRepository lookrepo;

    /** Tracks bounty related persistent statistics. */
    public static BountyRepository bountyrepo;

    /** Provides micropayment services. (This will need to be turned into a
     * pluggable interface to support third party micropayment systems.) */
    public static BangCoinManager coinmgr;

    /** Manages the market for exchange between scrips and coins. */
    public static BangCoinExchangeManager coinexmgr;

    /** Manages administrative services. */
    public static BangAdminManager adminmgr;

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

    /** Manages the Train Station and inter-town travel. */
    public static StationManager stationmgr;

    /** Manages the Hideout and Gangs. */
    public static HideoutManager hideoutmgr;

    /** Manages the Sheriff's Office and Bounties. */
    public static OfficeManager officemgr;

    /** Manages our selection of game boards. */
    public static BoardManager boardmgr = new BoardManager();

    /** Manages tracking and discouraging of misbehaving players. */
    public static NaughtyPlayerManager npmgr = new NaughtyPlayerManager();

    /** Contains information about the whole town. */
    public static TownObject townobj;

    /**
     * Ensures that the calling thread is the distributed object event dispatch thread, throwing an
     * {@link IllegalStateException} if it is not.
     */
    public static void requireDObjThread ()
    {
        if (!omgr.isDispatchThread()) {
            String errmsg = "This method must be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Ensures that the calling thread <em>is not</em> the distributed object event dispatch
     * thread, throwing an {@link IllegalStateException} if it is.
     */
    public static void refuseDObjThread ()
    {
        if (omgr.isDispatchThread()) {
            String errmsg = "This method must not be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    @Override // documentation inherited
    public void init ()
        throws Exception
    {
        // create out database connection provider this must be done before calling super.init()
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        perCtx = new PersistenceContext("bangdb", conprov);

        // create our transition manager prior to doing anything else
        transitrepo = new TransitionRepository(conprov);

        // TEMP: the authenticator is going to access the player repository, so it needs to
        // be created here
        playrepo = new PlayerRepository(conprov);

        // do the base server initialization
        super.init();

        // create and start up our auth invoker
        authInvoker = new Invoker("auth_invoker", omgr);
        authInvoker.setDaemon(true);
        authInvoker.start();

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
        comprepo = new BundledComponentRepository(rsrcmgr, null, AvatarCodes.AVATAR_RSRC_SET);
        alogic = new AvatarLogic(rsrcmgr, comprepo);

        // create our repositories
        itemrepo = new ItemRepository(conprov);
        gangrepo = new GangRepository(conprov);
        statrepo = new BangStatRepository(perCtx);
        ratingrepo = new RatingRepository(conprov);
        lookrepo = new LookRepository(conprov);
        bountyrepo = new BountyRepository(conprov);
        AccountActionRepository actionrepo = new AccountActionRepository(conprov);

        // create our various supporting managers
        playmgr = new PlayerManager();
        gangmgr = new GangManager();
        tournmgr = new BangTourniesManager(conprov);
        ratingmgr = new RatingManager();
        coinmgr = new BangCoinManager(conprov, actionrepo);
        coinexmgr = new BangCoinExchangeManager(conprov);
        actionmgr = new AccountActionManager(omgr, actionrepo);
        adminmgr = new BangAdminManager();

        // if we have a shared secret, assume we're running in a cluster
        String node = System.getProperty("node");
        if (node != null && ServerConfig.sharedSecret != null) {
            log.info("Running in cluster mode as node '" + ServerConfig.nodename + "'.");
            peermgr = new BangPeerManager();
        }

        // create and set up our configuration registry and admin service
        confreg = new DatabaseConfigRegistry(perCtx, invoker, ServerConfig.nodename);
        AdminProvider.init(invmgr, confreg);

        // now initialize our runtime configuration, postponing the remaining server initialization
        // until our configuration objects are available
        RuntimeConfig.init(omgr);
        omgr.postRunnable(new PresentsDObjectMgr.LongRunnable () {
            public void run () {
                try {
                    finishInit();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Server initialization failed.", e);
                    System.exit(-1);
                }
            }
        });

        // start up an interval that checks to see if our code has changed and auto-restarts the
        // server as soon as possible when it has
        if (ServerConfig.config.getValue("auto_restart", false)) {
            _codeModified = new File(ServerConfig.serverRoot, "dist/bang-code.jar").lastModified();
            new Interval(omgr) {
                public void expired () {
                    checkAutoRestart();
                }
            }.schedule(AUTO_RESTART_CHECK_INTERVAL, true);
        }
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

    @Override // documentation inherited
    protected ChatProvider createChatProvider ()
    {
        return new BangChatProvider();
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
        gangmgr.init(conprov);
        tournmgr.init();
        ratingmgr.init(conprov);
        coinexmgr.init();
        adminmgr.init(this);
        if (peermgr != null) {
            peermgr.init(perCtx, invoker, ServerConfig.nodename, ServerConfig.sharedSecret,
                         ServerConfig.hostname, ServerConfig.publicHostname, getListenPorts()[0]);
        }

        // create our managers
        saloonmgr = (SaloonManager)plreg.createPlace(new SaloonConfig());
        storemgr = (StoreManager)plreg.createPlace(new StoreConfig());
        bankmgr = (BankManager)plreg.createPlace(new BankConfig());
        ranchmgr = (RanchManager)plreg.createPlace(new RanchConfig());
        barbermgr = (BarberManager)plreg.createPlace(new BarberConfig());
        stationmgr = (StationManager)plreg.createPlace(new StationConfig());
        hideoutmgr = (HideoutManager)plreg.createPlace(new HideoutConfig());
        officemgr = (OfficeManager)plreg.createPlace(new OfficeConfig());

        // create the town object and an interval to keep it up-to-date
        townobj = omgr.registerObject(new TownObject());
        createTownObjectUpdateInterval();

        log.info("Bang server v" + DeploymentConfig.getVersion() + " initialized.");
    }

    /**
     * Creates the interval that updates the town object's population once every thirty seconds.
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
     * Registers a player observer.
     */
    public static void addPlayerObserver (PlayerObserver observer)
    {
        _playobs.add(observer);
    }

    /**
     * Removes a player observer registration.
     */
    public static void removePlayerObserver (PlayerObserver observer)
    {
        _playobs.remove(observer);
    }

    /**
     * Returns the player object for the specified user if they are online currently, null
     * otherwise. This should only be called from the dobjmgr thread.
     */
    public static PlayerObject lookupPlayer (Handle handle)
    {
        return _players.get(handle);
    }

    /**
     * Returns the player object for the specified id if they are online currently, null
     * otherwise. This should only be called from the dobjmgr thread.
     */
    public static PlayerObject lookupPlayer (int playerId)
    {
        return _playerIds.get(playerId);
    }

    /**
     * Returns the player object for the specified user if they are online currently, null
     * otherwise. This should only be called from the dobjmgr thread.
     */
    public static PlayerObject lookupByAccountName (Name accountName)
    {
        return (PlayerObject)clmgr.getClientObject(accountName);
    }

    /**
     * Called when a player starts their session to associate the handle with the player's
     * distributed object.
     */
    public static void registerPlayer (final PlayerObject player)
    {
        _players.put(player.handle, player);
        _playerIds.put(player.playerId, player);

        // update our players online count in the status object
        adminmgr.statobj.updatePlayersOnline(clmgr.getClientCount());

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerLoggedOn(player);
                return true;
            }
        });
    }

    /**
     * Called when a player sets their handle for the first time, or changes it later on,
     * to change the handle association.
     */
    public static void updatePlayer (final PlayerObject player, final Handle oldHandle)
    {
        _players.remove(oldHandle);
        _players.put(player.handle, player);

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerChangedHandle(player, oldHandle);
                return true;
            }
        });
    }

    /**
     * Called when a player ends their session to clear their handle to player object mapping.
     */
    public static void clearPlayer (final PlayerObject player)
    {
        _players.remove(player.handle);
        _playerIds.remove(player.playerId);

        // update our players online count in the status object
        adminmgr.statobj.updatePlayersOnline(clmgr.getClientCount());

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerLoggedOff(player);
                return true;
            }
        });
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
     * Creates an audit log with the specified name (which should includ the <code>.log</code>
     * suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        // qualify our log file with the nodename to avoid collisions
        logname = logname + "_" + ServerConfig.nodename;
        return new AuditLogger(_logdir, logname + ".log");
    }

    @Override // documentation inherited
    protected Authenticator createAuthenticator ()
    {
        // set up our authenticator (and keep a reference to it)
        author = ServerConfig.getAuthenticator();
        if (author == null) {
            throw new RuntimeException("Unable to create authenticator. We're doomed!");
        }
        return author;
    }

    @Override // documentation inherited
    protected int[] getListenPorts ()
    {
        return DeploymentConfig.getServerPorts(ServerConfig.townId);
    }

    @Override // documentation inherited
    protected void logReport (String report)
    {
        _stlog.log(report);
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context
        perCtx.shutdown();

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
        _plog.close();
        BangCoinManager.coinlog.close();
    }

    protected void checkAutoRestart ()
    {
        long lastModified = new File(ServerConfig.serverRoot, "dist/bang-code.jar").lastModified();
        if (lastModified > _codeModified) {
            int players = 0;
            for (Iterator<ClientObject> iter = clmgr.enumerateClientObjects(); iter.hasNext(); ) {
                if (iter.next() instanceof PlayerObject) {
                    players++;
                }
            }
            if (players == 0) {
                adminmgr.scheduleReboot(0, "codeUpdateAutoRestart");
            }
        }
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
            // annoyingly some background threads are hanging, so stick a fork in them for the time
            // being; when run() returns the dobj mgr and invoker thread will already have exited
            System.exit(0);
        } catch (Exception e) {
            log.log(Level.WARNING, "Server initialization failed.", e);
            System.exit(-1);
        }
    }

    protected long _codeModified;

    protected static HashMap<Handle,PlayerObject> _players = new HashMap<Handle,PlayerObject>();
    protected static HashIntMap<PlayerObject> _playerIds = new HashIntMap<PlayerObject>();

    protected static ObserverList<PlayerObserver> _playobs =
        new ObserverList<PlayerObserver>(ObserverList.FAST_UNSAFE_NOTIFY);

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
    protected static AuditLogger _plog = createAuditLog("perf");

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
