//
// $Id$

package com.threerings.bang.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.util.LoggingSystem;

import com.samskivert.util.OneLineLogFormatter;
import com.samskivert.util.LoggingLogProvider;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.jme.JmeApp;
import com.threerings.jme.input.GodViewHandler;

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
        app.init();
        app.run(server, port, username, password);
    }

    // documentation inherited
    public void init ()
    {
        super.init();

        // initialize our client instance
        _client = new BangClient();
        _client.init(this);

        // set up our minimum and maximum zoom
        GodViewHandler ih = (GodViewHandler)_input;
        ih.setZoomLimits(50f, 200f);

        // set up the camera
        Vector3f loc = new Vector3f(80, 40, 150);
        _camera.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/15, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        _camera.update();

        // speed up key input
        _input.setKeySpeed(150f);
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
                new UsernamePasswordCreds(new Name(username), password));
            client.logon();
        }

        // now start up the main event loop
        run();
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
