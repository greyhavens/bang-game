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
    public GameInputHandler (CameraHandler camhand, String api)
    {
        super(camhand, api);
    }

    /**
     * Configures the camera at the start of the game.
     */
    public void startGame (BangView view, BangObject bangobj, int pidx)
    {
        // listen for mouse wheel, etc. events
        view.view.addListener(_swingListener);
        view.addListener(_rollListener);

        // set the pan limits based on the board size
        _camhand.setPanLimits(
            0, 0,
            TILE_SIZE * bangobj.board.getWidth(),
            TILE_SIZE * bangobj.board.getHeight(), true);

        // start the camera in the center of the board, pointing straight down
        float cx = TILE_SIZE * bangobj.board.getWidth() / 2;
        float cy = TILE_SIZE * bangobj.board.getHeight() / 2;
        float height = CAMERA_ZOOMS[0];
        _camhand.setLocation(new Vector3f(cx, cy, height));

        // rotate the camera by 45 degrees and orient it properly
        _camhand.orbitCamera(FastMath.PI/4);
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

    protected void rollCamera (int nextidx, float angvel)
    {
        float curang = CAMERA_ANGLES[_camidx], curzoom = CAMERA_ZOOMS[_camidx];
        float nextang = CAMERA_ANGLES[nextidx], nextzoom = CAMERA_ZOOMS[nextidx];
        float deltaAngle = nextang-curang, deltaZoom = nextzoom-curzoom;
        _camidx = nextidx;
        _camhand.moveCamera(
            new SwingPath(_camhand, _camhand.getGroundPoint(),
                          _camhand.getCamera().getLeft(), deltaAngle,
                          angvel, deltaZoom));
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
        public void keyPressed (KeyEvent event) {
            switch (event.getKeyCode()) {
            case KeyInput.KEY_C:
                rollCamera();
                break;
            }
        }
        public void keyReleased (KeyEvent event) {
        }
    };

    protected int _camidx = 0;
    protected final static float[] CAMERA_ANGLES = {
        FastMath.PI/2, FastMath.PI/3, FastMath.PI/4 };
    protected final static float[] CAMERA_ZOOMS = {
        150f, 112.5f, 75f };
}
