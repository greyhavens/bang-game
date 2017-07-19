//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;

import com.threerings.bang.client.BangApp;
import com.threerings.jme.JmeApp;

/**
 * Sets up the necessary business for the Bang! editor.
 */
@Singleton
public class EditorApp extends JmeApp // TODO: use GDX's canvas stuffs
{
    public static String[] appArgs;

    public JFrame frame;
    public Canvas canvas;

    public Canvas getCanvas () {
        return canvas;
    }

    @Override // documentation inherited
    public void create ()
    {
        super.create();

        // two-pass transparency is expensive
        _ctx.getRenderer().getQueue().setTwoPassTransparency(false);

        // // queue an update to make sure that the context is current before the client's event
        // // handlers start firing.  somehow calling repaint() doesn't have the same effect.
        // postRunnable(new Runnable() {
        //     public void run () {
        //         _canvas.update(_canvas.getGraphics());
        //     }
        // });

        // initialize and start our client instance
        _client.init(this, frame);
        _client.start();

        System.out.println("EditorApp created!");
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

    @Inject protected EditorClient _client;
}
