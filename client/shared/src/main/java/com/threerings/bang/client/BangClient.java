//
// $Id: BangClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Input.Keys;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.EventListener;

import com.samskivert.servlet.user.Password;
import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Config;
import com.samskivert.util.Interval;
import com.samskivert.util.ResultListener;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;
import com.threerings.util.BrowserUtil;
import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.getdown.util.LaunchUtil;
import com.threerings.hemiptera.data.Report;
import com.threerings.hemiptera.util.SendReportUtil;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.effect.WindowSlider;

import com.threerings.openal.FileStream;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.CurseFilter;
import com.threerings.crowd.chat.client.MuteDirector;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.admin.data.AdminCodes;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.ranch.data.RanchObject;
import com.threerings.bang.station.client.FreePassView;
import com.threerings.bang.station.client.PassDetailsView;

import com.threerings.bang.chat.client.BangChatDirector;
import com.threerings.bang.chat.client.PardnerChatView;
import com.threerings.bang.chat.client.SystemChatView;

import com.threerings.bang.game.client.BangView;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.bounty.data.OfficeObject;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonObject;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.util.BoardCache;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangAuthResponseData;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.FreeTicket;
import com.threerings.bang.data.GuestHandle;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Shop;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up all of the necessary
 * configuration and getting the client bootstrapped.
 */
