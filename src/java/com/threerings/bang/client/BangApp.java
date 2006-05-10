//
// $Id$

package com.threerings.bang.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JOptionPane;

import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.system.DisplaySystem;
import com.jme.system.PropertiesIO;
import com.jme.util.LoggingSystem;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BRootNode;
import com.jmex.bui.PolledRootNode;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.TextEvent;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.RecentList;
import com.samskivert.util.RepeatRecordFilter;

import com.threerings.presents.client.Client;
import com.threerings.util.Name;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.CameraHandler;

import com.threerings.bang.game.client.GameCameraHandler;
import com.threerings.bang.game.client.GameInputHandler;

import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Provides the main entry point for the Bang! client.
 */
public class BangApp extends JmeApp
{
    /** Keep the last 50 formatted log records in memory. */
    public static RecentList recentLog = new RecentList(50);

    public static void configureLog (String file)
    {
        // we do this all in a strange order to avoid logging anything
        // unti we set up our log formatter but we can't do that until
        // after we've redirected system out and err
        String dlog = null;
        if (System.getProperty("no_log_redir") == null) {
            dlog = BangClient.localDataDir(file);
            try {
                PrintStream logOut = new PrintStream(
                    new FileOutputStream(dlog), true);
                System.setOut(logOut);
                System.setErr(logOut);

            } catch (IOException ioe) {
                log.warning("Failed to open debug log [path=" + dlog +
                            ", error=" + ioe + "].");
                dlog = null;
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
        OneLineLogFormatter.configureDefaultHandler(formatter);
        RepeatRecordFilter.configureDefaultHandler(100);
    }

    public static void main (String[] args)
    {
        // configure our debug log
        configureLog("bang.log");

        String server = DeploymentConfig.getServerHost();
        if (args.length > 0) {
            server = args[0];
        }

        int port = DeploymentConfig.getServerPort();
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                log.warning("Invalid port specification '" + args[1] + "'.");
            }
        }

        String username = (args.length > 2) ? args[2] : null;
        String password = (args.length > 3) ? args[3] : null;

        BangApp app = new BangApp();
        if (app.init()) {
            app.run(server, port, username, password);
        } else {
            System.exit(-1);
        }
    }

    @Override // documentation inherited
    public boolean init ()
    {
        if (!super.init()) {
            return false;
        }

        // two-pass transparency is expensive
        _ctx.getRenderer().getQueue().setTwoPassTransparency(false);
        
        // turn on the FPS display if we're profiling
        if (_profiling) {
            displayStatistics(true);
        }

        // initialize our client instance
        _client = new BangClient();
        _client.init(this);

        // speed up key input
        _input.setActionSpeed(150f);
        return true;
    }

    public void run (String server, int port, String username, String password)
    {
        Client client = _client.getContext().getClient();

        // configure our server, port and client version
        log.info("Using [server=" + server + ", port=" + port +
                 ", version=" + DeploymentConfig.getVersion() + "].");
        client.setServer(server, port);
        client.setVersion(String.valueOf(DeploymentConfig.getVersion()));

        // configure the client with credentials if they were supplied
        if (username != null && password != null) {
            client.setCredentials(
                _client.createCredentials(new Name(username), password));
        }

        // now start up the main event loop
        run();
    }

    @Override // documentation inherited
    protected DisplaySystem createDisplay ()
    {
        PropertiesIO props = new PropertiesIO(getConfigPath("jme.cfg"));
        BangPrefs.configureDisplayMode(props);
        _api = props.getRenderer();
        DisplaySystem display = DisplaySystem.getDisplaySystem(_api);
        display.setVSyncEnabled(!_profiling);
        display.createWindow(props.getWidth(), props.getHeight(),
                             props.getDepth(), props.getFreq(),
                             props.getFullscreen());
        return display;
    }

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
            protected void dispatchEvent (BComponent target, BEvent event) {
                super.dispatchEvent(target, event);
                if (event instanceof ActionEvent && target instanceof BButton &&
                    !(target instanceof SelectableIcon)) {
                    BangUI.play(BangUI.FeedbackSound.BUTTON_PRESS);
                } else if (event instanceof TextEvent) {
                    BangUI.play(BangUI.FeedbackSound.KEY_TYPED);
                }
            }
        };
    }

    @Override // documentation inherited
    protected void initLighting ()
    {
        // handle lights in board view
    }

    @Override // documentation inherited
    protected void reportInitFailure (Throwable t)
    {
        t.printStackTrace(System.err);
        JOptionPane.showMessageDialog(null, "Initialization failed: " + t);
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

    /** The main thing! */
    protected BangClient _client;

    /** Used to configure the renderer appropriately when profiling. */
    protected boolean _profiling =
        "true".equalsIgnoreCase(System.getProperty("profiling"));
}
