//
// $Id$

package com.threerings.bang.game.client;

import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseWheelListener;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.GodViewHandler;
import com.threerings.jme.camera.SwingPath;

import com.threerings.bang.client.util.KeyListener;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.bang.util.BasicContext;

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
    }

    /**
     * Provides the camera handler with a reference to our context.
     */
    public void init (BasicContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Configures the camera at the start of the game.
     */
    public void startGame (BangView view, BangObject bangobj, int pidx)
    {
        // listen for mouse wheel, etc. events
        view.view.addListener(_swingListener);
        view.addListener(_rollListener);

        // start the camera in the center of the board, pointing straight down
        float cx = TILE_SIZE * bangobj.board.getWidth() / 2;
        float cy = TILE_SIZE * bangobj.board.getHeight() / 2;
        float height = CAMERA_ZOOMS[0];
        _camera.setLocation(new Vector3f(cx, cy, height));

        // rotate the camera by 45 degrees and orient it properly
        float rotangle = FastMath.PI/4;
        Vector3f left = new Vector3f(-1, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f forward = new Vector3f(0, 0, -1);
        Matrix3f rotm = new Matrix3f();

        // the camera starts out looking straight down at the board, so we
        // rotate it around the forward axis to the desired starting angle
        rotm.fromAngleAxis(rotangle, forward);
        rotm.mult(forward, forward);
        rotm.mult(left, left);
        rotm.mult(up, up);

        _camera.setDirection(forward);
        _camera.setLeft(left);
        _camera.setUp(up);
        _camera.update();

        // recompute the panning vectors
        _rxdir.set(1, 0, 0);
        _rydir.set(0, 1, 0);
        rotm.mult(_rxdir, _rxdir);
        rotm.mult(_rydir, _rydir);

        // set the pan limits based on the board size
        float off = TILE_SIZE * (BangBoard.BORDER_SIZE + 6),
            breadth = TILE_SIZE * bangobj.board.getWidth() - off*2,
            depth = TILE_SIZE * bangobj.board.getHeight() - off*2;
        setPanLimits(off-50, off-50, off+breadth+50, off+depth+50);
    }

    public void endGame (BangView view)
    {
        // stop listening for mouse wheel, etc. events
        view.view.removeListener(_swingListener);
        view.removeListener(_rollListener);
    }

    /**
     * Swings the camera smoothly around the point on the ground at which it is
     * "looking", by the requested angle (in radians).
     */
    public void swingCamera (float deltaAngle)
    {
        JmeApp app = _ctx.getApp();
        if (app.cameraIsMoving()) {
            log.info("Already rotating, no swing: " + deltaAngle + ".");
            return;
        }

        float angvel = 2*FastMath.PI;
        app.moveCamera(new SwingPath(_camera, groundPoint(_camera),
                                     _groundNormal, deltaAngle, angvel, 0));

        // rotate our internal scrolling directions
        _rotm.fromAxisAngle(_groundNormal, deltaAngle);
        _rotm.mult(_rxdir, _rxdir);
        _rotm.mult(_rydir, _rydir);
    }

    /**
     * Moves the camera to the next elevation angle. The camera will smoothly
     * roll to the appropriate angle rather than changing abruptly.
     */
    public void rollCamera ()
    {
        JmeApp app = _ctx.getApp();
        if (app.cameraIsMoving()) {
            return;
        }
        rollCamera((_camidx + 1) % CAMERA_ANGLES.length, 2*FastMath.PI);
    }

    protected void rollCamera (int nextidx, float angvel)
    {
        float curang = CAMERA_ANGLES[_camidx], curzoom = CAMERA_ZOOMS[_camidx];
        float nextang = CAMERA_ANGLES[nextidx], nextzoom = CAMERA_ZOOMS[nextidx];
        float deltaAngle = nextang-curang, deltaZoom = nextzoom-curzoom;
        _camidx = nextidx;
        _ctx.getApp().moveCamera(
            new SwingPath(_camera, groundPoint(_camera), _camera.getLeft(),
                          deltaAngle, angvel, deltaZoom));
    }

    protected void setKeyBindings (String api)
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

    protected KeyListener _rollListener = new KeyListener() {
        public void keyPressed (int keyCode) {
            switch (keyCode) {
            case KeyInput.KEY_C:
                rollCamera();
                break;
            }
        }
    };

    protected BasicContext _ctx;
    protected int _camidx = 0;
    protected final static float[] CAMERA_ANGLES = {
        FastMath.PI/2, FastMath.PI/3, FastMath.PI/4 };
    protected final static float[] CAMERA_ZOOMS = {
        150f, 112.5f, 75f };
}
