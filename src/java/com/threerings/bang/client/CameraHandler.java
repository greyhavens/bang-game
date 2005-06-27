//
// $Id$

package com.threerings.bang.client;

import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.threerings.jme.input.GodViewHandler;

import static com.threerings.bang.Log.log;

/**
 * Provides camera handling.
 */
public class CameraHandler extends GodViewHandler
{
    public CameraHandler (Camera cam, String api)
    {
        super(cam, api);

        // set up limits on zooming and panning
        setZoomLimits(50f, 200f);
        setPanLimits(-50, -50, 250, 250);

        // set up the camera
        Vector3f loc = new Vector3f(50, -100, 200);
        cam.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/4, cam.getLeft());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());
        cam.update();
    }

    /**
     * Configures the camera according to the supplied board dimensions.
     */
    public void setBoardDimens (float breadth, float depth)
    {
        float height = Math.max(breadth, depth);
        float angle = FastMath.PI/3; // 60 degree angle view
        float recede = height / FastMath.tan(angle);

        // position the camera
        float cx = breadth/2, cy = depth/2;
        Vector3f pos = new Vector3f(cx, cy-recede, height);
        _camera.setLocation(pos);
        log.info("Board " + breadth + "x" + depth + ", position " + pos + ".");

        // orient the camera
        Vector3f left = new Vector3f(-1, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f forward = new Vector3f(0, 0, -1);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(angle-FastMath.PI/2, left);
        rotm.mult(forward, forward);
        rotm.mult(left, left);
        rotm.mult(up, up);

        // TODO: compute a rotation based on which player we are

        _camera.setDirection(forward);
        _camera.setLeft(left);
        _camera.setUp(up);
        _camera.update();

        // set the pan limits based on the board size
        setPanLimits(-50, -50, breadth+50, depth+50);
    }

//     protected void addPanActions (Camera cam)
//     {
//         // no panning
//     }

    protected void addRollActions (Camera cam)
    {
        // no rolling
    }
}
