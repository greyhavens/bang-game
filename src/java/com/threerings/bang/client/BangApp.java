//
// $Id$

package com.threerings.bang.client;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Box;
import com.jme.util.LoggingSystem;

import com.threerings.jme.JmeApp;
import com.threerings.util.Name;
import com.threerings.presents.client.Client;
import com.threerings.presents.net.UsernamePasswordCreds;

import static com.threerings.bang.Log.log;

/**
 * Provides the main entry point for the Bang! client.
 */
public class BangApp extends JmeApp
{
    // documentation inherited
    public void init ()
    {
        super.init();

        // initialize our client instance
        _client = new BangClient();
        _client.init(this);

        // add some simple geometry for kicks
        Vector3f max = new Vector3f(15, 15, 15);
        Vector3f min = new Vector3f(5, 5, 5);

        Box t = new Box("Box", min, max);
        t.setModelBound(new BoundingBox());
        t.updateModelBound();
        t.setLocalTranslation(new Vector3f(0, 0, -15));
        ColorRGBA[] colors = new ColorRGBA[24];
        for (int i = 0; i < 24; i++) {
            colors[i] = ColorRGBA.randomColor();
        }
        t.setColors(colors);
        _root.attachChild(t);
        _root.updateRenderState();

        // set up the camera
        Vector3f loc = new Vector3f(0, -200, 200);
        _camera.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/5, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        _camera.update();

        // speed up key input
        _input.setKeySpeed(100f);
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

    // documentation inherited
    public void stop ()
    {
        // log off before we shutdown
        Client client = _client.getContext().getClient();
        if (client.isLoggedOn()) {
            client.logoff(false);
        }
        log.info("Stopping.");
        super.stop();
    }

    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(java.util.logging.Level.OFF);

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

    protected BangClient _client;
}