public class BangClient extends BasicClient
    implements ClientObserver, BangCodes
{
    /** A marker interface for non-clearable popups. */
    public static interface NonClearablePopup
    {
    }

    /**
     * Checks the supplied logon failure message for client version related failure and decodes the
     * necessary business to instruct Getdown to update the client on the next invocation.
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
            log.warning("Error creating '" + vfile + "'", ioe);
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
     * Creates the auxiliary resource bundle activation file for the specified town and runs
     * Getdown to download that town's resources, exiting the application after a short delay.
     *
     * @return true if the process is started, false if the town is already activated.
     *
     * @exception IOException thrown if the town activation file could not be created for some
     * reason.
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

        // note that we want to go straight to the train station next time we log on to make it a
        // little easier to do the first time download
        File mfile = new File(localDataDir("go_station.dat"));
        if (!mfile.exists()) {
            try {
                mfile.createNewFile();
            } catch (IOException ioe) {
                log.warning("Failed to create marker file '" + mfile + "'.", ioe);
            }
        }

        // relaunch getdown (communicating failure with an exception because returning false means
        // we're already activated)
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
        report.submitter = (user == null) ? "<unknown>" : user.username.toString();
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
        report.setAttribute("GL Extensions", GL11.glGetString(GL11.GL_EXTENSIONS));
        report.setAttribute("Graphics Detail", BangPrefs.getDetailLevel().toString());

        // and send it along with our debug logs
        URL submitURL = DeploymentConfig.getBugSubmitURL();
        if (submitURL == null) {
            log.warning("Unable to submit bug report, no submit URL.");
            return;
        }

        log.info("Submitting bug report '" + descrip + "'.");
        String[] files = { BangClient.localDataDir("old-bang.log"),
                           BangClient.localDataDir("bang.log")};
        ResultListener<Object> rl = new ResultListener<Object>() {
            public void requestCompleted (Object result) {
                ctx.getChatDirector().displayFeedback(BANG_MSGS, "m.bug_submit_completed");
                if (exitAfterSubmit) {
                    ctx.getApp().stop();
                }
            }
            public void requestFailed (Exception cause) {
                log.warning("Bug submission failed.", cause);
                ctx.getChatDirector().displayFeedback(BANG_MSGS, "m.bug_submit_failed");
            }
        };
        SendReportUtil.submitReportAsync(
            submitURL, report, files, ctx.getClient().getRunQueue(), rl);
        ctx.getChatDirector().displayFeedback(BANG_MSGS, "m.bug_submit_started");
    }

    /**
     * Attempts to locate and read the file created by our installer which contains the name of the
     * installer (or an affiliate identifier) which contains any affiliate information that we
     * might need.
     */
    public static String getAffiliateFromInstallFile ()
    {
        // look for the handy dandy file created by the installer
        File insfile = new File(localDataDir("installer.txt"));
        if (!insfile.exists()) {
            return null;
        }

        try {
            BufferedReader bin = new BufferedReader(
                new InputStreamReader(new FileInputStream(insfile)));
            String name = bin.readLine();
            if (StringUtil.isBlank(name)) {
                return null;
            }

            // check for an new-style string with site id and tag id
            Matcher m = Pattern.compile("bang-(.*)-install").matcher(name);
            if (m.find()) {
                return m.group(1);
            }

            // if it's just bang-install then return empty string (no affiliate)
            if (name.indexOf("bang-install") != -1) {
                return null;
            }

            // otherwise return the whole thing
            return name;

        } catch (Exception e) {
            // not readable, no problem
        }
        return null;
    }

    /**
     * Initializes a new client and provides it with a frame in which to display everything.
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

        // initialize their detail level
        initDetailLevel();

        // listen for logon
        _client.addClientObserver(this);

        // create the pardner chat view, which will listen for tells from pardners and pop up when
        // possible
        _pcview = new PardnerChatView(_ctx);

        // create the system chat view, which will display system chat messages outside of games
        _scview = new SystemChatView(_ctx);

        // register our global key bindings
        _functionPopup = new FKeyPopups(_ctx);

        // setup our modal shade color
        _ctx.getRootNode().setModalShade(BangUI.MODAL_SHADE);

        // configure our guest handle name
        GuestHandle.setDefaultName(_ctx.xlate(BangCodes.BANG_MSGS, "m.you"));

        // create and display the logon view; which we do by hand instead of using setMainView()
        // because we don't want to start the resource resolution until we're faded in
        final LogonView lview = new LogonView(_ctx);
        _mview = lview;
        _ctx.getRootNode().addWindow(_mview);
        _mview.pack();
        _mview.center();
        FadeInOutEffect fade = new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, true) {
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
     * Returns a reference to the context in effect for this client. This reference is valid for
     * the lifetime of the application.
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
     * Returns the player status view; creating the view if necessary (as long as
     * <code>create</code> is true). We purge the view from memory every time the main view is
     * switched, but preserve it while in one place.
     */
    public StatusView getStatusView (boolean create)
    {
        if (_status == null && create) {
            _status = new StatusView(_ctx);
        }
        return _status;
    }

    /**
     * Returns the prior location identifier.
     */
    public String getPriorLocationIdent ()
    {
        return _priorLocationIdent;
    }

    /**
     * Returns the prior location oid.
     */
    public int getPriorLocationOid ()
    {
        return _priorLocationOid;
    }

    /**
     * Convenience method for going to a particular shop.
     */
    public void goTo (Shop shop)
    {
        BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
        _ctx.getLocationDirector().moveTo(bbd.getPlaceOid(shop));
    }

    /**
     * Adds a notification to be displayed next time the player is in town.
     */
    public void queueTownNotificaton (Runnable notifier)
    {
        _pendingNots.add(notifier);
    }

    /**
     * Displays any pending notifications to the player. This is called automatically on entering
     * the town view and again as popups are cleared and generally needn't be called manually.
     */
    public boolean checkNotifications ()
    {
        PlayerObject user = _ctx.getUserObject();

        // if there are any pending server notifications, show the first one
        for (Notification notification : user.notifications) {
            if (!notification.responded) {
                return displayNotification(notification);
            }
        }

        // if we have pending notifications, run the next one on the list
        if (!_pendingNots.isEmpty()) {
            _pendingNots.remove(0).run();
            return true;
        }

        // if the main view is the town view, activate it because we're done fooling around
        if (isShowingTownView()) {
            ((TownView)_mview).setActive(true);
        }
        return false;
    }

    /**
     * Called when the create avatar view is dismissed. Potentially continues the new user
     * configuration process.
     *
     * @param created true if the user created their avatar, false if they canceled the dialog.
     */
    public void createAvatarDismissed (boolean created)
    {
        // if we're in a shop and either created our avatar or didn't, then just go back to
        // whatever we were doing before
        if (_ctx.getLocationDirector().getPlaceObject() != null) {
            return;
        }
        // otherwise make sure we're in town
        resetTownView();
    }

    /**
     * Called when a popup is cleared that responded to a moveTo failure.
     */
    public void continueMoveTo ()
    {
        if (_headingTo != -1) {
            _ctx.getLocationDirector().moveTo(_headingTo);
            _headingTo = -1;
            return;
        }
        resetTownView();
    }

    /**
     * Leaves our current location and displays the town view. Also first fades out any existing
     * main view.
     *
     * @return true if we were able to leave our current location, false if the departure was
     * vetoed.
     */
    public boolean showTownView ()
    {
        // if we're currently in a place, attempt to leave it
        if (_ctx.getLocationDirector().getPlaceObject() != null &&
            !_ctx.getLocationDirector().leavePlace()) {
            return false; // sorry charlie
        }
        if (!isShowingTownView()) {
            setMainView(new TownView(_ctx));
        }
        return true;
    }

    /**
     * Returns true if we're currently displaying the town view.
     */
    public boolean isShowingTownView ()
    {
        return (_mview instanceof TownView);
    }

    /**
     * Resets the client to the town view.
     */
    public void resetTownView ()
    {
        if (isShowingTownView()) {
            ((TownView)_mview).resetViewpoint();
        } else {
            showTownView();
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

        // don't allow FKEY or STATUS popups if we have other popups showing
        if ((type == MainView.Type.FKEY || type == MainView.Type.STATUS) && hasPopups()) {
            return false;
        }

        // otherwise ask the view what they think about it
        if (_mview instanceof MainView) {
            return ((MainView)_mview).allowsPopup(type);
        } else {
            return type == MainView.Type.POSTER_DISPLAY;
        }
    }

    /**
     * Like {@link #displayPopup(BWindow,boolean)} but allows the specification of a desired width
     * for the popup.
     */
    public void displayPopup (BWindow popup, boolean animate, int twidth)
    {
        displayPopup(popup, animate, twidth, false);
    }

    /**
     * Like {@link #displayPopup(BWindow,boolean)} but allows the specification of a desired width
     * for the popup.
     */
    public void displayPopup (BWindow popup, boolean animate, int twidth, boolean topLayer)
    {
        if (popup == null) {
            log.warning("Some naughty boy tried to display a null popup.");
            Thread.dumpStack();
            return;
        }

        _ctx.getRootNode().addWindow(popup, topLayer);
        _popups.add(popup);

        if (animate) {
            animatePopup(popup, twidth);
        }
    }

    /**
     * Slides the popup window down on to the screen.
     */
    public void animatePopup (BWindow popup, int twidth)
    {
        BangUI.play(BangUI.FeedbackSound.WINDOW_OPEN);
        popup.pack(twidth, -1);
        int dy = (_mview instanceof BangView) ? ((BangView)_mview).getCenterOffset() : 0;
        _ctx.getInterface().attachChild(
            new WindowSlider(popup, WindowSlider.FROM_TOP, 0.25f, 0, dy));
    }

    /**
     * Displays a popup window that will automatically be cleared if we leave the current
     * "place". This should be used for any overlay view shown atop the normal place views.
     *
     * @param animate if true the popup will be {@link BWindow#pack}ed and slid onto the screen
     * from some direction. Otherwise the caller is responsible for {@link BWindow#pack}ing and
     * {@link BWindow#center}ing the window.
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
            int dy = (_mview instanceof BangView) ? ((BangView)_mview).getCenterOffset() : 0;
            _ctx.getInterface().attachChild(
                new WindowSlider(popup, WindowSlider.TO_RIGHT, 0.25f, 0, dy) {
                protected void slideComplete () {
                    super.slideComplete();
                    if (popup.isAdded()) {
                        _ctx.getRootNode().removeWindow(popup);
                    }
                    // if we're in town, see if we have additional notifications to show
                    if (isShowingTownView()) {
                        checkNotifications();
                    }
                }
            });
        } else {
            if (popup.isAdded()) {
                _ctx.getRootNode().removeWindow(popup);
            }
        }
    }

    /**
     * Creates an action listener that clears the supplied popup. Useful for cancel buttons.
     */
    public ActionListener makePopupClearer (final BWindow popup, final boolean animate)
    {
        return new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                clearPopup(popup, animate);
            }
        };
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
     * @param crossfade if non-zero, the interval over which to fade out the music previously
     * playing and fade in the new music
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

        // first check for the full length version
        File mfile = null, ifile = null;
        if (key.endsWith("/town")) {
            String townId = key.substring(0, key.length()-5);
            if (_ctx.getUserObject().ownsSong(townId)) {
                mfile = _rsrcmgr.getResourceFile("../soundtrack/" + townId + ".mp3");
            }
        } else if (key.indexOf("scenario_") != -1) {
            String scenario = key.substring(key.length() - 2);
            if (_ctx.getUserObject().ownsSong(scenario)) {
                mfile = _rsrcmgr.getResourceFile("../soundtrack/" + scenario + ".mp3");
            }
        } else {
            mfile = _rsrcmgr.getResourceFile("../soundtrack/" + key + ".mp3");
        }

        // if no full length version, use the regular version
        if (mfile == null || !mfile.exists()) {
            String prefix = "sounds/music/" + key;
            mfile = _rsrcmgr.getResourceFile(prefix + ".ogg");
            ifile = _rsrcmgr.getResourceFile(prefix + "_intro.ogg");
        }
        if (!mfile.exists()) {
            log.warning("Requested to play non-existent music", "key", key);
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
            if (!_playedIntro && ifile != null && ifile.exists()) {
                _playedIntro = true;
                _mstream = new FileStream(_soundmgr, ifile, false);
                _mstream.queueFile(mfile, loop);
            } else {
                _mstream = new FileStream(_soundmgr, mfile, loop);
            }

            _mstream.setGain(volume);
            if (wasPlaying && crossfade > 0f) {
                _mstream.fadeIn(crossfade);
            } else {
                _mstream.play();
            }

        } catch (Throwable t) {
            log.warning("Failed to start music", "path", mfile, t);
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
     * Parses some system properties and starts a quick test game vs the computer. Used by
     * developers.
     */
    public void startTestGame (boolean tutorial)
    {
        ReportingListener rl = new ReportingListener(_ctx, BANG_MSGS, "m.quick_start_failed");
        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);

        // start a tutorial if requested
        if (tutorial) {
            psvc.playTutorial(System.getProperty("tutorial"), rl);

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
                    _ctx.getUserObject().townId, rounds, pcount, null, false, Criterion.ANY);
            }
            String board = System.getProperty("board");
            if (board != null) {
                // hackery to work around shell escaping problems
                board = board.replace("~", "'");
            }
            boolean autoPlay = Boolean.parseBoolean(System.getProperty("autoplay"));
            psvc.playComputer(pcount, scenarios, board, autoPlay, rl);
        }
    }

    /**
     * Creates and initializes credentials that can be used to authenticate with the server.
     *
     * @param username the username to use when logging in.
     * @param password the cleartext of the password to use when logging in.
     */
    public BangCredentials createCredentials (Name username, String password)
    {
        BangCredentials creds = new BangCredentials(
            username, (password == null ? null : Password.makeFromClear(password)));
        creds.ident = IdentUtil.getMachineIdentifier();
        // if we got a real ident from the client, mark it as such
        if (creds.ident != null && !creds.ident.matches("S[A-Za-z0-9/+]{32}")) {
            creds.ident = "C" + creds.ident;
        }
        if (creds.anonymous) {
            creds.affiliate = getAffiliateFromInstallFile();
        }
        return creds;
    }

    /**
     * Logs off of our current server and logs onto the lobby server for the specified town.
     */
    public void switchToTown (String townId)
    {
        if (_pendingTownId != null) {
            log.warning("Refusing to switch to town, we're already headed somewhere",
                        "townId", townId, "pTownId", _pendingTownId);
            return;
        }

        // note that we're switching towns
        _pendingTownId = townId;

        // if we are not logged on (maybe we failed to connect to a town and now we're trying to go
        // back to the previous town) then try logging on to the new server immediately
        if (!_ctx.getClient().isLoggedOn()) {
            clientDidClear(_ctx.getClient());
        } else {
            // logoff off from our current town; our logged off observer will then log us onto the
            // pending town (we need to wait until we're fully logged off before doing, hence this
            // hoop jumping)
            _ctx.getClient().logoff(true);
        }
    }

    // documentation inherited from interface ClientObserver
    public void clientWillLogon (Client client)
    {
        client.addServiceGroup(AdminCodes.ADMIN_GROUP);
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
        BangAuthResponseData bard = (BangAuthResponseData)client.getAuthResponseData();
        if (bard != null && bard.ident != null) {
            IdentUtil.setMachineIdentifier(bard.ident);
        }

        // update our title to contain our username
        PlayerObject user = _ctx.getUserObject();
        if (!user.tokens.isAnonymous()) {
            Display.setTitle(
                _msgmgr.getBundle(BangCodes.BANG_MSGS).get("m.online_title", user.username));
        }

        // get a reference to the player service
        _psvc = _client.requireService(PlayerService.class);

        // create the mute director here because the mute list is specific to the account
        final String mkey = ((BangCredentials)client.getCredentials()).getUsername() + ".muted";
        _mutedir = new MuteDirector(
            _ctx, createHandles(BangPrefs.config.getValue(mkey, new String[0])));
        _mutedir.setChatDirector(_chatdir);
        _mutedir.addMuteObserver(new MuteDirector.MuteObserver() {
            public void muteChanged (Name player, boolean nowMuted) {
                BangPrefs.config.setValue(mkey, StringUtil.join(_mutedir.getMuted()));
            }
        });
        // update our chat throttle
        ((BangChatDirector)_ctx.getChatDirector()).checkClientThrottle();

        // register our status view key bindings
        StatusView.bindKeys(_ctx);

        // listen for notifications to pop up
        client.getClientObject().addListener(_nlistener);

        // listen to location changes so we know where we're coming from
        BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
        _ctx.getLocationDirector().addLocationObserver(_locationObserver);
        _priorLocationIdent = "saloon";
        _priorLocationOid = bbd.saloonOid;

        // developers can jump right into a tutorial or game
        if (!StringUtil.isBlank(System.getProperty("test"))) {
            startTestGame(false);
            return;
        } else if (!StringUtil.isBlank(System.getProperty("tutorial"))) {
            startTestGame(true);
            return;
        }

        // check for a marker file indicating that we should go straight to the train station when
        // we first start up which we do to smooth out the process of first downloading a new
        // town's media
        Shop where = null;
        File mfile = new File(localDataDir("go_station.dat"));
        if (mfile.exists()) {
            mfile.delete();
            where = Shop.STATION;
        } else {
            // check for a "go" parameter used by developers to go right to a shop
            try {
                where = Enum.valueOf(Shop.class, System.getProperty("go").toUpperCase());
            } catch (Exception e) {
                log.warning("Invalid 'go' parameter " + e);
            }
        }

        // go somewhere if we were asked to do so
        if (where != null) {
            goTo(where);
            return;
        }

        // if they've got a free ticket, potentially queue up a notification for it
        final FreeTicket fticket = user.getFreeTicket();
        if (fticket != null && !fticket.isActivated() &&
            BangPrefs.shouldShowPassDetail(user, fticket.getTownId())) {
            queueTownNotificaton(new Runnable() {
                public void run () {
                    displayPopup(new PassDetailsView(_ctx, fticket, false), true);
                }
            });
        }

        // if there's a free town open, potentially show it
        final TrainTicket tticket = user.getFreeTownTicket();
        if (tticket != null && !user.townId.equals(tticket.getTownId())) {
            queueTownNotificaton(new Runnable() {
                public void run () {
                    displayPopup(new FreePassView(_ctx, tticket), true);
                }
            });
        }

        // show the town view to start, it will call checkNotifications() once the town view has
        // "presented" the first town
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
        _logOffMsg = "connection";
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
        if (client.getClientObject() != null) {
            client.getClientObject().removeListener(_nlistener);
        }

        // clear our location observer
        _ctx.getLocationDirector().removeLocationObserver(_locationObserver);

        if (_logOffMsg != null) {
            showLogOffMessage(_logOffMsg);

        } else if (_pendingTownId == null) {
            showLogOffMessage(DEFAULT_LOGOFF_MESSAGE);
        }
    }

    // documentation inherited from interface ClientObserver
    public void clientDidClear (Client client)
    {
        if (_pendingTownId != null) {
            _ctx.getClient().setServer(DeploymentConfig.getServerHost(_pendingTownId),
                                       DeploymentConfig.getServerPorts(_pendingTownId));
            if (!_ctx.getClient().logon()) {
                log.warning("Trying to connect to " + _pendingTownId +
                            " but we're already logged on!?");
            }
            _pendingTownId = null;
        }
    }

    /**
     * Queues up a popup that suggests a lower level of graphical detail to the user. The popup
     * will be displayed next time the user enters town.
     */
    public void queueSuggestLowerDetail ()
    {
        if (!BangPrefs.shouldSuggestDetail() || BangPrefs.isMediumDetail()) {
            return; // already suggsted, or at lowest/user configured detail level
        }

        queueTownNotificaton(new Runnable() {
            public void run () {
                final BangPrefs.DetailLevel current = BangPrefs.getDetailLevel();
                final BangPrefs.DetailLevel lower = BangPrefs.isHighDetail() ?
                    BangPrefs.DetailLevel.MEDIUM : BangPrefs.DetailLevel.LOW;
                OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button == 0) { // switch
                            BangPrefs.updateDetailLevel(lower);
                        } else if (button == 2) { // disable suggestions
                            BangPrefs.setSuggestDetail(false);
                        }
                    }
                };
                String text = MessageBundle.compose(
                    "m.detail_suggest", "m.detail_" + StringUtil.toUSLowerCase(current.toString()),
                    "m.detail_" + StringUtil.toUSLowerCase(lower.toString()));
                OptionDialog.showConfirmDialog(_ctx, "options", text, new String[] {
                        "m.detail_yes", "m.detail_no", "m.detail_dontask" }, rr);
            }
        });
    }

    /**
     * Called to let the client potentially stop the app from shutting down.
     */
    public boolean shouldStop ()
    {
        if (!_ctx.getClient().isLoggedOn()) {
            return true;
        }
        PlayerObject user = _ctx.getUserObject();
        if (!user.tokens.isAnonymous() ||
            Iterables.filter(_popups, CreateAccountView.class).iterator().hasNext()) {
            return true;
        }
        CreateAccountView.show(_ctx, null, true);
        return false;
    }

    /**
     * Initialize the clients detail level if a preference isn't already set.
     */
    protected void initDetailLevel ()
    {
        if (BangPrefs.isDetailSet()) {
            return;
        }
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        if (renderer == null) {
            return;
        }

        for (String prefix : LOW_DETAIL) {
            if (renderer.startsWith(prefix)) {
                log.info("Setting default detail level to low", "renderer", renderer);
                BangPrefs.updateDetailLevel(BangPrefs.DetailLevel.LOW);
                return;
            }
        }

        for (String prefix : MEDIUM_DETAIL) {
            if (renderer.startsWith(prefix)) {
                log.info("Setting default detail level to medium", "renderer", renderer);
                BangPrefs.updateDetailLevel(BangPrefs.DetailLevel.MEDIUM);
                return;
            }
        }

        log.info("Setting default detail level to high", "renderer", renderer);
        BangPrefs.updateDetailLevel(BangPrefs.DetailLevel.HIGH);
    }

    /**
     * Called when the client log's off with a message.
     */
    protected void showLogOffMessage (String msg)
    {
        // stop the music
        if (_mstream != null) {
            _mstream.fadeOut(0.5f, true);
            _mstream = null;
        }

        // Clear out all the windows and just show the Idled out window
        _ctx.getRootNode().removeAllWindows();
        _mview = new LogOffView(_ctx, msg);
        _ctx.getRootNode().addWindow(_mview);
        _mview.pack();
        _mview.center();
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
        _chatdir.setMogrifyChat(BangPrefs.getChatMogrifierEnabled());
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

    protected boolean displayNotification (final Notification notification)
    {
        final ReportingListener rl = new ReportingListener(_ctx, BANG_MSGS, "e.response_failed");

        // if it comes from a person on our mute list, auto-reject it
        final Handle source = notification.getSource();
        if (source != null && _ctx.getMuteDirector().isMuted(source)) {
            log.info("Auto-rejecting notification", "who", source, "notification", notification);
            _psvc.respondToNotification(notification.getKey(), notification.getRejectIndex(), rl);
            notification.responded = true;
            return false;
        }

        // append the mute button if it comes from a person
        String[] buttons = notification.getResponses();
        if (source != null) {
            buttons = ArrayUtil.append(buttons, MessageBundle.qualify(
                BANG_MSGS, MessageBundle.tcompose("m.notification_ignore", source)));
        }

        OptionDialog.showConfirmDialog(
            _ctx, notification.getBundle(), notification.getTitle(), notification.getText(),
            buttons, notification.getEnabledDelay(), new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (button >= notification.getResponses().length) { // ignore the pesky bugger
                    _ctx.getMuteDirector().setMuted(source, true);
                    button = notification.getRejectIndex();
                }
                _psvc.respondToNotification(notification.getKey(), button, rl);
                notification.responded = true;
            }
        });
        return true;
    }

    protected void setMainView (final BWindow view)
    {
        // if the view is a game view, fade out the current music as we fade out the current view
        if (view instanceof BangView) {
            fadeOutMusic(0.5f);
        }

        // if we have an existing main view, fade that out
        if (_mview != null) {
            FadeInOutEffect fade = new FadeInOutEffect(ColorRGBA.black, 0f, 1f, 0.5f, true) {
                protected void fadeComplete () {
                    super.fadeComplete();
                    _ctx.getRootNode().removeAllWindows();
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
        _scview.maybeShow();

        if (!(view instanceof BangView)) {
            // if this is not the game view, play the town theme
            String townId = _ctx.getUserObject().townId;
            queueMusic(townId + "/town", true, 3f);

            // also re-wire up our options view whenever the main view changes as the BangView
            // overrides the escape mapping during the game
            _ctx.getKeyManager().registerCommand(Keys.ESCAPE, _clearPopup);
        }

        // don't fade in the game or town views, they'll handle that themselves when they're ready
        if (view instanceof BangView || view instanceof TownView) {
            return;
        }
        _ctx.getInterface().attachChild(new FadeInOutEffect(ColorRGBA.black, 1f, 0f, 0.25f, true));
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
     * Attempts to relaunch Getdown, exiting the application after a short pause.
     *
     * @return true if Getdown was successfully relaunched, false if we were unable to do so for a
     * variety of reasons (which will have been logged).
     */
    protected static boolean relaunchGetdown (final BangContext ctx, long exitDelay)
    {
        File pro = new File(localDataDir("getdown-pro.jar"));
        if (LaunchUtil.mustMonitorChildren() || !pro.exists()) {
            return false;
        }

        File appdir = new File(localDataDir(""));
        String[] args = new String[] {
            LaunchUtil.getJVMPath(appdir), "-jar", pro.toString(), appdir.getPath()
        };

        // if we were passed a username and password on the command line, tack those on as well (we
        // have to use -Dapp.name to get Getdown to pass them back to us when we're launched)
        String uname = System.getProperty("username"), pass = System.getProperty("password");
        if (!StringUtil.isBlank(uname) && !StringUtil.isBlank(pass)) {
            args = ArrayUtil.concatenate(args, new String[] {
                "-Dapp.username=" + uname, "-Dapp.password=" + pass
            });
        }

        log.info("Running " + StringUtil.join(args, "n  "));
        try {
            Runtime.getRuntime().exec(args, null);
        } catch (IOException ioe) {
            log.warning("Failed to run getdown", ioe);
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

    /** Tracks user idleness and lets the server know when we're idle and eventually logs us
     * off. */
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
            // if this is a demo account, don't log them off for being idle
            if (_ctx.getUserObject() != null && _ctx.getUserObject().tokens.isDemo()) {
                return;
            }
            // if we're on loopplay don't log them off for being idle
            if (Boolean.parseBoolean(System.getProperty("loopplay"))) {
                return;
            }
            if (idle > LOGOFF_DELAY) {
                if (_ctx.getClient().isLoggedOn()) {
                    cancel();
                    log.info("Client idled out.");
                    _logOffMsg = "idle";
                    _ctx.getClient().logoff(false);
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
            log.info("Setting idle " + isIdle + "", "time", idleTime);
            if (_ctx.getClient().isLoggedOn()) {
                _ctx.getClient().requireService(BodyService.class).setIdle(isIdle);
            }
        }

        protected boolean _isIdle;
        protected long _lastEventStamp;
    }

    /** The context implementation. This provides access to all of the objects and services that
     * are needed by the operating client. */
    protected class BangContextImpl extends BasicContextImpl
        implements BangContext
    {
        /** Apparently the default constructor has default access, rather than protected access,
         * even though this class is declared to be protected. Why, I don't know, but we need to be
         * able to extend this class elsewhere, so we need this. */
        protected BangContextImpl () {
        }

        public Config getConfig () {
            return BangPrefs.config;
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

        public void showURL (final URL url) {
            BrowserUtil.browseURL(url, new ResultListener<Void>() {
                public void requestCompleted (Void result) {
                }
                public void requestFailed (Exception cause) {
                    String msg = MessageBundle.tcompose("m.browser_launch_failed", url);
                    getChatDirector().displayFeedback(BangAuthCodes.AUTH_MSGS, msg);
                }
            });
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
                pview.setBounds(0, 0, _ctx.getDisplay().getWidth(), _ctx.getDisplay().getHeight());
            }

            // configure the main view; fades the previous view out and fades the new view in
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

    /** Handles some location change related bits. */
    protected LocationAdapter _locationObserver = new LocationAdapter() {
        public void locationDidChange (PlaceObject place) {
            if (place instanceof SaloonObject) {
                _priorLocationIdent = "saloon";
            } else if (place instanceof ParlorObject) {
                _priorLocationIdent = "parlor";
            } else if (place instanceof OfficeObject) {
                _priorLocationIdent = "office";
            } else if (place instanceof RanchObject) {
                _priorLocationIdent = "ranch";
            } else if (place instanceof HideoutObject) {
                _priorLocationIdent = "hideout";
            } else if (place instanceof BangObject) {
                return;
            } else {
                _priorLocationIdent = null;
            }
            if (_priorLocationIdent != null) {
                _priorLocationOid = place.getOid();
            } else {
                _priorLocationIdent = "saloon";
                _priorLocationOid =
                    ((BangBootstrapData)_ctx.getClient().getBootstrapData()).saloonOid;
            }
        }

        public void locationChangeFailed (int placeId, String reason) {
            if (placeId == _priorLocationOid && "m.no_such_place".equals(reason)) {
                // if we fail to move to our prior location since it no longer exists, it was
                // likely a back parlor which got destroyed, so just head back to the saloon
                goTo(Shop.SALOON);

            } else {
                if (E_CREATE_HANDLE.equals(reason)) {
                    _headingTo = placeId;
                    CreateAvatarView.show(_ctx);
                } else if (reason.startsWith(E_SIGN_UP)) {
                    String[] bits = MessageUtil.decompose(reason);
                    String customMsg = (bits.length > 1) ? "m.account_info_" + bits[1] : null;
                    CreateAccountView.show(_ctx, customMsg, false);
                } else if (E_UNDER_13.equals(reason)) {
                    _headingTo = placeId;
                    displayPopup(new CoppaView(_ctx), true, 800);
                } else {
                    _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, reason);
                    resetTownView();
                }
            }
        }
    };

    protected GlobalKeyManager.Command _clearPopup = new GlobalKeyManager.Command() {
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

    protected SetAdapter<Notification> _nlistener = new SetAdapter<Notification>() {
        public void entryAdded (EntryAddedEvent<Notification> event) {
            if (event.getName().equals(PlayerObject.NOTIFICATIONS) &&
                canDisplayPopup(MainView.Type.NOTIFICATION)) {
                displayNotification(event.getEntry());
            }
        }
    };

    protected BangContextImpl _ctx;
    protected String _pendingTownId;

    protected BangChatDirector _chatdir;
    protected BoardCache _bcache;
    protected MuteDirector _mutedir;
    protected PlayerService _psvc;

    protected BWindow _mview;
    protected List<BWindow> _popups = Lists.newArrayList();
    protected PardnerChatView _pcview;
    protected SystemChatView _scview;
    protected StatusView _status;

    protected List<Runnable> _pendingNots = Lists.newArrayList();
    protected FKeyPopups _functionPopup;

    protected String _playingMusic;
    protected FileStream _mstream;
    protected boolean _playedIntro;
    protected boolean _viewTransition = false;
    protected String _priorLocationIdent;
    protected int _priorLocationOid;
    protected String _logOffMsg;
    protected int _headingTo = -1;

    /** We need to trigger static initialization of the StatType class before we download our
     * PlayerObject from the server which will be full of stats. */
    protected static final StatType INIT_STATS = StatType.UNUSED;

    /** The time in milliseconds after which we log off an idle user. */
    protected static final long LOGOFF_DELAY = 8L * 60L * 1000L;

    /** The default logoff message. */
    protected static final String DEFAULT_LOGOFF_MESSAGE = "logoff";

    /** Renderer prefixes that default to low detail. */
    protected static final String[] LOW_DETAIL = {
        "Intel", "GeForce4 MX", "VIA", "Radeon 7", "GeForce FX 52", "P8M"
    };

    /** Renderer prefixes that default to medium detail. */
    protected static final String[] MEDIUM_DETAIL = {
        "GeForce FX 55", "Radeon 8", "GeForce4 Ti", "RADEON XPRESS 200", "RADEON XPRESS Series"
    };
}
