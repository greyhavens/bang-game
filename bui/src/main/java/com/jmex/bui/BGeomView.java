//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import org.lwjgl.opengl.GL11;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.system.DisplaySystem;

import com.jmex.bui.util.Insets;
import com.jmex.bui.util.Rectangle;

/**
 * Displays 3D geometry (a {@link Spatial}) inside a normal user interface.
 */
public class BGeomView extends BComponent
{
    /**
     * Creates a view with no configured geometry. Geometry can be set later with {@link
     * #setGeometry}.
     */
    public BGeomView ()
    {
        this(null);
    }

    /**
     * Creates a view with the specified {@link Spatial} to be rendered.
     */
    public BGeomView (Spatial geom)
    {
        _geom = geom;
    }

    /**
     * Returns the camera used when rendering our geometry.
     */
    public Camera getCamera ()
    {
        if (_camera == null) {
            _camera = createCamera(DisplaySystem.getDisplaySystem());
        }
        return _camera;
    }

    /**
     * Configures the spatial to be rendered by this view.
     */
    public void setGeometry (Spatial geom)
    {
        _geom = geom;
    }

    /**
     * Returns the geometry rendered by this view.
     */
    public Spatial getGeometry()
    {
    	return _geom;
    }

    /**
     * Called every frame (while we're added to the view hierarchy) by the {@link BRootNode}.
     */
    public void update (float frameTime)
    {
        if (_geom != null) {
            _geom.updateGeometricState(frameTime, true);
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _root = getWindow().getRootNode();
        _root.registerGeomView(this);
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _root.unregisterGeomView(this);
        _root = null;
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        if (_geom == null) {
            return;
        }

        applyDefaultStates();
        Camera cam = renderer.getCamera();
        boolean useOrtho = (_geom.getRenderQueueMode() == Renderer.QUEUE_ORTHO);
        try {
            if (!useOrtho) {
                renderer.unsetOrtho();

                // create or resize our camera if necessary
                DisplaySystem display = DisplaySystem.getDisplaySystem();
                int swidth = display.getWidth(), sheight = display.getHeight();
                boolean updateDisplay = false;
                if (_camera == null || _swidth != swidth || _sheight != sheight) {
                    _swidth = swidth;
                    _sheight = sheight;
                    if (_camera == null) {
                        _camera = createCamera(display);
                    } else {
                        _camera.resize(_swidth, _sheight);
                    }
                    updateDisplay = true;
                }

                // set up our camera viewport if it has changed
                Insets insets = getInsets();
                int ax = getAbsoluteX() + insets.left, ay = getAbsoluteY() + insets.bottom;
                int width = _width - insets.getHorizontal(), height = _height - insets.getVertical();
                if (updateDisplay || _cx != ax || _cy != ay ||
                    _cwidth != width || _cheight != height) {
                    _cx = ax;
                    _cy = ay;
                    _cwidth = width;
                    _cheight = height;
                    float left = _cx / _swidth, right = left + _cwidth / _swidth;
                    float bottom = _cy / _sheight;
                    float top = bottom + _cheight / _sheight;
                    _camera.setViewPort(left, right, bottom, top);
                    _camera.setFrustumPerspective(45.0f, _width / (float)_height, 1, 1000);
                }

                // clear the z buffer over the area to which we will be drawing
                boolean scissored = intersectScissorBox(_srect, ax, ay, width, height);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                restoreScissorState(scissored, _srect);

                // now set up the custom camera and render our geometry
                renderer.setCamera(_camera);
                _camera.update();
            }

            // actually render the geometry
            renderer.draw(_geom);

        } finally {
            if (!useOrtho) {
                // restore the camera
                renderer.setCamera(cam);
                cam.update();
                renderer.setOrtho();

                // we need to restore the GL translation as that got wiped out when we left and
                // re-entered ortho mode
                GL11.glTranslatef(getAbsoluteX(), getAbsoluteY(), 0);
            }
        }
    }

    /**
     * Called to create and configure the camera that we'll use when rendering our geometry.
     */
    protected Camera createCamera (DisplaySystem ds)
    {
        // create a standard camera and frustum
        Camera camera = ds.getRenderer().createCamera(_swidth, _sheight);
        camera.setParallelProjection(false);

        // put and point it somewhere sensible by default
        Vector3f loc = new Vector3f(0.0f, 0.0f, 25.0f);
        Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f dir = new Vector3f(0.0f, 0f, -1.0f);
        camera.setFrame(loc, left, up, dir);

        return camera;
    }

    protected BRootNode _root;
    protected Camera _camera;
    protected Spatial _geom;
    protected int _swidth, _sheight;
    protected float _cx, _cy, _cwidth, _cheight;

    protected Rectangle _srect = new Rectangle();
}
