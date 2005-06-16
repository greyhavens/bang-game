//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JFrame;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.util.LoggingSystem;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.jme.JmeCanvasApp;
import com.threerings.util.Name;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Sets up the necessary business for the Bang! editor.
 */
public class EditorApp extends JmeCanvasApp
{
    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        LoggingSystem.getLogger().setLevel(Level.WARNING);

        // create our editor server which we're going to run in the same
        // JVM with the client
        EditorServer server = new EditorServer();
        try {
            server.init();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server.", e);
        }

        // let the BangClientController know we're in editor mode
        System.setProperty("editor", "true");

        // this is the entry point for all the "client-side" stuff
        EditorApp app = new EditorApp();
        app.create();
        app.run();
    }

    @Override // documentation inherited
    public boolean init ()
    {
        if (super.init()) {
            // post a runnable that will get executed after everything is
            // initialized and happy
            EditorServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    _client.logon();
                }
            });
            return true;
        }
        return false;
    }

    public void create ()
    {
        // create a frame
        _frame = new JFrame("Bang Editor");
        _frame.setSize(new Dimension(800, 600));
        _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // create and initialize our client instance
        _client = new EditorClient(this, _frame);

        // display the GL canvas to start so that it initializes everything
        _frame.getContentPane().add(_canvas, BorderLayout.CENTER);
        _frame.setVisible(true);
    }

    protected EditorApp ()
    {
        super(800, 600);
    }

    @Override // documentation inherited
    protected void readDisplayConfig ()
    {
        BangPrefs.configureDisplayMode(_properties);
    }

    @Override // documentation inherited
    protected void initRoot ()
    {
        super.initRoot();

        // create a bunch of standard rendering stuff
        RenderUtil.init(_client.getContext());

        // set up the camera
        Vector3f loc = new Vector3f(80, 40, 200);
        _camera.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/15, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        _camera.update();
    }

    @Override // documentation inherited
    protected void initLighting ()
    {
        _geom.setRenderState(
            _lights = RenderUtil.createDaylight(_client.getContext()));
    }

    protected JFrame _frame;
    protected EditorClient _client;
}
