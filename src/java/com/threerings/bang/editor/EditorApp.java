//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;

import com.threerings.jme.JmeCanvasApp;

import com.threerings.bang.client.BangApp;

import static com.threerings.bang.Log.log;

/**
 * Sets up the necessary business for the Bang! editor.
 */
public class EditorApp extends JmeCanvasApp
{
    public static String[] appArgs;

    public static void main (String[] args)
    {
        // configure our debug log
        BangApp.configureLog("editor.log");

        // save these for later
        appArgs = args;

        // create our editor server which we're going to run in the same JVM with the client
        Injector injector = Guice.createInjector(new EditorServer.Module());
        EditorServer server = injector.getInstance(EditorServer.class);
        try {
            server.init(injector);
        } catch (Exception e) {
            log.warning("Unable to initialize server.", e);
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
            // two-pass transparency is expensive
            _ctx.getRenderer().getQueue().setTwoPassTransparency(false);
        
            // queue an update to make sure that the context is current
            // before the client's event handlers start firing.  somehow
            // calling repaint() doesn't have the same effect.
            postRunnable(new Runnable() {
                public void run () {
                    _canvas.update(_canvas.getGraphics());
                }
            });
            
            // create and initialize our client instance
            _client = new EditorClient(this, _frame);

            // start up the client
            _client.start();

            return true;
        }
        return false;
    }

    public void create ()
    {
        // create a frame
        _frame = new JFrame("Bang Editor");
        _frame.setSize(new Dimension(1224, 768));
        _frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // display the GL canvas to start so that it initializes everything
        _frame.getContentPane().add(_canvas, BorderLayout.CENTER);
        _frame.setVisible(true);
    }

    protected EditorApp ()
    {
        super(1024, 768);
    }

    @Override // documentation inherited
    protected void initRoot ()
    {
        super.initRoot();

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
        // handle lights in board view
    }

    protected JFrame _frame;
    protected EditorClient _client;
}
