//
// $Id$

package com.threerings.bang.client;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.jme.bui.BLookAndFeel;
import com.jme.bui.BRootNode;
import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.parlor.client.ParlorDirector;

import com.threerings.jme.JmeApp;
import com.threerings.jme.tile.FringeConfiguration;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains basic client initialization and configuration that is shared
 * between the game and editor clients.
 */
public class BasicClient
{
    /**
     * Given a subdirectory name (that should correspond to the calling
     * service), returns a file path that can be used to store local data.
     */
    public static String localDataDir (String subdir)
    {
        String appdir = System.getProperty("appdir");
        if (StringUtil.blank(appdir)) {
            appdir = ".bang";
            String home = System.getProperty("user.home");
            if (!StringUtil.blank(home)) {
                appdir = home + File.separator + appdir;
            }
        }
        return appdir + File.separator + subdir;
    }

    /**
     * Initializes various standard client services.
     */
    protected void initClient (BangContext ctx, JmeApp app, RunQueue rqueue)
    {
        _app = app;
        _ctx = ctx;

        // create the directors/managers/etc. provided by the context
        createContextServices(rqueue);

        // create a bunch of standard rendering stuff
        RenderUtil.init(_ctx);
    }

    /**
     * Creates and initializes the various services that are provided by
     * the context. Derived classes that provide an extended context
     * should override this method and create their own extended
     * services. They should be sure to call
     * <code>super.createContextServices</code>.
     */
    protected void createContextServices (RunQueue rqueue)
    {
        // create the handles on our various services
        _client = new Client(null, rqueue);

        // these manage local client resources
        _rsrcmgr = new ResourceManager("rsrc");
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _lnf = new BangLookAndFeel();
        _mcache = new ModelCache(_ctx);

        // create our sound manager
        _soundmgr = SoundManager.createSoundManager(rqueue);

        // load up our fringe configuration
        try {
            _fringeconf = (FringeConfiguration)
                CompiledConfig.loadConfig(
                    _rsrcmgr.getResource("tiles/fringe/config.dat"));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load fringe config.", ioe);
        }

        // these manage "online" state
        _locdir = new LocationDirector(_ctx);
        _occdir = new OccupantDirector(_ctx);
        _pardir = new ParlorDirector(_ctx);
        _chatdir = new ChatDirector(_ctx, _msgmgr, null);
    }

    /**
     * The basic context implementation. This provides access to objects
     * and services that are needed by the operating client.
     */
    protected abstract class BasicContextImpl extends BangContext
    {
        /** Apparently the default constructor has default access, rather
         * than protected access, even though this class is declared to be
         * protected. Why, I don't know, but we need to be able to extend
         * this class elsewhere, so we need this. */
        protected BasicContextImpl () {
        }

        public Client getClient () {
            return _client;
        }

        public DObjectManager getDObjectManager () {
            return _client.getDObjectManager();
        }

        public LocationDirector getLocationDirector () {
            return _locdir;
        }

        public OccupantDirector getOccupantDirector () {
            return _occdir;
        }

        public ChatDirector getChatDirector () {
            return _chatdir;
        }

        public ParlorDirector getParlorDirector () {
            return _pardir;
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public JmeApp getApp () {
            return _app;
        }

        public SoundManager getSoundManager () {
            return _soundmgr;
        }

        public ModelCache getModelCache () {
            return _mcache;
        }

        public FringeConfiguration getFringeConfig () {
            return _fringeconf;
        }

        public DisplaySystem getDisplay () {
            return _app.getContext().getDisplay();
        }

        public Renderer getRenderer () {
            return _app.getContext().getRenderer();
        }

        public Camera getCamera () {
            return _app.getContext().getCamera();
        }

        public Node getGeometry () {
            return _app.getContext().getGeometry();
        }

        public Node getInterface () {
            return _app.getContext().getInterface();
        }

        public InputHandler getInputHandler () {
            return _app.getContext().getInputHandler();
        }

        public BRootNode getRootNode () {
            return _app.getContext().getRootNode();
        }

        public BLookAndFeel getLookAndFeel () {
            return _lnf;
        }
    }

    protected JmeApp _app;
    protected BangContext _ctx;

    protected MessageManager _msgmgr;
    protected ResourceManager _rsrcmgr;
    protected SoundManager _soundmgr;
    protected BLookAndFeel _lnf;
    protected ModelCache _mcache;
    protected FringeConfiguration _fringeconf;

    protected Client _client;
    protected LocationDirector _locdir;
    protected OccupantDirector _occdir;
    protected ChatDirector _chatdir;
    protected ParlorDirector _pardir;

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
