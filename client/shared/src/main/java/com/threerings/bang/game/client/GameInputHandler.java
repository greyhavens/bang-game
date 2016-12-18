//
// $Id$

package com.threerings.bang.game.client;

import com.badlogic.gdx.Input.Keys;

import com.jme.input.KeyBindingManager;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseWheelListener;

import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.camera.CameraPath;
import com.threerings.jme.camera.GodViewHandler;
import com.threerings.jme.camera.PanPath;
import com.threerings.jme.camera.SwingPath;

import com.threerings.bang.game.data.BangObject;

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
    public void prepareForRound (BangView view, BangObject bangobj)
    {
        // listen for mouse wheel events
        view.view.addListener(_swingListener);
        if (view.tutwin != null) {
            view.tutwin.addListener(_swingListener);
        }

        // set up the starting zoom index
        _camidx = CAMERA_ZOOMS.length-1;
        _camdelta = -1;

        // reset the camera's orientation
        _camhand.resetAxes();

        // start the camera in the center of the board, pointing straight down
        GameCameraHandler gcamhand = (GameCameraHandler)_camhand;
        float cx = TILE_SIZE * bangobj.board.getWidth() / 2;
        float cy = TILE_SIZE * bangobj.board.getHeight() / 2;
        float height = gcamhand.getSmoothedHeight(cx, cy) + CAMERA_ZOOMS[0];
        _camhand.setLocation(new Vector3f(cx, cy, height));
        gcamhand.resetGroundPointHeight();

        // rotate the camera by 45 degrees and orient it properly
        _camhand.orbitCamera(FastMath.PI/4);

        // add a camera observer that updates the board view's hover state after the camera
        // completes a path
        _camhand.addCameraObserver(_hoverUpdater = new HoverUpdater(view.view));
    }

    /**
     * Called when the round ends.
     */
    public void endRound (BangView view)
    {
        // stop listening for mouse wheel events
        if (view.tutwin != null) {
            view.tutwin.removeListener(_swingListener);
        }
        view.view.removeListener(_swingListener);

        // stop updating hover state and clear out reference
        _camhand.removeCameraObserver(_hoverUpdater);
        _hoverUpdater = null;
    }

    /**
     * Swings the camera smoothly around the point on the ground at which it is
     * "looking", by the requested angle (in radians).
     */
    public void swingCamera (float deltaAngle)
    {
        if (!isEnabled() || _camhand.cameraIsMoving()) {
            return;
        }

        float angvel = 2*FastMath.PI;
        _camhand.moveCamera(new SwingPath(_camhand, _camhand.getGroundPoint(),
                                          _camhand.getGroundNormal(), deltaAngle, angvel, 0));
    }

    /**
     * Moves the camera to the next elevation angle. The camera will smoothly roll to the
     * appropriate angle rather than changing abruptly.
     */
    public void rollCamera ()
    {
        rollCamera(2*FastMath.PI);
    }

    /**
     * Moves the camera to the next elevation angle. The camera will smoothly roll to the
     * appropriate angle at the specified velocity (in radians per second) rather than changing
     * abruptly.
     */
    public void rollCamera (float angvel)
    {
        if (!isEnabled() || _camhand.cameraIsMoving()) {
            return;
        }
        int nextidx = (_camidx + _camdelta) % CAMERA_ANGLES.length;
        if (nextidx == 0) {
            _camdelta = 1;
        } else if (nextidx == CAMERA_ANGLES.length-1) {
            _camdelta = -1;
        }
        rollCamera(nextidx, angvel);
    }

    /**
     * Pans the camera to a point where the specified location is basically in the center of the
     * view. The specified location's x and y coordinate will be used but the z coordinate will be
     * assumed to be on the ground.
     */
    public void aimCamera (Vector3f location)
    {
        Vector3f start = new Vector3f(_camhand.getCamera().getLocation());
        GameCameraHandler gcamhand = (GameCameraHandler)_camhand;
        gcamhand.getGroundPoint(_gpoint);
        gcamhand.panCameraAbs(location.x - _gpoint.x, location.y - _gpoint.y);
        Vector3f end = new Vector3f(_camhand.getCamera().getLocation());
        _camhand.setLocation(start);
        _camhand.moveCamera(new PanPath(_camhand, end, 0.25f));
    }

    protected void rollCamera (int nextidx, float angvel)
    {
        float curang = CAMERA_ANGLES[_camidx], curzoom = CAMERA_ZOOMS[_camidx];
        float nextang = CAMERA_ANGLES[nextidx],
            nextzoom = CAMERA_ZOOMS[nextidx];
        float deltaAngle = nextang-curang, deltaZoom = nextzoom-curzoom;
        _camidx = nextidx;
        _camhand.moveCamera(new SwingPath(_camhand, _camhand.getGroundPoint(),
                                          _camhand.getCamera().getLeft(), deltaAngle,
                                          angvel, deltaZoom));
    }

    protected void setKeyBindings ()
    {
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();

        // we only allow free form panning, nothing else
        keyboard.set("forward", Keys.W);
        keyboard.set("arrow_forward", Keys.UP);
        keyboard.set("backward", Keys.S);
        keyboard.set("arrow_backward", Keys.DOWN);
        keyboard.set("left", Keys.A);
        keyboard.set("arrow_left", Keys.LEFT);
        keyboard.set("right", Keys.D);
        keyboard.set("arrow_right", Keys.RIGHT);
    }

    /** Updates the board view's hover state on completion of camera paths. */
    protected static class HoverUpdater implements CameraPath.Observer
    {
        public HoverUpdater (BangBoardView view)
        {
            _view = view;
        }

        public boolean pathCompleted (CameraPath path)
        {
            _view.updateHoverState();
            return true;
        }

        protected BangBoardView _view;
    }

    protected MouseWheelListener _swingListener = new MouseWheelListener() {
        public void mouseWheeled (MouseEvent e) {
            swingCamera((e.getDelta() > 0) ? -FastMath.PI/2 : FastMath.PI/2);
        }
    };

    protected int _camidx = -1, _camdelta = 0;
    protected HoverUpdater _hoverUpdater;

    protected Vector3f _gpoint = new Vector3f();

    protected final static float[] CAMERA_ANGLES = {
        FastMath.PI/2, FastMath.PI/3, FastMath.PI/4 };
    protected final static float[] CAMERA_ZOOMS = {
        150f, 112.5f, 75f };
}
