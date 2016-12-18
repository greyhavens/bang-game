//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.Iterator;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.depot.ConnectionProvider;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.StaticConnectionProvider;

import com.samskivert.util.AuditLogger;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.admin.server.DatabaseConfigRegistry;
import com.threerings.cast.bundle.BundledComponentRepository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.presents.annotation.AuthInvoker;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsAuthInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsInvoker;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.net.PresentsConnectionManager;

import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.LocationManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.user.depot.AccountActionRepository;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberConfig;
import com.threerings.bang.avatar.server.BarberManager;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.bounty.data.OfficeConfig;
import com.threerings.bang.bounty.server.OfficeManager;
import com.threerings.bang.bounty.server.persist.BountyRepository;
import com.threerings.bang.chat.server.BangChatManager;
import com.threerings.bang.chat.server.BangChatProvider;
import com.threerings.bang.gang.data.HideoutConfig;
import com.threerings.bang.gang.server.GangManager;
import com.threerings.bang.gang.server.HideoutManager;
import com.threerings.bang.ranch.data.RanchConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.saloon.data.SaloonConfig;
import com.threerings.bang.saloon.server.SaloonManager;
import com.threerings.bang.station.data.StationConfig;
import com.threerings.bang.station.server.StationManager;
import com.threerings.bang.store.data.StoreConfig;
import com.threerings.bang.store.server.StoreManager;
import com.threerings.bang.tourney.server.BangTourniesManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TownObject;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Creates and manages all the services needed on the bang server.
 */
public class BangServer extends CrowdServer
{
    /** Configures dependencies needed by the Bang server. */
    public static class Module extends CrowdServer.CrowdModule {
        @Override protected void configure () {
            super.configure();

            // we need a legacy samskivert JDBC connection provider for our ye olde JORA and Simple
            // repositories; I'm not too keen to rewrite this decade+ old code... blah.
            com.samskivert.jdbc.ConnectionProvider legconprov =
                new com.samskivert.jdbc.StaticConnectionProvider(ServerConfig.getJDBCConfig());
            bind(com.samskivert.jdbc.ConnectionProvider.class).toInstance(legconprov);

            ConnectionProvider conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
            bind(ConnectionProvider.class).toInstance(conprov);
            // depot dependencies (we will initialize this persistence context later when the
            // server is ready to do database operations; not initializing it now ensures that no
            // one sneaks any database manipulations into the dependency resolution phase)
            PersistenceContext pctx = new PersistenceContext();
            bind(PersistenceContext.class).toInstance(pctx);
            bind(PeerManager.class).to(BangPeerManager.class);
            bind(ReportManager.class).to(BangReportManager.class);
            bind(ChatProvider.class).to(BangChatProvider.class);
            bind(Authenticator.class).to(ServerConfig.getAuthenticator());
            bind(BodyLocator.class).to(PlayerLocator.class);
            bind(ConfigRegistry.class).to(BangConfigRegistry.class);
            // bang dependencies
            ResourceManager rsrcmgr = new ResourceManager("rsrc");
            AccountActionRepository aarepo = new AccountActionRepository(pctx);
            AvatarLogic alogic;
            try {
                rsrcmgr.initBundles(null, "config/resource/manager.properties", null);
                alogic = new AvatarLogic(rsrcmgr, new BundledComponentRepository(
                    rsrcmgr, null, AvatarCodes.AVATAR_RSRC_SET));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            bind(ResourceManager.class).toInstance(rsrcmgr);
            bind(AccountActionRepository.class).toInstance(aarepo);
            bind(AvatarLogic.class).toInstance(alogic);
        }
        @Override protected void bindInvokers() {
            // replace the presents invoker with a custom version
            bind(Invoker.class).annotatedWith(MainInvoker.class).to(BangInvoker.class);
            bind(PresentsInvoker.class).to(BangInvoker.class);
            bind(Invoker.class).annotatedWith(AuthInvoker.class).to(PresentsAuthInvoker.class);
        }
    }

    /** The connection provider used to obtain access to our JDBC databases. */
    public static ConnectionProvider conprov;

    /** Used to provide database access to our Depot repositories. */
    public static PersistenceContext perCtx;

    /** A reference to the authenticator in use by the server. */
    public static BangAuthenticator author;

    /** Manages global player related bits. */
    public static PlayerManager playmgr;

    /** Manages gangs. */
    public static GangManager gangmgr;

    /** Manages tournaments. */
    public static BangTourniesManager tournmgr;

    /** Manages rating bits. */
    public static RatingManager ratingmgr;

    /** Keeps an eye on the Ranch, a good man to have around. */
    public static RanchManager ranchmgr;

    /** Manages the Saloon and match-making. */
    public static SaloonManager saloonmgr;

    /** Manages the General Store and item purchase. */
    public static StoreManager storemgr;

    /** Manages the Barber and avatar customization. */
    public static BarberManager barbermgr;

    /** Manages the Train Station and inter-town travel. */
    public static StationManager stationmgr;

    /** Manages the Hideout and Gangs. */
    public static HideoutManager hideoutmgr;

    /** Manages the Sheriff's Office and Bounties. */
    public static OfficeManager officemgr;

    /** Manages tracking and discouraging of misbehaving players. */
    public static NaughtyPlayerManager npmgr = new NaughtyPlayerManager();

    /** Contains information about the whole town. */
    public static TownObject townobj;

    // legacy static Presents services; try not to use these
    public static Invoker invoker;
    public static PresentsConnectionManager conmgr;
    public static ClientManager clmgr;
    public static PresentsDObjectMgr omgr;
    public static InvocationManager invmgr;

    // legacy static Crowd services; try not to use these
    public static PlayerLocator locator;
    public static PlaceRegistry plreg;
    public static LocationManager locman;

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

