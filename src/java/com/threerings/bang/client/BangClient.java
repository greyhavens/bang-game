//
// $Id: BangClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import com.jme.input.KeyInput;
import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;

import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.ResultListener;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.getdown.util.LaunchUtil;
import com.threerings.hemiptera.data.Report;
import com.threerings.hemiptera.util.SendReportUtil;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.effect.WindowSlider;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.SessionObserver;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.MuteDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.ranch.client.FirstBigShotView;

import com.threerings.bang.game.client.BangView;
import com.threerings.bang.game.client.effect.ParticleFactory;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.util.BoardCache;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
public class BangClient extends BasicClient
    implements SessionObserver, PlayerReceiver, BangCodes
{
    /** A marker interface for non-clearable popups. */
    public static interface NonClearablePopup
    {
    }

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
     * Asynchronously submits a bug report with the specified description.
     */
    public static void submitBugReport (final BangContext ctx, String descrip,
                                        final boolean exitAfterSubmit)
    {
        // fill in a bug report
        PlayerObject user = ctx.getUserObject();
        Report report = new Report();
        report.submitter = user.username.toString();
        if (descrip.length() > 255) {
            report.summary = StringUtil.truncate(descrip, 255);
            report.setAttribute("Description", descrip);
        } else {
            report.summary = descrip;
        }
        report.setAttribute("Handle", user.handle.toString());

        // and send it along with our debug logs
        URL submitURL = DeploymentConfig.getBugSubmitURL();
        if (submitURL == null) {
            log.warning("Unable to submit bug report, no submit URL.");
            return;
        }

        String[] files = { BangClient.localDataDir("bang.log") };
        ResultListener rl = new ResultListener() {
            public void requestCompleted (Object result) {
                ctx.getChatDirector().displayFeedback(
                    BANG_MSGS, "m.bug_submit_completed");
                if (exitAfterSubmit) {
                    ctx.getApp().stop();
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Bug submission failed.", cause);
                ctx.getChatDirector().displayFeedback(
                    BANG_MSGS, "m.bug_submit_failed");
            }
        };
        SendReportUtil.submitReportAsync(
            submitURL, report, files, ctx.getClient().getRunQueue(), rl);
        ctx.getChatDirector().displayFeedback(
            BANG_MSGS, "m.bug_submit_started");
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

        // create the pardner chat view, which will listen for tells from
        // pardners and pop up when possible
        _pcview = new PardnerChatView(_ctx);

        // create the system chat view, which will display system chat messages
        // outside of games
        _scview = new SystemChatView(_ctx);
        
        // register our global key bindings
        _functionPopup = new FKeyPopups(_ctx);

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

        // if they haven't done (or declined) the tutorials yet, show them
        if (BangPrefs.shouldShowTutorials(_ctx.getUserObject())) {
            displayPopup(new PickTutorialView(
                             _ctx, PickTutorialView.Mode.FIRST_TIME), true);
            return true;
        }

        // if there are any pending pardner invitations, show those
        if (_invites.size() > 0) {
            displayPardnerInvite(_invites.remove(0));
            return true;
        }

        // if the main view is the town view, activate it because we're done
        // fooling around
        if (_mview instanceof TownView) {
            ((TownView)_mview).setActive(true);
        }

        return false;
    }

    /**
     * Displays the town view, first fading out any existing main view.
     */
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
        // only allow status view after we've created our avatar
        if (type == MainView.Type.STATUS &&
            _ctx.getUserObject().handle == null) {
            return false;
        }

        // don't allow FKEY popups if we have other popups showing
        if (type == MainView.Type.FKEY && _popups.size() > 0) {
            return false;
        }

        // otherwise ask the view what they think about it
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

        // create the mute director here because the mute list is specific to
        // the account
        final String mkey = client.getCredentials().getUsername() + ".muted";
        _mutedir = new MuteDirector(_ctx, createHandles(
            _config.getValue(mkey, new String[0])));
        _mutedir.setChatDirector(_chatdir);
        _mutedir.addMuteObserver(new MuteDirector.MuteObserver() {
            public void muteChanged (Name playerName, boolean nowMuted) {
                _config.setValue(mkey, StringUtil.join(_mutedir.getMuted()));
            }
        });
        
        // register our status view key bindings
        StatusView.bindKeys(_ctx);

        // we potentially jump right into a game when developing
        BangConfig config = null;
        if ("tutorial".equals(System.getProperty("test"))) {
            PlayerService psvc = (PlayerService)
                _ctx.getClient().requireService(PlayerService.class);
            psvc.playTutorial(_ctx.getClient(), "test", new ReportingListener(
                                  _ctx, BANG_MSGS, "m.start_tut_failed"));
            return;

        } else if (System.getProperty("test") != null) {
            config = new BangConfig();
            config.players = new Name[] {
                _ctx.getUserObject().getVisibleName(),
                new Name("Larry"), new Name("Moe"), new Name("Curly") };
            config.ais = new GameAI[] {
                null, new GameAI(1, 50), new GameAI(0, 50), new GameAI(0, 50) };
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
            return;
        } else if ("bank".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.bankOid);
            return;
        } else if ("store".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.storeOid);
            return;
        } else if ("saloon".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.saloonOid);
            return;
        } else if ("barber".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.barberOid);
            return;
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
        // clear our status view key bindings
        StatusView.clearKeys(_ctx);

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

    protected Name[] createHandles (String[] strings)
    {
        Name[] handles = new Name[strings.length];
        for (int ii = 0; ii < strings.length; ii++) {
            handles[ii] = new Handle(strings[ii]);
        }
        return handles;
    }
    
    @Override // documentation inherited
    protected void createContextServices (RunQueue rqueue)
    {
        super.createContextServices(rqueue);

        // create our custom directors
        _chatdir = new BangChatDirector(_ctx);
        _bcache = new BoardCache();
        
        // warm up the particle factory
        ParticleFactory.warmup(_ctx);
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

        // re-wire up our options view whenever the main view changes as the
        // BangView overrides the escape mapping during the game
        if (!(view instanceof BangView)) {
            _ctx.getKeyManager().registerCommand(
                KeyInput.KEY_ESCAPE, _clearPopup);
        }

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

        public MuteDirector getMuteDirector () {
            return _mutedir;
        }
        
        public BoardCache getBoardCache () {
            return _bcache;
        }
        
        public void setPlaceView (PlaceView view) {
            // clear any lingering popups
            clearPopups(false);

            // shop views are hard-coded to 1024x768
            BWindow pview = (BWindow)view;
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
    }

    protected GlobalKeyManager.Command _clearPopup =
        new GlobalKeyManager.Command() {
        public void invoke (int keyCode, int modifiers) {
            if (_popups.size() > 0) {
                BWindow popup = _popups.get(_popups.size()-1);
                if (!(popup instanceof NonClearablePopup)) {
                    clearPopup(popup, true);
                }
            } else {
                displayPopup(new OptionsView(_ctx, _mview), true);
            }
        }
    };
    protected FKeyPopups _functionPopup;

    protected BangContextImpl _ctx;
    protected Config _config = new Config("bang");

    protected BangChatDirector _chatdir;
    protected BoardCache _bcache;
    protected MuteDirector _mutedir;
    protected PlayerService _psvc;

    protected BWindow _mview;
    protected ArrayList<BWindow> _popups = new ArrayList<BWindow>();
    protected PardnerChatView _pcview;
    protected SystemChatView _scview;

    protected ArrayList<Name> _invites = new ArrayList<Name>();
}
