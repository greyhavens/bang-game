//
// $Id$

package com.threerings.bang.game.client;

import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.threerings.jme.input.GodViewHandler;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Provides camera handling.
 */
public class CameraHandler extends GodViewHandler
{
    public CameraHandler (Camera cam, String api)
    {
        super(cam, api);

        // set up limits on zooming and panning
        setZoomLimits(50f, 150f);
        setPanLimits(-50, -50, 250, 250);
        setRotateVelocity(2*FastMath.PI);

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
    public void setBoardDimens (BangObject bangobj, int pidx)
    {
        float breadth = TILE_SIZE * bangobj.board.getWidth();
        float depth = TILE_SIZE * bangobj.board.getHeight();

        float height = 100; // default to 100 units up in the air
        float angle = FastMath.PI/3; // default to 60 degree view angle

        // find the center of our units
        Piece[] pieces = bangobj.getPieceArray();
        int pcount = 0, x = 0, y = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii].owner == pidx) {
                pcount++;
                x += pieces[ii].x;
                y += pieces[ii].y;
            }
        }

        // if there are no pieces, just choose an arbitrary center point
        if (pcount == 0) {
            x = y = 5;
            pcount = 1;
        }

        // orient the camera
        Vector3f left = new Vector3f(-1, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f forward = new Vector3f(0, 0, -1);
        Matrix3f rotm = new Matrix3f();

        // determine from which corner we want to view the board based on
        // the player's starting location
        float cx = TILE_SIZE * x / pcount, cy = TILE_SIZE * y / pcount;
        float rotangle;
        if (cx > breadth/2) {
            rotangle = (cy > depth/2) ? -3*FastMath.PI/4 : -FastMath.PI/4;
        } else {
            rotangle = (cy > depth/2) ? 3*FastMath.PI/4 : FastMath.PI/4;
        }

        log.info("Camera [cx=" + cx + ", cy=" + cy +
                 ", rot=" + (rotangle/FastMath.PI) + "].");

        // the camera starts out looking straight down at the board, so we
        // rotate it around the forward axis to the desired starting angle
        rotm.fromAngleAxis(rotangle, forward);
        rotm.mult(forward, forward);
        rotm.mult(left, left);
        rotm.mult(up, up);

        // recompute the panning vectors
        _rxdir.set(1, 0, 0);
        _rydir.set(0, 1, 0);
        rotm.mult(_rxdir, _rxdir);
        rotm.mult(_rydir, _rydir);

        // next we position the camera back a bit from the point of focus
        // so that when we roll it up, said point will be in the center of
        // the screen
        Vector3f pos = new Vector3f(cx, cy, height);
        pos.add(up.mult(-1 * FastMath.tan(FastMath.PI/2-angle) * height), pos);
        _camera.setLocation(pos);

        // finally we roll it up to our desired elevation angle
        rotm.fromAngleAxis(angle-FastMath.PI/2, left);
        rotm.mult(forward, forward);
        rotm.mult(left, left);
        rotm.mult(up, up);

        _camera.setDirection(forward);
        _camera.setLeft(left);
        _camera.setUp(up);
        _camera.update();

        // set the pan limits based on the board size
        setPanLimits(-50, -50, breadth+50, depth+50);
    }

    protected void setKeyBindings (String api)
    {
        super.setKeyBindings(api);

        // additional key bindings for the zoom actions
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();
        keyboard.set("zoomIn", KeyInput.KEY_E);
        keyboard.set("zoomOut", KeyInput.KEY_Q);
    }

    protected void addRollActions (Camera cam)
    {
        // no rolling
    }
}
