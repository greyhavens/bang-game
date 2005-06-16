//
// $Id$

package com.threerings.bang.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JOptionPane;
import java.util.logging.Level;

import com.jme.input.InputHandler;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.util.LoggingSystem;

import com.samskivert.servlet.user.Password;
import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.bang.util.RenderUtil;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.jme.JmeApp;

import static com.threerings.bang.Log.log;

/**
 * Provides the main entry point for the Bang! client.
 */
public class BangApp extends JmeApp
{
    public static void main (String[] args)
    {
        // we do this all in a strange order to avoid logging anything
        // unti we set up our log formatter but we can't do that until
        // after we've redirected system out and err
        String dlog = null;
        if (System.getProperty("no_log_redir") == null) {
            dlog = BangClient.localDataDir("bang.log");
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

        LoggingSystem.getLogger().setLevel(Level.WARNING);

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        String server = "localhost";
        if (args.length > 0) {
            server = args[0];
        }

        int port = Client.DEFAULT_SERVER_PORT;
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

        // initialize our client instance
        _client = new BangClient();
        _client.init(this);

        // speed up key input
        _input.setKeySpeed(150f);
        return true;
    }

    public void run (String server, int port, String username, String password)
    {
        Client client = _client.getContext().getClient();

        // pass them on to the client
        log.info("Using [server=" + server + ", port=" + port + "].");
        client.setServer(server, port);

        // configure the client with some credentials and logon
        if (username != null && password != null) {
            // create and set our credentials
            client.setCredentials(
                new UsernamePasswordCreds(
                    new Name(username),
                    Password.makeFromClear(password).getEncrypted()));
            client.logon();
        }

        // now start up the main event loop
        run();
    }

    @Override // documentation inherited
    protected void readDisplayConfig ()
    {
        BangPrefs.configureDisplayMode(_properties);
    }

    @Override // documentation inherited
    protected InputHandler createInputHandler (Camera camera, String api)
    {
        return new CameraHandler(camera, api);
    }

    @Override // documentation inherited
    protected void initLighting ()
    {
        _geom.setRenderState(
            _lights = RenderUtil.createDaylight(_client.getContext()));
    }

    @Override // documentation inherited
    protected void reportInitFailure (Throwable t)
    {
        JOptionPane.showMessageDialog(null, "Initialization failed: " + t);
    }

    protected void cleanup ()
    {
        super.cleanup();

        // log off before we shutdown
        Client client = _client.getContext().getClient();
        if (client.isLoggedOn()) {
            client.logoff(false);
        }
    }

    protected BangClient _client;
}
