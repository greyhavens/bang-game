//
// $Id: BangClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.glu.GLU;

import com.jme.input.KeyInput;
import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.EventListener;

import com.samskivert.servlet.user.Password;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.ResultListener;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;
import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.getdown.util.LaunchUtil;
import com.threerings.hemiptera.data.Report;
import com.threerings.hemiptera.util.SendReportUtil;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.effect.WindowSlider;

import com.threerings.openal.OggFileStream;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.CurseFilter;
import com.threerings.crowd.chat.client.MuteDirector;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.client.PlaceView;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.ranch.client.FirstBigShotView;

import com.threerings.bang.chat.client.BangChatDirector;
import com.threerings.bang.chat.client.PardnerChatView;
import com.threerings.bang.chat.client.SystemChatView;

import com.threerings.bang.game.client.BangView;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.util.BoardCache;
import com.threerings.bang.client.util.PerfMonitor;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangAuthResponseData;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
public class BangClient extends BasicClient
    implements ClientObserver, BangCodes
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
    public static boolean checkForUpgrade (BangContext ctx, String message)
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
        if (relaunchGetdown(ctx, 3000L)) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the specified town has been activated, false otherwise.
     */
    public static boolean isTownActive (String townId)
    {
        // frontier town is always active
        return townId.equals(BangCodes.FRONTIER_TOWN) ? true :
            new File(localDataDir(townId + ".dat")).exists();
    }

    /**
     * Creates the auxiliary resource bundle activation file for the specified
     * town and runs Getdown to download that town's resources, exiting the
     * application after a short delay.
     *
     * @return true if the process is started, false if the town is already
     * activated.
     *
     * @exception IOException thrown if the town activation file could not be
     * created for some reason.
     */
    public static boolean activateTown (BangContext ctx, String townId)
        throws IOException
    {
        // create the activation file if we don't already have it
        File afile = new File(localDataDir(townId + ".dat"));
        if (afile.exists()) {
            return false;
        }
        afile.createNewFile();

        // note that we want to go straight to the train station next time we
        // log on to make it a little easier to do the first time download
        File mfile = new File(localDataDir("go_station.dat"));
        if (!mfile.exists()) {
            try {
                mfile.createNewFile();
            } catch (IOException ioe) {
                log.log(Level.WARNING,
                    "Failed to create marker file '" + mfile + "'.", ioe);
            }
        }

        // relaunch getdown (communicating failure with an exception because
        // returning false means we're already activated)
        if (!relaunchGetdown(ctx, 500L)) {
            throw new IOException("m.getdown_relaunch_failed");
        }

        return true;
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
        report.submitter = (user == null) ?
            "<unknown>" : user.username.toString();
        if (descrip.length() > 255) {
            report.summary = StringUtil.truncate(descrip, 255);
            report.setAttribute("Description", descrip);
        } else {
            report.summary = descrip;
        }
        report.version = String.valueOf(DeploymentConfig.getVersion());
        if (user != null) {
            report.setAttribute("Handle", "" + user.handle);
        }
        report.setAttribute("Driver", Display.getAdapter());
        report.setAttribute("GL Display Mode", "" + Display.getDisplayMode());
        report.setAttribute("GL Version", GL11.glGetString(GL11.GL_VERSION));
        report.setAttribute("GL Vendor", GL11.glGetString(GL11.GL_VENDOR));
        report.setAttribute("GL Renderer", GL11.glGetString(GL11.GL_RENDERER));
        report.setAttribute("GL Extensions",
                            GL11.glGetString(GL11.GL_EXTENSIONS));
        report.setAttribute("GLU Extensions",
                            GLU.gluGetString(GLU.GLU_EXTENSIONS));
        report.setAttribute("Graphics Detail", 
                BangPrefs.getDetailLevel().toString());

        // and send it along with our debug logs
        URL submitURL = DeploymentConfig.getBugSubmitURL();
        if (submitURL == null) {
            log.warning("Unable to submit bug report, no submit URL.");
            return;
        }

        log.info("Submitting bug report '" + descrip + "'.");
        String[] files = { BangClient.localDataDir("old-bang.log"),
                           BangClient.localDataDir("bang.log")};
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
    public void init (BangApp app, boolean failureMode)
    {
        _ctx = new BangContextImpl();
        initClient(_ctx, app, app);

        // if we're recovering from a failure, stop here
        if (failureMode) {
            return;
        }

        // set our proper window title
        Display.setTitle(_ctx.xlate(BangCodes.BANG_MSGS, "m.app_title"));

        // upgrade getdown if appropriate
        File newgd = new File(localDataDir("code/getdown-pro-new.jar"));
        File curgd = new File(localDataDir("getdown-pro.jar"));
        File oldgd = new File(localDataDir("getdown-pro-old.jar"));
        LaunchUtil.upgradeGetdown(oldgd, curgd, newgd);

        // listen for logon
        _client.addClientObserver(this);

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

        // start our idle tracker
        new IdleTracker().start();
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
     * Returns the entity that manages our popups.
     */
    public FKeyPopups getPopupManager ()
    {
        return _functionPopup;
    }

    /**
     * Returns the player status view; creating the view if necessary (as long
     * as <code>create</code> is true). We purge the view from memory every
     * time the main view is switched, but preserve it while in one place.
     */
    public StatusView getStatusView (boolean create)
    {
        if (_status == null && create) {
            _status = new StatusView(_ctx);
        }
        return _status;
    }

    /**
     * Potentially shows the next phase of the client introduction or tutorial
     * or displays pending notifications. Basically anything that should be
     * popped up once a player is in the town view and ready to go is shown.
     * This is called after first logging on and then at the completion of each
     * phase of the intro and tutorial.
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

        // if requested, recommend a lower detail level
        if (_suggestLowerDetail && displayLowerDetailSuggestion()) {
            return true;
        }

        // if there are any pending notifications, show the first one
        for (Notification notification : user.notifications) {
            if (!notification.responded) {
                displayNotification(notification);
                return true;
            }
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
     * Returns true if there are popups showing.
     */
    public boolean hasPopups ()
    {
        return _popups.size() > 0;
    }

    /**
     * Determines whether we can display a pop-up at the moment.
     */
    public boolean canDisplayPopup (MainView.Type type)
    {
        // don't allow popups during view transitions
        if (_viewTransition) {
            return false;
        }

        // only allow status view after we've created our avatar
        if (type == MainView.Type.STATUS &&
            _ctx.getUserObject().handle == null) {
            return false;
        }

        // don't allow FKEY or STATUS popups if we have other popups showing
        if ((type == MainView.Type.FKEY || type == MainView.Type.STATUS) &&
            hasPopups()) {
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
        if (popup == null) {
            log.warning("Some naughty boy tried to display a null popup.");
            Thread.dumpStack();
            return;
        }

        _ctx.getRootNode().addWindow(popup);
        _popups.add(popup);

        if (animate) {
            BangUI.play(BangUI.FeedbackSound.WINDOW_OPEN);
            popup.pack(twidth, -1);
            int dy = (_mview instanceof BangView) ?
                ((BangView)_mview).getCenterOffset() : 0;
            _ctx.getInterface().attachChild(
                new WindowSlider(popup, WindowSlider.FROM_TOP, 0.25f, 0, dy));
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
            int dy = (_mview instanceof BangView) ?
                ((BangView)_mview).getCenterOffset() : 0;
            _ctx.getInterface().attachChild(
                new WindowSlider(popup, WindowSlider.TO_RIGHT, 0.25f, 0, dy) {
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
     * @param key the track key (e.g., "frontier_town/post_game0")
     * @param crossfade if non-zero, the interval over which to fade out the
     * music previously playing and fade in the new music
     */
    public void queueMusic (String key, boolean loop, float crossfade)
    {
        // if we're already playing this track, keep it running
        if (key.equals(_playingMusic)) {
            return;
        }

        // set the volume based on the user's volume preferences
        float volume = BangPrefs.getMusicVolume() / 100f;
        if (volume == 0f) {
            // if we're at zero volume, don't play it at all
            return;
        }

        String prefix = "sounds/music/" + key;
        File mfile = _rsrcmgr.getResourceFile(prefix + ".ogg"),
            ifile = _rsrcmgr.getResourceFile(prefix + "_intro.ogg");
        if (!mfile.exists()) {
            log.warning("Requested to play non-existent music " +
                        "[key=" + key + "].");
            return;
        }

        // stop any currently playing stream
        boolean wasPlaying = (_mstream != null && _mstream.isPlaying());
        if (wasPlaying) {
            if (crossfade > 0f) {
                _mstream.fadeOut(crossfade, true);
            } else {
                _mstream.dispose();
            }
            _mstream = null;
        }

        try {
            _playingMusic = key;
            if (!_playedIntro && ifile.exists()) {
                _playedIntro = true;
                _mstream = new OggFileStream(_soundmgr, ifile, false);
                _mstream.queueFile(mfile, loop);
            } else {
                _mstream = new OggFileStream(_soundmgr, mfile, loop);
            }
            _mstream.setGain(volume);
            if (wasPlaying && crossfade > 0f) {
                _mstream.fadeIn(crossfade);
            } else {
                _mstream.play();
            }
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
            _mstream.setGain(volume / 100f);
        }
    }

    /**
     * Causes any currently playing music to fade out over the period.
     */
    public void fadeOutMusic (float period)
    {
        if (_mstream != null && _mstream.isPlaying()) {
            _mstream.fadeOut(period, true);
            _mstream = null;
            _playingMusic = null;
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
            int rounds = Integer.getInteger("rounds", 1);
            String scenario = System.getProperty("scenario");
            String[] scenarios;
            if (scenario != null) {
                scenarios = new String[rounds];
                Arrays.fill(scenarios, scenario);
            } else {
                scenarios = ScenarioInfo.selectRandomIds(
                    _ctx.getUserObject().townId, rounds, pcount, null, false);
            }
            String board = System.getProperty("board");
            if (board != null) {
                // hackery to work around shell escaping problems
                board = board.replace("~", "'");
            }
            psvc.playComputer(
                _ctx.getClient(), pcount, scenarios, board,
                Boolean.parseBoolean(System.getProperty("autoplay")), rl);
        }
    }

    /**
     * Creates and initializes credentials that can be used to authenticate
     * with the server.
     *
     * @param username the username to use when logging in.
     * @param password the cleartext of the password to use when logging in.
     */
    public BangCredentials createCredentials (Name username, String password)
    {
        BangCredentials creds = new BangCredentials(
            username, Password.makeFromClear(password));
        creds.ident = IdentUtil.getMachineIdentifier();
        // if we got a real ident from the client, mark it as such
        if (creds.ident != null && !creds.ident.matches("S[A-Za-z0-9/+]{32}")) {
            creds.ident = "C" + creds.ident;
        }
        return creds;
    }

    /**
     * Logs off of our current server and logs onto the lobby server for the
     * specified town.
     */
    public void switchToTown (String townId)
    {
        if (_pendingTownId != null) {
            log.warning("Refusing to switch to town, we're already headed " +
                "somewhere [townId=" + townId +
                ", pTownId=" + _pendingTownId + "].");
            return;
        }

        // note that we're switching towns
        _pendingTownId = townId;

        // if we are not logged on (maybe we failed to connect to a town and
        // now we're trying to go back to the previous town) then try logging
        // on to the new server immediately
        if (!_ctx.getClient().isLoggedOn()) {
            clientDidClear(_ctx.getClient());
        } else {
            // logoff off from our current town; our logged off observer will
            // then log us onto the pending town (we need to wait until we're
            // fully logged off before doing, hence this hoop jumping)
            _ctx.getClient().logoff(true);
        }
    }

    // documentation inherited from interface ClientObserver
    public void clientFailedToLogon (Client client, Exception cause)
    {
        // nothing doing
    }

    // documentation inherited from interface ClientObserver
    public void clientDidLogon (Client client)
    {
        // if we were provided with a machine identifier, write it out
        BangAuthResponseData bard = (BangAuthResponseData)
            client.getAuthResponseData();
        if (bard != null && bard.ident != null) {
            IdentUtil.setMachineIdentifier(bard.ident);
        }

        // update our title to contain our username
        Display.setTitle(_msgmgr.getBundle(BangCodes.BANG_MSGS).get(
                             "m.online_title", _ctx.getUserObject().username));

        // get a reference to the player service
        _psvc = (PlayerService)_client.requireService(PlayerService.class);

        // create the mute director here because the mute list is specific to
        // the account
        final String mkey = client.getCredentials().getUsername() + ".muted";
        _mutedir = new MuteDirector(
            _ctx, createHandles(_config.getValue(mkey, new String[0])));
        _mutedir.setChatDirector(_chatdir);
        _mutedir.addMuteObserver(new MuteDirector.MuteObserver() {
            public void muteChanged (Name player, boolean nowMuted) {
                _config.setValue(mkey, StringUtil.join(_mutedir.getMuted()));
            }
        });

        // register our status view key bindings
        StatusView.bindKeys(_ctx);

        // listen for notifications to pop up
        client.getClientObject().addListener(_nlistener);
        
        // developers can jump right into a tutorial or game
        if (!StringUtil.isBlank(System.getProperty("test"))) {
            startTestGame(false);
            return;
        } else if (!StringUtil.isBlank(System.getProperty("tutorial"))) {
            startTestGame(true);
            return;
        }

        // check for a marker file indicating that we should go straight to the
        // train station when we first start up which we do to smooth out the
        // process of first downloading a new town's media
        String where = null;
        File mfile = new File(localDataDir("go_station.dat"));
        if (mfile.exists()) {
            mfile.delete();
            where = "station";
        }

        // next check for a "go" parameter
        if (where == null) {
            where = System.getProperty("go");
        }

        // finally go somewhere if we were asked to do so
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
        } else if ("station".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.stationOid);
            return;
        } else if ("hideout".equals(where)) {
            _ctx.getLocationDirector().moveTo(bbd.hideoutOid);
            return;
        }

        // show the town view to start, this will call checkShowIntro() once
        // the town view has "presented" the first town
        showTownView();
    }

    // documentation inherited from interface ClientObserver
    public void clientObjectDidChange (Client client)
    {
        // nada
    }

    // documentation inherited from interface ClientObserver
    public void clientConnectionFailed (Client client, Exception cause)
    {
        // nothing doing
    }

    // documentation inherited from interface ClientObserver
    public boolean clientWillLogoff (Client client)
    {
        return true; // always fine with us
    }

    // documentation inherited from interface ClientObserver
    public void clientDidLogoff (Client client)
    {
        // clear our status view key bindings
        StatusView.clearKeys(_ctx);

        // stop listening to the client object
        client.getClientObject().removeListener(_nlistener);
        
        if (_pendingTownId == null) {
            // shut her right on down
            _ctx.getApp().stop();
        }
    }

    // documentation inherited from interface ClientObserver
    public void clientDidClear (Client client)
    {
        if (_pendingTownId != null) {
            _ctx.getClient().setServer(
                DeploymentConfig.getServerHost(_pendingTownId),
                DeploymentConfig.getServerPorts(_pendingTownId));
            if (!_ctx.getClient().logon()) {
                log.warning("Trying to connect to " + _pendingTownId +
                            " but we're already logged on!?");
            }
            _pendingTownId = null;
        }
    }

    /**
     * Pops up a dialog suggesting a lower level of graphical detail to the
     * user, or saves the suggestion until it can be displayed.
     */
    public void suggestLowerDetail ()
    {
        if (canDisplayPopup(MainView.Type.DETAIL_SUGGESTION)) {
            displayLowerDetailSuggestion();
        } else {
            _suggestLowerDetail = true;
        }
    }
    
    protected Handle[] createHandles (String[] strings)
    {
        Handle[] handles = new Handle[strings.length];
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
        String curse = _ctx.xlate(BangCodes.CHAT_MSGS, "x.cursewords");
        String stop = _ctx.xlate(BangCodes.CHAT_MSGS, "x.stopwords");
        _chatdir.addChatFilter(new CurseFilter(curse, stop) {
            public CurseFilter.Mode getFilterMode () {
                return BangPrefs.getChatFilterMode();
            }
        });
        _bcache = new BoardCache();

        // warm up the particle pool
        ParticlePool.warmup(_ctx);
    }

    protected void displayNotification (final Notification notification)
    {
        final ReportingListener rl =
            new ReportingListener(_ctx, BANG_MSGS, "e.response_failed");
            
        // if it comes from a person on our mute list, auto-reject it
        final Handle source = notification.getSource();
        if (source != null && _ctx.getMuteDirector().isMuted(source)) {
            log.info("Auto-rejecting notification [who=" + source + ", notification=" +
                notification + "].");
            _psvc.respondToNotification(
                _client, notification.getKey(), notification.getRejectIndex(), rl);
            // flag this notification as answered and loop back to checkShowIntro
            // in case there are more
            notification.responded = true;
            checkShowIntro();
            return;
        }
        
        // append the mute button if it comes from a person
        String[] buttons = notification.getResponses();
        if (source != null) {
            buttons = ArrayUtil.append(buttons, MessageBundle.qualify(
                BANG_MSGS, MessageBundle.tcompose("m.notification_ignore", source)));
        }
        
        OptionDialog.showConfirmDialog(
            _ctx, notification.getBundle(), notification.getText(), buttons,
            new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (button >= notification.getResponses().length) { // ignore the pesky bugger
                    _ctx.getMuteDirector().setMuted(source, true);
                    button = notification.getRejectIndex();
                }
                _psvc.respondToNotification(_client, notification.getKey(), button, rl);
                // flag this notification as answered and loop back to checkShowIntro
                // in case there are more
                notification.responded = true;
                checkShowIntro();
            }
        });
    }
    
    protected boolean displayLowerDetailSuggestion ()
    {
        _suggestLowerDetail = false;
        if (!BangPrefs.isMediumDetail()) {
            return false; // already at lowest detail level
        }
        final BangPrefs.DetailLevel current = BangPrefs.getDetailLevel(),
            lower = BangPrefs.isHighDetail() ?
                BangPrefs.DetailLevel.MEDIUM : BangPrefs.DetailLevel.LOW;
        OptionDialog.ResponseReceiver rr =
            new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (button == 0) { // switch
                    BangPrefs.updateDetailLevel(lower);
                } else if (button == 2) { // disable suggestions
                    BangPrefs.setSuggestDetail(false);
                }
                checkShowIntro();
            }
        };
        String text = MessageBundle.compose("m.detail_suggest",
            "m.detail_" + current.toString().toLowerCase(),
            "m.detail_" + lower.toString().toLowerCase());
        OptionDialog.showConfirmDialog(_ctx, "options", text,
            new String[] { "m.detail_yes", "m.detail_no", "m.detail_dontask" },
            rr);
        return true;
    }
    
    protected void setMainView (final BWindow view)
    {
        // if the new view is a game view, fade out the current music as we
        // fade out the current view
        if (view instanceof BangView && _mstream != null) {
            _mstream.fadeOut(0.5f, true);
            _mstream = null;
        }

        // if we have an existing main view, fade that out
        if (_mview != null) {
            FadeInOutEffect fade =
                new FadeInOutEffect(ColorRGBA.black, 0f, 1f, 0.5f, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    _ctx.getRootNode().removeWindow(_mview);
                    _viewTransition = false;
                    fadeInMainView(view);
                }
            };
            _ctx.getInterface().attachChild(fade);
            _viewTransition = true;
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
            queueMusic(townId + "/town", true, 3f);

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
            _mstream.dispose();
            _mstream = null;
        }

        // clean up the UI bits
        BangUI.shutdown();
    }

    /**
     * Attempts to relaunch Getdown, exiting the application after a short
     * pause.
     *
     * @return true if Getdown was successfully relaunched, false if we were
     * unable to do so for a variety of reasons (which will have been logged).
     */
    protected static boolean relaunchGetdown (
        final BangContext ctx, long exitDelay)
    {
        File pro = new File(localDataDir("getdown-pro.jar"));
        if (LaunchUtil.mustMonitorChildren() || !pro.exists()) {
            return false;
        }

        String[] args = new String[] {
            LaunchUtil.getJVMPath(), "-jar", pro.toString(), localDataDir("")
        };
        log.info("Running " + StringUtil.join(args, "\n  "));
        try {
            Runtime.getRuntime().exec(args, null);
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to run getdown", ioe);
            return false;
        }

        // now stick a fork in ourselves after the caller specified delay
        new Interval(ctx.getClient().getRunQueue()) {
            public void expired () {
                log.info("Exiting for Getdown relaunch.");
                ctx.getApp().stop();
            }
        }.schedule(exitDelay);

        return true;
    }

    /** Tracks user idleness and lets the server know when we're idle and
     * eventually logs us off. */
    protected class IdleTracker extends Interval
        implements EventListener
    {
        public IdleTracker () {
            super(_ctx.getClient().getRunQueue());
        }

        public void start () {
            _ctx.getRootNode().addGlobalEventListener(this);
            _lastEventStamp = _ctx.getRootNode().getTickStamp();
            schedule(10000L, true);
        }

        public void expired () {
            long idle = _ctx.getRootNode().getTickStamp() - _lastEventStamp;
            if (!_isIdle && idle > ChatCodes.DEFAULT_IDLE_TIME) {
                updateIdle(true, idle);
            }
            if (idle > LOGOFF_DELAY) {
                if (_ctx.getClient().isLoggedOn()) {
                    log.info("Client idled out, exiting.");
                    _ctx.getApp().stop();
                }
            }
        }

        public void eventDispatched (BEvent event) {
            _lastEventStamp = (event.getWhen() > 0L) ?
                event.getWhen() : _ctx.getRootNode().getTickStamp();
            if (_isIdle) {
                updateIdle(false, 0L);
            }
        }

        protected void updateIdle (boolean isIdle, long idleTime) {
            _isIdle = isIdle;
            log.info("Setting idle " + isIdle + " [time=" + idleTime + "].");
            if (_ctx.getClient().isLoggedOn()) {
                BodyService bsvc = (BodyService)
                    _ctx.getClient().requireService(BodyService.class);
                bsvc.setIdle(_ctx.getClient(), isIdle);
            }
        }

        protected boolean _isIdle;
        protected long _lastEventStamp;
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
    protected String _pendingTownId;

    protected SetAdapter _nlistener = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(PlayerObject.NOTIFICATIONS) &&
                canDisplayPopup(MainView.Type.NOTIFICATION)) {
                displayNotification((Notification)event.getEntry());
            }
        }
    };
    
    protected BangChatDirector _chatdir;
    protected BoardCache _bcache;
    protected MuteDirector _mutedir;
    protected PlayerService _psvc;

    protected BWindow _mview;
    protected ArrayList<BWindow> _popups = new ArrayList<BWindow>();
    protected PardnerChatView _pcview;
    protected SystemChatView _scview;
    protected StatusView _status;

    protected boolean _suggestLowerDetail;
    
    protected String _playingMusic;
    protected OggFileStream _mstream;
    protected boolean _playedIntro;
    protected boolean _viewTransition = false;

    /** The time in milliseconds after which we log off an idle user. */
    protected static final long LOGOFF_DELAY = 8L * 60L * 1000L;
}
