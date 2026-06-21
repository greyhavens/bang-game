//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import javax.swing.JFrame;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import com.threerings.bang.client.BangApp;
import com.threerings.jme.JmeApp;

/**
 * Sets up the necessary business for the Bang! editor.
 */
@Singleton
public class EditorApp extends JmeApp
{
    public static String[] appArgs;

    public JFrame frame;
    public Canvas canvas;

    public Canvas getCanvas () {
        return canvas;
    }

    // jME3 cutover (Phase 7b — editor canvas embedding): called by the SimpleApplication host from
    // simpleInitApp, on the jME3 render thread, once startCanvas() spins the GL loop. By now
    // EditorDesktop has built the Swing JFrame and the embedded AWT canvas and published both to
    // this app (frame/canvas), so EditorClient.init installs the menu bar + status bar into the
    // real (non-null) frame, and EditorPanel.willEnterPlace sizes the BUI board view to the canvas.
    @Override
    public void create ()
    {
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

    // jME3 cutover: JmeApp no longer has an initLighting() hook (the fork set up a global
    // LightState here); the editor handles lights in the board view. Kept as a plain method.
    protected void initLighting ()
    {
        // handle lights in board view
    }

    @Inject protected EditorClient _client;
}
