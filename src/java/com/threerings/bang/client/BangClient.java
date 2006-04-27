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

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.glu.GLU;

import com.jme.input.KeyInput;
import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;

import com.jmex.sound.openAL.objects.MusicStream;

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
import com.threerings.presents.client.SessionObserver;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.MuteDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.ranch.client.FirstBigShotView;

import com.threerings.bang.game.client.BangView;
import com.threerings.bang.game.client.effect.ParticleFactory;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.util.BoardCache;
import com.threerings.bang.client.util.PerfMonitor;
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
        report.version = String.valueOf(DeploymentConfig.getVersion());
        report.setAttribute("Handle", "" + user.handle);
        report.setAttribute("Driver", Display.getAdapter());
        report.setAttribute("GL Display Mode", "" + Display.getDisplayMode());
        report.setAttribute("GL Version", GL11.glGetString(GL11.GL_VERSION));
        report.setAttribute("GL Vendor", GL11.glGetString(GL11.GL_VENDOR));
        report.setAttribute("GL Renderer", GL11.glGetString(GL11.GL_RENDERER));
        report.setAttribute("GL Extensions",
                            GL11.glGetString(GL11.GL_EXTENSIONS));
        report.setAttribute("GLU Extensions",
                            GLU.gluGetString(GLU.GLU_EXTENSIONS));

        // and send it along with our debug logs
        URL submitURL = DeploymentConfig.getBugSubmitURL();
        if (submitURL == null) {
            log.warning("Unable to submit bug report, no submit URL.");
            return;
        }

        log.info("Submitting bug report '" + descrip + "'.");
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

        // upgrade getdown if appropriate
        File newgd = new File(localDataDir("code/getdown-pro-new.jar"));
        File curgd = new File(localDataDir("getdown-pro.jar"));
        File oldgd = new File(localDataDir("getdown-pro-old.jar"));
        LaunchUtil.upgradeGetdown(oldgd, curgd, newgd);

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
     * Returns the player status view; creating the view if necessary. We purge
     * the view from memory every time the main view is switched, but preserve
     * it while in one place.
     */
    public StatusView getStatusView ()
    {
        if (_status == null) {
            _status = new StatusView(_ctx);
        }
        return _status;
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
            BangUI.play(BangUI.FeedbackSound.WINDOW_OPEN);
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
        BWindow[] popups = _popups.toArray(new BWindow[_popups.size()]);
        for (int ii = 0; ii < popups.length; ii++) {
            // don't auto clear certain popups
            if (popups[ii].getLayer() < BangCodes.NEVER_CLEAR_LAYER) {
                clearPopup(popups[ii], animate);
            }
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
            BangUI.play(BangUI.FeedbackSound.WINDOW_DISMISS);
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

    /**
     * Queues up the music track with the specified path.
     *
     * TODO: fade between the two tracks? or quickly fade out the old track and
     * fade in the new.
     *
     * @param musicPath the path to an OGG resource containing the music.
     */
    public void queueMusic (String musicPath, boolean loop)
    {
        // if we're already playing this track, keep it running
        if (musicPath.equals(_playingMusic)) {
            return;
        }

        // set the volume based on the user's volume preferences
        float volume = BangPrefs.getMusicVolume() / 100f;
        if (volume == 0f) {
            // if we're at zero volume, don't play it at all
            return;
        }

        File mfile = _ctx.getResourceManager().getResourceFile(musicPath);
        if (!mfile.exists()) {
            log.warning("Requested to play non-existent music " +
                        "[path=" + musicPath + "].");
            return;
        }

        // stop any currently playing stream
        if (_mstream != null) {
            _mstream.stop();
            _mstream.close();
            _mstream = null;
        }

        try {
            _playingMusic = musicPath;
            _mstream = new MusicStream(mfile.toString(), false);
            _mstream.loop(loop);
            _mstream.setVolume(volume);
            _mstream.play();
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to start music " +
                    "[path=" + mfile + "].", t);
        }
    }

    /**
     * Adjusts the volume of any currently playing music.
     *
     * @param volume a value between 0 and 100.
     */
    public void setMusicVolume (int volume)
    {
        if (_mstream != null) {
            _mstream.setVolume(volume / 100f);
        }
    }

    /**
     * Parses some system properties and starts a quick test game vs the
     * computer. Used by developers.
     */
    public void startTestGame (boolean tutorial)
    {
        ReportingListener rl =
            new ReportingListener(_ctx, BANG_MSGS, "m.quick_start_failed");
        PlayerService psvc = (PlayerService)
            _ctx.getClient().requireService(PlayerService.class);

        // start a tutorial if requested
        if (tutorial) {
            psvc.playTutorial(_ctx.getClient(),
                              System.getProperty("tutorial"), rl);

        } else {
            // otherwise we're starting a test game versus the computer
            int pcount;
            try {
                pcount = Integer.parseInt(System.getProperty("players"));
            } catch (Throwable t) {
                pcount = 4;
            }
            psvc.playComputer(_ctx.getClient(), pcount,
                              System.getProperty("scenario", "gr"),
                              System.getProperty("board"), rl);
        }
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

        // configure our performance reporting
        PerfMonitor.setReportToChat(_ctx.getUserObject().tokens.isAdmin());

        // developers can jump right into a tutorial or game
        if (!StringUtil.isBlank(System.getProperty("test"))) {
            startTestGame(false);
            return;
        } else if (!StringUtil.isBlank(System.getProperty("tutorial"))) {
            startTestGame(true);
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

        // shut her right on down
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
        // clear out the status view because the main view is switching
        if (_status != null) {
            if (_status.isAdded()) {
                clearPopup(_status, true);
            }
            _status = null;
        }

        // now add the main view
        _mview = view;
        _ctx.getRootNode().addWindow(_mview);

        if (!(view instanceof BangView)) {
            // if this is not the game view, play the town theme
            String townId = _ctx.getUserObject().townId;
            queueMusic("sounds/music/" + townId + ".ogg", true);

            // also re-wire up our options view whenever the main view changes
            // as the BangView overrides the escape mapping during the game
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

    /**
     * Called by the main app when we're about to exit (cleanly).
     */
    protected void willExit ()
    {
        // stop any currently playing stream
        if (_mstream != null) {
            _mstream.stop();
            _mstream.close();
            _mstream = null;
        }

        // clean up the UI bits
        BangUI.shutdown();
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
            // nada
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
    protected StatusView _status;

    protected ArrayList<Name> _invites = new ArrayList<Name>();

    protected String _playingMusic;
    protected MusicStream _mstream;
}
