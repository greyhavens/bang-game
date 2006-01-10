//
// $Id: BangClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;
import com.threerings.util.Name;

import com.threerings.jme.effect.FadeInOutEffect;

import com.threerings.cast.CharacterManager;
import com.threerings.cast.bundle.BundledComponentRepository;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.SessionObserver;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.ranch.client.FirstBigShotView;

import com.threerings.bang.game.client.BangView;
import com.threerings.bang.game.client.effect.ParticleFactory;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
public class BangClient extends BasicClient
    implements SessionObserver
{
    /**
     * Initializes a new client and provides it with a frame in which to
     * display everything.
     */
    public void init (BangApp app)
    {
        _ctx = new BangContextImpl();
        initClient(_ctx, app, app);

        // listen for logon
        _client.addClientObserver(this);

        // create and display the logon view; which we do by hand instead of
        // using setMainView() because we don't want to start the resource
        // resolution until we're faded in
        final LogonView lview = new LogonView(_ctx);
        _mview = lview;
        _ctx.getRootNode().addWindow(_mview);
        _mview.pack();
        _mview.center();
        FadeInOutEffect fade =
            new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, false) {
            protected void fadeComplete () {
                _ctx.getInterface().detachChild(this);
                // now start unpacking our resources
                initResources(lview);
            }
        };
        _ctx.getInterface().attachChild(fade);
    }

    /**
     * Returns a reference to the context in effect for this client. This
     * reference is valid for the lifetime of the application.
     */
    public BangContext getContext ()
    {
        return _ctx;
    }

    /**
     * Potentially shows the next phase of the client introduction and
     * tutorial. This is called after first logging on and then at the
     * completion of each phase of the intro and tutorial.
     */
    public void checkShowIntro ()
    {
        PlayerObject user = _ctx.getUserObject();

        // if this player does not have a name, it's their first time, so pop
        // up the create avatar view
        if (user.handle == null) {
            CreateAvatarView cav = new CreateAvatarView(_ctx);
            _ctx.getRootNode().addWindow(cav);
            cav.pack();
            cav.center();
            return;
        }

        // if they have no big shots then they need the intro for those
        if (!user.hasBigShot()) {
            FirstBigShotView fbsv = new FirstBigShotView(_ctx);
            _ctx.getRootNode().addWindow(fbsv);
            fbsv.pack(600, -1);
            fbsv.center();
            return;
        }

        // otherwise, display the town view
        _tview = new TownView(_ctx);
        showTownView();
    }

    public void showTownView ()
    {
        setMainView(_tview);
    }

    /**
     * Displays a popup window that will automatically be cleared if we leave
     * the current "place". This should be used for any overlay view shown atop
     * the normal place views.
     */
    public void displayPopup (BWindow popup)
    {
        if (_popup != null) {
            log.warning("Overriding popup [old=" + _popup + ", new=" + popup + "].");
        }
        _ctx.getRootNode().addWindow(_popup = popup);
    }

    /**
     * Dismisses a popup displayed with {@link #displayPopup}.
     */
    public void clearPopup ()
    {
        if (_popup != null) {
            _ctx.getRootNode().removeWindow(_popup);
            _popup = null;
        }
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogon (Client client)
    {
        // we potentially jump right into a game when developing
        BangConfig config = null;
        if ("tutorial".equals(System.getProperty("test"))) {
            config = new BangConfig();
            TutorialConfig tconfig =
                TutorialUtil.loadTutorial(_rsrcmgr, "controls");
            config.players = new Name[] {
                _ctx.getUserObject().getVisibleName(),
                new Name("Larry") /*, new Name("Moe")*/ };
            config.ais = new GameAI[] {
                null, new GameAI(1, 50) /*, new GameAI(0, 50)*/ };
            config.scenarios = new String[] { tconfig.ident };
            config.tutorial = true;
            config.board = tconfig.board;

        } else if (System.getProperty("test") != null) {
            config = new BangConfig();
            config.players = new Name[] {
                _ctx.getUserObject().getVisibleName(),
                new Name("Larry") /*, new Name("Moe")*/ };
            config.ais = new GameAI[] {
                null, new GameAI(1, 50) /*, new GameAI(0, 50)*/ };
            config.scenarios = new String[] { "cj" };
            config.board = System.getProperty("board");
        }

        if (config != null) {
            ConfirmListener cl = new ConfirmListener() {
                public void requestProcessed () {
                }
                public void requestFailed (String reason) {
                    log.warning("Failed to create game: " + reason);
                }
            };
            _ctx.getParlorDirector().startSolitaire(config, cl);
        }

        // start up the introduction process, if appropriate, or if no intro is
        // needed this will show the town view
        checkShowIntro();
    }

    // documentation inherited from interface SessionObserver
    public void clientObjectDidChange (Client client)
    {
        // nada
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogoff (Client client)
    {
        System.exit(0);
    }

    @Override // documentation inherited
    protected void createContextServices (RunQueue rqueue)
    {
        super.createContextServices(rqueue);

        // create our custom directors
        _chatdir = new BangChatDirector(_ctx);

        // warm up the particle factory
        ParticleFactory.warmup(_ctx);
    }

    @Override // documentation inherited
    protected void postResourcesInit ()
    {
        super.postResourcesInit();

        try {
            _charmgr = new CharacterManager(
                _imgmgr, new BundledComponentRepository(
                    _rsrcmgr, _imgmgr, AvatarCodes.AVATAR_RSRC_SET));
            _alogic = new AvatarLogic(
                _rsrcmgr, _charmgr.getComponentRepository());

        } catch (IOException ioe) {
            // TODO: report to the client
            log.log(Level.WARNING, "Initialization failed.", ioe);
        }
    }

    protected void setMainView (final BWindow view)
    {
        // if we have an existing main view, fade that out
        if (_mview != null) {
            FadeInOutEffect fade =
                new FadeInOutEffect(ColorRGBA.black, 0f, 1f, 0.25f, false) {
                protected void fadeComplete () {
                    _ctx.getRootNode().removeWindow(_mview);
                    fadeInMainView(view);
                    _ctx.getInterface().detachChild(this);
                }
            };
            _ctx.getInterface().attachChild(fade);
        } else {
            fadeInMainView(view);
        }
    }

    protected void fadeInMainView (BWindow view)
    {
        _mview = view;
        _ctx.getRootNode().addWindow(_mview);
        FadeInOutEffect fade =
            new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, false) {
            protected void fadeComplete () {
                _ctx.getInterface().detachChild(this);
            }
        };
        _ctx.getInterface().attachChild(fade);
    }

    /** The context implementation. This provides access to all of the
     * objects and services that are needed by the operating client. */
    protected class BangContextImpl extends BasicContextImpl
        implements BangContext
    {
        /** Apparently the default constructor has default access, rather
         * than protected access, even though this class is declared to be
         * protected. Why, I don't know, but we need to be able to extend
         * this class elsewhere, so we need this. */
        protected BangContextImpl () {
        }

        public Config getConfig () {
            return _config;
        }

        public ChatDirector getChatDirector () {
            return _chatdir;
        }

        public void setPlaceView (PlaceView view) {
            // clear any lingering popup
            clearPopup();

            // wire a status view to this place view (show by pressing esc);
            // the window must be modal prior to adding it to the hierarchy to
            // ensure that it is a default event target (and hears the escape
            // key pressed event)
            BWindow pview = (BWindow)view;
            pview.setModal(true);
            if (!(pview instanceof BangView)) {
                new StatusView(_ctx).bind(pview);
            }

            // size the view to fill the display
            pview.setBounds(0, 0, _ctx.getDisplay().getWidth(),
                            _ctx.getDisplay().getHeight());

            // configure the main view; this will fade the previous view out
            // and fade the new view in
            setMainView(pview);
        }

        public void clearPlaceView (PlaceView view) {
            // while testing, reload the stylesheet every time we switch the
            // place view
            BangUI.reloadStylesheet();
        }

        public BangClient getBangClient() {
            return BangClient.this;
        }

        public PlayerObject getUserObject () {
            return (PlayerObject)getClient().getClientObject();
        }

        public CharacterManager getCharacterManager () {
            return _charmgr;
        }

        public AvatarLogic getAvatarLogic () {
            return _alogic;
        }
    }

    protected BangContextImpl _ctx;
    protected Config _config = new Config("bang");

    protected BangChatDirector _chatdir;
    protected CharacterManager _charmgr;
    protected AvatarLogic _alogic;

    protected BWindow _mview, _popup;
    protected TownView _tview;
}
