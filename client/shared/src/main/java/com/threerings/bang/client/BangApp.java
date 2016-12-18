//
// $Id$

package com.threerings.bang.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.util.LoggingSystem;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BRootNode;
import com.jmex.bui.PolledRootNode;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.TextEvent;

import com.samskivert.util.FormatterUtil;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RecentList;
import com.samskivert.util.RepeatRecordFilter;
import com.samskivert.util.StringUtil;
import com.samskivert.util.SystemInfo;

import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.CameraHandler;

import com.threerings.bang.game.client.GameCameraHandler;
import com.threerings.bang.game.client.GameInputHandler;

import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Provides the main entry point for the Bang! client.
 */
public class BangApp extends JmeApp
{
    /** Keep the last 50 formatted log records in memory. */
    public static RecentList recentLog = new RecentList(50);

    /**
     * Redirects the output of the application to the specified log file and configures our various
     * logging systems.
     */
    public static void configureLog (String logfile)
    {
        // potentially redirect stdout and stderr to a log file
        File nlog = null;
        if (System.getProperty("no_log_redir") == null) {
            // first delete any previous previous log file
            File olog = new File(BangClient.localDataDir("old-" + logfile));
            if (olog.exists()) {
                olog.delete();
            }

            // next rename the previous log file
            nlog = new File(BangClient.localDataDir(logfile));
            if (nlog.exists()) {
                nlog.renameTo(olog);
            }

            // and now redirect our output
            try {
                PrintStream logOut = new PrintStream(new FileOutputStream(nlog), true);
                System.setOut(logOut);
                System.setErr(logOut);

            } catch (IOException ioe) {
                log.warning("Failed to open debug log", "path", nlog, "error", ioe);
            }
        }

        // turn off JME's verbose logging
        LoggingSystem.getLogger().setLevel(Level.WARNING);

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter formatter = new OneLineLogFormatter(false) {
            public String format (LogRecord record) {
                String output = super.format(record);
                recentLog.add(output);
                return output;
            }
        };
        FormatterUtil.configureDefaultHandler(formatter);
        RepeatRecordFilter.configureDefaultHandler(100);

        // if we've redirected our log output, note where to
        if (nlog != null) {
            log.info("Logging to '" + nlog + "'.");
        }
    }

        // // default to connecting to the frontier town server
        // String username = (args.length > 0) ? args[0] : System.getProperty("username");
        // String password = (args.length > 1) ? args[1] : System.getProperty("password");
        // String townId = (args.length > 2) ? args[2] : BangCodes.FRONTIER_TOWN;
        // String server = DeploymentConfig.getServerHost(townId);
        // int[] ports = DeploymentConfig.getServerPorts(townId);
        // BangApp app = new BangApp();
        // if (app.init()) {
        //     app.run(server, ports, username, password);
        // }

    @Override // documentation inherited
    public void create ()
    {
        // configure our debug log
        configureLog("bang.log");

        // set up our application icons
        BangUI.configIcons();

        try {
            checkJavaVersion();
        } catch (Throwable t) {
            reportInitFailure(t);
            return;
        }

        super.create();

        // two-pass transparency is expensive
        _ctx.getRenderer().getQueue().setTwoPassTransparency(false);

        // // turn on the FPS display if we're profiling
        // if (_profiling) {
        //     displayStatistics(true);
        // }

        // initialize our client instance
        _client = new BangClient();
        _client.init(this, false);

        // speed up key input
        _input.setActionSpeed(150f);
    }

    public void run (String server, int[] ports, String username, String password)
    {
        Client client = _client.getContext().getClient();

        // configure our server, port and client version
        log.info("Using", "server", server, "ports", StringUtil.toString(ports),
                 "version", DeploymentConfig.getVersion());
        client.setServer(server, ports);
        client.setVersion(String.valueOf(DeploymentConfig.getVersion()));

        // configure the client with credentials if they were supplied
        if (username != null && password != null) {
            client.setCredentials(_client.createCredentials(new Name(username), password));
        }

        // now start up the main event loop
        // run();
    }

    @Override // documentation inherited
    public void stop ()
    {
        if (_client.shouldStop()) {
            super.stop();
        }
    }

