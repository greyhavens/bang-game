//
// $Id$

package com.threerings.bang.client;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.threerings.jme.input.GodViewHandler;

/**
 * Provides camera handling.
 */
public class CameraHandler extends GodViewHandler
{
    public CameraHandler (Camera cam, String api)
    {
        super(cam, api);

        // set up limits on zooming and panning
        setZoomLimits(50f, 250f);
        setPanLimits(-50, -50, 250, 250);

        // set up the camera
        Vector3f loc = new Vector3f(80, 40, 200);
        cam.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/15, cam.getLeft());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());
        cam.update();
    }
}
