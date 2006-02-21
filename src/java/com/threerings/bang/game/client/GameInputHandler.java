
//
// $Id$

package com.threerings.bang.game.client;

import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.KeyListener;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseWheelListener;

import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.camera.GodViewHandler;
import com.threerings.jme.camera.PanPath;
import com.threerings.jme.camera.SwingPath;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Provides camera handling.
 */
public class GameInputHandler extends GodViewHandler
{
    public GameInputHandler (CameraHandler camhand)
    {
        super(camhand);
    }

    /**
     * Configures the camera at the start of the game.
     */
    public void prepareForRound (BangView view, BangObject bangobj, int pidx)
    {
        // listen for mouse wheel events
        view.view.addListener(_swingListener);

        // set up the starting zoom index
        _camidx = 0;

//         // set the pan limits based on the board size
//         _camhand.setPanLimits(
//             0, 0,
//             TILE_SIZE * bangobj.board.getWidth(),
//             TILE_SIZE * bangobj.board.getHeight(), true);

        // reset the camera's orientation
        _camhand.resetAxes();
        
        // start the camera in the center of the board, pointing straight
        // down
        float cx = TILE_SIZE * bangobj.board.getWidth() / 2;
        float cy = TILE_SIZE * bangobj.board.getHeight() / 2;
        float height = CAMERA_ZOOMS[0];
        _camhand.setLocation(new Vector3f(cx, cy, height));

        // rotate the camera by 45 degrees and orient it properly
        _camhand.orbitCamera(FastMath.PI/4);
    }

    public void endRound (BangView view)
    {
        // stop listening for mouse wheel events
        view.view.removeListener(_swingListener);
    }

    /**
     * Swings the camera smoothly around the point on the ground at which it is
     * "looking", by the requested angle (in radians).
     */
    public void swingCamera (float deltaAngle)
    {
        if (_camhand.cameraIsMoving()) {
            log.info("Already rotating, no swing: " + deltaAngle + ".");
            return;
        }

        float angvel = 2*FastMath.PI;
        _camhand.moveCamera(
            new SwingPath(_camhand, _camhand.getGroundPoint(),
                          _camhand.getGroundNormal(), deltaAngle, angvel, 0));
    }

    /**
     * Moves the camera to the next elevation angle. The camera will smoothly
     * roll to the appropriate angle rather than changing abruptly.
     */
    public void rollCamera ()
    {
        if (_camhand.cameraIsMoving()) {
            return;
        }
        rollCamera((_camidx + 1) % CAMERA_ANGLES.length, 2*FastMath.PI);
    }

    /**
     * Pans the camera to a point where the specified location is basically in
     * the center of the view. The specified location's x and y coordinate will
     * be used but the z coordinate will be assumed to be on the ground.
     */
    public void aimCamera (Vector3f location)
    {
        // compute the distance from the camera to the ground plane along the
        // camera's direction vector
        Vector3f gnorm = _camhand.getGroundNormal();
        Vector3f camloc = _camhand.getCamera().getLocation();
        Vector3f camdir = _camhand.getCamera().getDirection();
        float camdist = -1f * gnorm.dot(camloc) / gnorm.dot(camdir);

        // slide our target location backwards along the direction vector by
        // the same distance and we'll have our desired camera location
        Vector3f spot = new Vector3f(location.x, location.y, 0);
        spot.scaleAdd(-camdist, camdir, spot);

        _camhand.moveCamera(new PanPath(_camhand, spot, 0.25f));
    }

    protected void rollCamera (int nextidx, float angvel)
    {
        float curang = CAMERA_ANGLES[_camidx], curzoom = CAMERA_ZOOMS[_camidx];
        float nextang = CAMERA_ANGLES[nextidx],
            nextzoom = CAMERA_ZOOMS[nextidx];
        float deltaAngle = nextang-curang, deltaZoom = nextzoom-curzoom;
        _camidx = nextidx;
        _camhand.moveCamera(
            new SwingPath(_camhand, _camhand.getGroundPoint(),
                          _camhand.getCamera().getLeft(), deltaAngle,
                          angvel, deltaZoom));
    }

    protected void setKeyBindings ()
    {
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();

        // we only allow free form panning, nothing else
        keyboard.set("forward", KeyInput.KEY_W);
        keyboard.set("backward", KeyInput.KEY_S);
        keyboard.set("left", KeyInput.KEY_A);
        keyboard.set("right", KeyInput.KEY_D);
    }

    protected MouseWheelListener _swingListener = new MouseWheelListener() {
        public void mouseWheeled (MouseEvent e) {
            swingCamera((e.getDelta() > 0) ? -FastMath.PI/2 : FastMath.PI/2);
        }
    };

    protected int _camidx = -1;

    protected final static float[] CAMERA_ANGLES = {
        FastMath.PI/2, FastMath.PI/3, FastMath.PI/4 };
    protected final static float[] CAMERA_ZOOMS = {
        150f, 112.5f, 75f };
}