    // @Override // documentation inherited
    // protected DisplaySystem createDisplay ()
    //     throws JmeException
    // {
    //     PropertiesIO props = new PropertiesIO(getConfigPath("jme.cfg"));
    //     BangPrefs.configureDisplayMode(props, Boolean.getBoolean("safemode"));
    //     _api = props.getRenderer();
    //     DisplaySystem display = DisplaySystem.getDisplaySystem(_api);
    //     display.setVSyncEnabled(!_profiling);
    //     display.createWindow(props.getWidth(), props.getHeight(), props.getDepth(), props.getFreq(),
    //                          props.getFullscreen());
    //     return display;
    // }

    @Override // documentation inherited
    protected CameraHandler createCameraHandler (Camera camera)
    {
        return new GameCameraHandler(camera);
    }

    @Override // documentation inherited
    protected InputHandler createInputHandler (CameraHandler camhand)
    {
        return new GameInputHandler(camhand);
    }

    @Override // documentation inherited
    protected BRootNode createRootNode ()
    {
        return new PolledRootNode(_timer, _input) {
            protected boolean dispatchEvent (BComponent target, BEvent event) {
                boolean dispatched = super.dispatchEvent(target, event);
                if (event instanceof ActionEvent && target instanceof BButton &&
                    !(target instanceof SelectableIcon)) {
                    // allow buttons to have special feedback sounds
                    BangUI.FeedbackSound sound = (BangUI.FeedbackSound)
                        target.getProperty("feedback_sound");
                    if (sound == null) {
                        sound = BangUI.FeedbackSound.BUTTON_PRESS;
                    }
                    BangUI.play(sound);
                } else if (event instanceof TextEvent) {
                    BangUI.play(BangUI.FeedbackSound.KEY_TYPED);
                }
                return dispatched;
            }
        };
    }

    @Override // documentation inherited
    protected void initLighting ()
    {
        // handle lights in board view
    }

    /**
     * Checks that we are running on at least the 1.5.0_06 JVM.
     *
     * @throws Exception if the version is invalid
     */
    protected void checkJavaVersion ()
        throws Exception
    {
        SystemInfo sysinfo = new SystemInfo();
        int[] minVersion = new int[] {1, 5, 0, 6};
        String[] jVersion = sysinfo.javaVersion.split("[._-]");
        String errmsg = "You are running java version " + sysinfo.javaVersion +
            ", but we require at least version 1.5.0_06";
        for (int ii = 0, ll = Math.min(minVersion.length, jVersion.length); ii < ll; ii++) {
            int diff;
            try {
                diff = minVersion[ii] - Integer.valueOf(jVersion[ii]);
            } catch (NumberFormatException e) {
                diff = 1;
            }
            if (diff < 0) {
                return;
            } else if (diff > 0) {
                throw new Exception(errmsg);
            }
        }
        if (jVersion.length != minVersion.length) {
            throw new Exception(errmsg);
        }
    }

    protected void reportInitFailure (Throwable t)
    {
        reportInitFailure(_client, t);
    }

    @Override // documentation inherited
    protected void update (long frameTick)
    {
        super.update(frameTick);
        _client._soundmgr.updateStreams(_frameTime);
    }

    @Override // documentation inherited
    protected void cleanup ()
    {
        super.cleanup();

        // let the client clean things up before we shutdown
        _client.willExit();

        // log off before we shutdown
        Client client = _client.getContext().getClient();
        if (client.isLoggedOn()) {
            client.logoff(false);
        }
    }

    /**
     * Pops up a dialog telling the user that we were wholly unable to start up the client and
     * giving them some indication of their meager options.
     */
    protected static void reportInitFailure (BangClient client, Throwable t)
    {
        log.warning("JME initalization failed.", t);

        // if we don't have a client yet, create a bare bones client that we
        // can use to get our context
        MessageManager msgmgr = (client == null) ?
            new MessageManager(BangClient.MESSAGE_MANAGER_PREFIX) :
            client.getContext().getMessageManager();
        InitFailedDialog ifd = new InitFailedDialog(msgmgr, t);
        ifd.pack();
        ifd.setVisible(true);
    }

    /** The main thing! */
    protected BangClient _client;

    /** Used to configure the renderer appropriately when profiling. */
    protected boolean _profiling = "true".equalsIgnoreCase(System.getProperty("profiling"));
}
