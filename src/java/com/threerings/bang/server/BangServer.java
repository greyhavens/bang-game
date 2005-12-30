//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.cast.ComponentRepository;
import com.threerings.cast.bundle.BundledComponentRepository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.presents.server.Authenticator;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.server.ParlorManager;

import com.threerings.user.AccountActionRepository;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberConfig;
import com.threerings.bang.avatar.server.BarberManager;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.bank.data.BankConfig;
import com.threerings.bang.bank.server.BankManager;
import com.threerings.bang.lobby.data.LobbyConfig;
import com.threerings.bang.lobby.server.LobbyManager;
import com.threerings.bang.ranch.data.RanchConfig;
import com.threerings.bang.ranch.server.RanchManager;
import com.threerings.bang.store.data.StoreConfig;
import com.threerings.bang.store.server.StoreManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.RatingRepository;
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

    /** A resource manager with which we can load resources in the same manner
     * that the client does (for resources that are used on both the server and
     * client). */
    public static ResourceManager rsrcmgr;

    /** Provides information on our character components. */
    public static ComponentRepository comprepo;

    /** Handles the heavy lifting relating to avatar looks and articles. */
    public static AvatarLogic alogic;

    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    /** Provides visibility into global OOO account actions. */
    public static AccountActionRepository actionrepo;

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
    public static LobbyManager saloonmgr;

    /** Manages the General Store and item purchase. */
    public static StoreManager storemgr;

    /** Manages the Bank and the coin exchange. */
    public static BankManager bankmgr;

    /** Manages the Barber and avatar customization. */
    public static BarberManager barbermgr;

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

        // create our resource manager and other resource bits
        rsrcmgr = new ResourceManager("rsrc");
        rsrcmgr.initBundles(null, "config/resource/manager.properties", null);

        // create our avatar related bits
        comprepo = new BundledComponentRepository(
            rsrcmgr, null, AvatarCodes.AVATAR_RSRC_SET);
        alogic = new AvatarLogic(rsrcmgr, comprepo);
            
        // create our database connection provider and repositories
        conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
        actionrepo = new AccountActionRepository(conprov);
        playrepo = new PlayerRepository(conprov);
        itemrepo = new ItemRepository(conprov);
        statrepo = new StatRepository(conprov);
        ratingrepo = new RatingRepository(conprov);
        lookrepo = new LookRepository(conprov);
        playmgr = new PlayerManager();
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
        playmgr.init();
        coinexmgr.init();

        // create our managers
        PlaceRegistry.CreationObserver crobs =
            new PlaceRegistry.CreationObserver() {
            public void placeCreated (PlaceObject place, PlaceManager pmgr) {
                if (pmgr instanceof LobbyManager) {
                    saloonmgr = (LobbyManager)pmgr;
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
        plreg.createPlace(new LobbyConfig(), crobs);
        plreg.createPlace(new StoreConfig(), crobs);
        plreg.createPlace(new BankConfig(), crobs);
        plreg.createPlace(new RanchConfig(), crobs);
        plreg.createPlace(new BarberConfig(), crobs);

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
     * Returns the player object for the specified user if they are online
     * currently, null otherwise. This should only be called from the dobjmgr
     * thread.
     */
    public static PlayerObject lookupPlayer (Handle handle)
    {
        return _players.get(handle);
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
    }

    /**
     * Called when a player ends their session to clear their handle to player
     * object mapping.
     */
    public static void clearPlayer (PlayerObject player)
    {
        _players.remove(player.handle);
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

    protected static HashMap<Handle,PlayerObject> _players =
        new HashMap<Handle,PlayerObject>();

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server.log");
    protected static AuditLogger _ilog = createAuditLog("item.log");
}