    /**
     * The main entry point for the Bang server.
     */
    public static void main (String[] args)
    {
        // // if we're on the dev server, up our long invoker warning to 3 seconds
        // if (ServerConfig.config.getValue("auto_restart", false)) {
        //     Invoker.setDefaultLongThreshold(3000L);
        // }

        Injector injector = Guice.createInjector(new Module());
        BangServer server = injector.getInstance(BangServer.class);
        try {
            server.init(injector);
            server.run();
            // annoyingly some background threads are hanging, so stick a fork in them for the time
            // being; when run() returns the dobj mgr and invoker thread will already have exited
            System.exit(0);
        } catch (Exception e) {
            log.warning("Server initialization failed.", e);
            System.exit(255);
        }
    }

    @Override // documentation inherited
    public void init (final Injector injector)
        throws Exception
    {
        // create out database connection provider this must be done before calling super.init()
        conprov = _conprov;
        perCtx = _perCtx;

        // make sure we have a valid payment type configured
        try {
            DeploymentConfig.getPaymentType();
        } catch (Exception e) {
            log.warning("deployment.properties payment_type invalid: " + e.getMessage());
            System.exit(255);
        }

        // set up some legacy static references
        invoker = _invoker;
        conmgr = _conmgr;
        clmgr = _clmgr;
        omgr = _omgr;
        invmgr = _invmgr;
        locator = _locator;
        plreg = _plreg;
        locman = _locman;

        // create and set up our configuration registry and admin service
        ConfigRegistry confreg = new DatabaseConfigRegistry(perCtx, invoker, ServerConfig.nodename);

        // initialize our depot repositories; running all of our schema and data migrations
        _perCtx.init("bangdb", _conprov, null);
        _perCtx.initializeRepositories(true);

        // create our various supporting managers
        playmgr = _playmgr;
        gangmgr = _gangmgr;
        tournmgr = injector.getInstance(BangTourniesManager.class);
        ratingmgr = injector.getInstance(RatingManager.class);

        // now initialize our runtime configuration
        RuntimeConfig.init(omgr, confreg);

        // do the base server initialization
        super.init(injector);

        // initialize our managers
        _boardmgr.init();
        _playmgr.init();
        _gangmgr.init();
        tournmgr.init();
        ratingmgr.init();
        _adminmgr.init();

        // start up our periodic server status reporting
        _repmgr.activatePeriodicReport(omgr);

        // create the town object and initialize the locator which will keep it up-to-date
        townobj = omgr.registerObject(new TownObject());
        _locator.init();

        // create our managers
        saloonmgr = (SaloonManager)plreg.createPlace(new SaloonConfig());
        storemgr = (StoreManager)plreg.createPlace(new StoreConfig());
        ranchmgr = (RanchManager)plreg.createPlace(new RanchConfig());
        barbermgr = (BarberManager)plreg.createPlace(new BarberConfig());
        stationmgr = (StationManager)plreg.createPlace(new StationConfig());
        hideoutmgr = (HideoutManager)plreg.createPlace(new HideoutConfig());
        officemgr = (OfficeManager)plreg.createPlace(new OfficeConfig());

        // if we have a shared secret, assume we're running in a cluster
        String node = System.getProperty("node");
        if (node != null && ServerConfig.sharedSecret != null) {
            log.info("Running in cluster mode as node '" + ServerConfig.nodename + "'.");
            _peermgr.init(ServerConfig.nodename, ServerConfig.sharedSecret,
                          ServerConfig.hostname, ServerConfig.publicHostname, getListenPorts()[0]);
        }

        // set up our authenticator
        author = (BangAuthenticator)_author;
        author.init();

        // configure the client manager to use the appropriate client class
        clmgr.setDefaultSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (AuthRequest areq) {
                return BangSession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return BangClientResolver.class;
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

        log.info("Bang server v" + DeploymentConfig.getVersion() + " initialized.");
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
    protected int[] getListenPorts ()
    {
        return DeploymentConfig.getServerPorts(ServerConfig.townId);
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
                _adminmgr.scheduleReboot(0, "codeUpdateAutoRestart");
            }
        }
    }

    /** Used to direct our server reports to an audit log file. */
    protected static class BangReportManager extends ReportManager
    {
        @Override protected void logReport (String report) {
            _stlog.log(report);
        }
    }

    @Singleton
    protected static class BangConfigRegistry extends DatabaseConfigRegistry
    {
        @Inject public BangConfigRegistry (PersistenceContext perCtx,
                                           @MainInvoker Invoker invoker) {
            super(perCtx, invoker, ServerConfig.nodename);
        }
    }

    protected long _codeModified;

    @Inject protected ConnectionProvider _conprov;
    @Inject protected PersistenceContext _perCtx;
    @Inject protected Authenticator _author;
    @Inject protected ParlorManager _parmgr;
    @Inject protected BodyManager _bodymgr;
    @Inject protected ResourceManager _rsrcmgr;

    @Inject protected PlayerLocator _locator;
    @Inject protected BangAdminManager _adminmgr;
    @Inject protected BoardManager _boardmgr;
    @Inject protected GangManager _gangmgr;
    @Inject protected PlayerManager _playmgr;
    @Inject protected BangPeerManager _peermgr;
    @Inject protected BangChatManager _chatmgr;
    @Inject protected BangReportManager _repmgr;

    // need to inject this guy here as he's otherwise not referenced until the office manager is
    // created which is too late in our initialization for safe repository creation
    @Inject protected BountyRepository _bountyrepo;

    // reference needed to bring these managers into existence
    @Inject protected AccountActionManager _actionmgr;

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
    protected static AuditLogger _plog = createAuditLog("perf");

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;
}
