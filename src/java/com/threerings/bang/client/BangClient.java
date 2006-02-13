//
// $Id: BangClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;

import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.getdown.util.LaunchUtil;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.effect.WindowSlider;

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

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
public class BangClient extends BasicClient
    implements SessionObserver, PlayerReceiver, BangCodes
{
    /**
     * Checks the supplied logon failure message for client version related
     * failure and decodes the necessary business to instruct Getdown to update
     * the client on the next invocation.
     */
    public static boolean checkForUpgrade (
        final BangContext ctx, String message)
    {
        if (!message.startsWith(BangAuthCodes.VERSION_MISMATCH)) {
            return false;
        }

        int didx = message.indexOf("|~");
        if (didx == -1) {
            log.warning("Bogus version mismatch: '" + message + "'.");
            return false;
        }

        // create the file that instructs Getdown to upgrade
        String vers = message.substring(didx+2);
        File vfile = new File(localDataDir("version.txt"));
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(vfile));
            ps.println(vers);
            ps.close();
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Error creating '" + vfile + "'", ioe);
            return false;
        }

        // if we can relaunch Getdown automatically, do so
        File pro = new File(localDataDir("getdown-pro.jar"));
        if (!LaunchUtil.mustMonitorChildren() && pro.exists()) {
            String[] args = new String[] {
                LaunchUtil.getJVMPath(),
                "-jar",
                pro.toString(),
                localDataDir("")
            };
            log.info("Running " + StringUtil.join(args, "\n  "));
            try {
                Runtime.getRuntime().exec(args, null);
            } catch (IOException ioe) {
                log.log(Level.WARNING, "Failed to run getdown", ioe);
                return false;
            }

            // now stick a fork in ourselves in about 3 seconds
            new Interval(ctx.getClient().getRunQueue()) {
                public void expired () {
                    log.info("Exiting due to out-of-dateness.");
                    ctx.getApp().stop();
                }
            }.schedule(3000L);

            return true;
        }

        return false;
    }

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

        // register as receiver for player notifications
        _client.getInvocationDirector().registerReceiver(
            new PlayerDecoder(this));

        // create and display the logon view; which we do by hand instead of
        // using setMainView() because we don't want to start the resource
        // resolution until we're faded in
        final LogonView lview = new LogonView(_ctx);
        _mview = lview;
        _ctx.getRootNode().addWindow(_mview);
        _mview.pack();
        _mview.center();
        FadeInOutEffect fade =
            new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, true) {
            protected void fadeComplete () {
                super.fadeComplete();
                // now start unpacking our resources
                initResources(lview);
            }
        };
        _ctx.getInterface().attachChild(fade);

        // create the pardner chat view, which will listen for tells from
        // pardners and pop up when possible
        _pcview = new PardnerChatView(_ctx);
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
    public boolean checkShowIntro ()
    {
        PlayerObject user = _ctx.getUserObject();

        // if this player does not have a name, it's their first time, so pop
        // up the create avatar view
        if (user.handle == null) {
            displayPopup(new CreateAvatarView(_ctx), true, 800);
            return true;
        }

        // if they have no big shots then they need the intro for those
        if (!user.hasBigShot()) {
            displayPopup(new FirstBigShotView(_ctx), true, 600);
            return true;
        }

        // if there are any pending pardner invitations, show those
        if (_invites.size() > 0) {
            displayPardnerInvite(_invites.remove(0));
        }

        return false;
    }

    public void showTownView ()
    {
        if (!(_mview instanceof TownView)) {
            setMainView(new TownView(_ctx));
        }
    }

    /**
     * Determines whether we can display a pop-up at the moment.
     */
    public boolean canDisplayPopup (MainView.Type type)
    {
        if (_mview instanceof MainView) {
            return ((MainView)_mview).allowsPopup(type);
        } else {
            return false;
        }
    }

    /**
     * Like {@link #displayPopup(BWindow,boolean)} but allows the specification
     * of a desired width for the popup.
     */
    public void displayPopup (BWindow popup, boolean animate, int twidth)
    {
        _ctx.getRootNode().addWindow(popup);
        _popups.add(popup);

        if (animate) {
            popup.pack(twidth, -1);
            _ctx.getInterface().attachChild(
                new WindowSlider(popup, WindowSlider.FROM_TOP, 0.25f));
        }
    }

    /**
     * Displays a popup window that will automatically be cleared if we leave
     * the current "place". This should be used for any overlay view shown atop
     * the normal place views.
     *
     * @param animate if true the popup will be {@link BWindow#pack}ed and slid
     * onto the screen from some direction. Otherwise the caller is responsible
     * for {@link BWindow#pack}ing and {@link BWindow#center}ing the window.
     */
    public void displayPopup (BWindow popup, boolean animate)
    {
        displayPopup(popup, animate, -1);
    }

    /**
     * Dismisses all popups.
     */
    public void clearPopups (boolean animate)
    {
        while (_popups.size() > 0) {
            clearPopup(_popups.get(0), animate);
        }
    }

    /**
     * Dismisses a popup displayed with {@link #displayPopup}.
     */
    public void clearPopup (final BWindow popup, boolean animate)
    {
        if (!_popups.remove(popup)) {
            return;
        }
        if (animate) {
            _ctx.getInterface().attachChild(
                new WindowSlider(popup, WindowSlider.TO_RIGHT, 0.25f) {
                    protected void slideComplete () {
                        super.slideComplete();
                        _ctx.getRootNode().removeWindow(popup);
                    }
                });
        } else {
            _ctx.getRootNode().removeWindow(popup);
        }
    }

    /**
     * Returns a reference to the {@link PardnerChatView}.
     */
    public PardnerChatView getPardnerChatView ()
    {
        return _pcview;
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogon (Client client)
    {
        // get a reference to the player service
        _psvc = (PlayerService)_client.requireService(PlayerService.class);

        // we potentially jump right into a game when developing
        BangConfig config = null;
        if ("tutorial".equals(System.getProperty("test"))) {
            config = new BangConfig();
            TutorialConfig tconfig =
                TutorialUtil.loadTutorial(_rsrcmgr, "test");
            config.rated = false;
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
            config.teamSize = 3;
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
            return;
        }

        // check for a "go" parameter
        String where = System.getProperty("go");
        BangBootstrapData bbd = (BangBootstrapData)
            _ctx.getClient().getBootstrapData();
        if ("ranch".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.ranchOid);
        } else if ("bank".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.bankOid);
        } else if ("store".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.storeOid);
        } else if ("saloon".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);
        } else if ("barber".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.barberOid);
        }

        // show the town view to start, this will call checkShowIntro() once
        // the town view has "presented" the first town
        showTownView();
    }

    // documentation inherited from interface SessionObserver
    public void clientObjectDidChange (Client client)
    {
        // nada
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogoff (Client client)
    {
        // TODO: go back to the logon page?
        _ctx.getApp().stop();
    }

    // documentation inherited from interface PlayerReceiver
    public void receivedPardnerInvite (final Name handle)
    {
        if (canDisplayPopup(MainView.Type.PARDNER_INVITE)) {
            displayPardnerInvite(handle);
        } else {
            // stick it on a list and we'll show the invite next the we're in
            // the town view
            _invites.add(handle);
        }
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

    protected void displayPardnerInvite (final Name handle)
    {
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                _psvc.respondToPardnerInvite(
                    _client, handle, button == 0,
                    new ReportingListener(
                        _ctx, BANG_MSGS, "e.response_failed"));
                checkShowIntro();
            }
        };
        String title = MessageBundle.tcompose("m.pardner_invite", handle);
        OptionDialog.showConfirmDialog(
            _ctx, BANG_MSGS, title, "m.pardner_accept", "m.pardner_reject", rr);
    }

    protected void setMainView (final BWindow view)
    {
        // if we have an existing main view, fade that out
        if (_mview != null) {
            FadeInOutEffect fade =
                new FadeInOutEffect(ColorRGBA.black, 0f, 1f, 0.5f, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    _ctx.getRootNode().removeWindow(_mview);
                    fadeInMainView(view);
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

        // don't fade in the game or town views, they will handle that
        // themselves when they're ready to roll
        if (view instanceof BangView || view instanceof TownView) {
            return;
        }
        _ctx.getInterface().attachChild(
            new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, true));
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
            // clear any lingering popups
            clearPopups(false);

            // wire a status view to this place view (show by pressing esc);
            // the window must be modal prior to adding it to the hierarchy to
            // ensure that it is a default event target (and hears the escape
            // key pressed event)
            BWindow pview = (BWindow)view;
            pview.setModal(true);
            if (!(pview instanceof BangView)) {
                new StatusView(_ctx).bind(pview);
            }

            // shop views are hard-coded to 1024x768
            if (pview instanceof ShopView) {
                pview.setSize(1024, 768);
                pview.center();
            } else {
                // size the view to fill the display
                pview.setBounds(0, 0, _ctx.getDisplay().getWidth(),
                                _ctx.getDisplay().getHeight());
            }

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
    protected PlayerService _psvc;

    protected BWindow _mview;
    protected ArrayList<BWindow> _popups = new ArrayList<BWindow>();
    protected PardnerChatView _pcview;

    protected ArrayList<Name> _invites = new ArrayList<Name>();
}
