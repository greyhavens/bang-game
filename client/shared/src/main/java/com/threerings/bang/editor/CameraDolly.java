//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JPanel;

import com.badlogic.gdx.Input.Keys;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.jme.camera.CameraHandler;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Allows the user to move the camera around the board.
 */
public class CameraDolly extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "camera_dolly";

    public CameraDolly (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);

        CameraHandler camhand = _ctx.getCameraHandler();
        camhand.setZoomLimits(MIN_DISTANCE, MAX_DISTANCE);
        camhand.setTiltLimits(MIN_ELEVATION, MAX_ELEVATION);
    }

    /**
     * Recenters the camera, pointing it at the center of the board.
     */
    public void recenter ()
    {
        CameraHandler camhand = _ctx.getCameraHandler();
        float width = _panel.view.getBoard().getWidth() * TILE_SIZE,
            height = _panel.view.getBoard().getHeight() * TILE_SIZE;
        Vector3f offset = camhand.getCamera().getLocation().subtract(
            camhand.getGroundPoint());
        camhand.setLocation(offset.addLocal(width / 2, height / 2, 0f));
    }

    /**
     * Saves the dolly's position and takes it offline.
     */
    public void suspend ()
    {
        if (_camloc != null) {
            return;
        }
        Camera camera = _ctx.getCameraHandler().getCamera();
        _camloc = new Vector3f(camera.getLocation());
        _camleft = new Vector3f(camera.getLeft());
        _camup = new Vector3f(camera.getUp());
        _camdir = new Vector3f(camera.getDirection());
    }

    /**
     * Brings the dolly back online and restores the saved position.
     */
    public void resume ()
    {
        if (_camloc == null) {
            return;
        }
        _ctx.getCameraHandler().getCamera().setFrame(
            _camloc, _camleft, _camup, _camdir);
        _camloc = null;
    }

    // documentation inherited
    public String getName ()
    {
        return NAME;
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        _lastX = e.getX();
        _lastY = e.getY();
        _lastButton = e.getButton();
    }

    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        int dx = _lastX - e.getX(), dy =  _lastY - e.getY();
        switch(_lastButton) {
            case MouseEvent.BUTTON1: // left rotates
                _ctx.getCameraHandler().orbitCamera(dx * ANGULAR_SCALE);
                _ctx.getCameraHandler().tiltCamera(dy * ANGULAR_SCALE);
                break;

            case MouseEvent.BUTTON2: // right pans
                _ctx.getCameraHandler().panCamera(dx * LINEAR_SCALE,
                    dy * LINEAR_SCALE);
                break;

            case MouseEvent.BUTTON3: // middle "zooms"
                _ctx.getCameraHandler().zoomCamera(dy * LINEAR_SCALE);
                break;
        }

        _lastX = e.getX();
        _lastY = e.getY();
    }

    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        _ctx.getCameraHandler().zoomCamera(e.getDelta() * 10 * LINEAR_SCALE);
    }

    @Override public void keyPressed (KeyEvent e)
    {
        int code = e.getKeyCode();
        CameraHandler camhand = _ctx.getCameraHandler();
        switch (code) {
            case Keys.Q: camhand.zoomCamera(+5f); break;
            case Keys.W: camhand.panCamera(0f, +5f); break;
            case Keys.E: camhand.zoomCamera(-5f); break;
            case Keys.A: camhand.panCamera(-5f, 0f); break;
            case Keys.S: camhand.panCamera(0f, -5f); break;
            case Keys.D: camhand.panCamera(+5f, 0f); break;
            case Keys.UP: camhand.tiltCamera(FastMath.PI * 0.01f); break;
            case Keys.DOWN: camhand.tiltCamera(-FastMath.PI * 0.01f); break;
            case Keys.LEFT: camhand.orbitCamera(-FastMath.PI * 0.01f); break;
            case Keys.RIGHT: camhand.orbitCamera(+FastMath.PI * 0.01f); break;
        }
    }

    @Override public void keyReleased (KeyEvent e)
    {
        // no-op
    }

    // documentation inherited
    protected JPanel createOptions ()
    {
        return new JPanel();
    }

    /** The last mouse coordinates and button pressed. */
    protected int _lastX, _lastY, _lastButton;

    /** Camera position saved on suspension. */
    protected Vector3f _camloc, _camleft, _camup, _camdir;

    /** The angular scale (radians per pixel). */
    protected static final float ANGULAR_SCALE = FastMath.PI / 1000;

    /** The linear scale (world units per pixel). */
    protected static final float LINEAR_SCALE = 0.25f;

    /** The minimum distance from the target. */
    protected static final float MIN_DISTANCE = 50.0f;

    /** The maximum distance from the target. */
    protected static final float MAX_DISTANCE = 500.0f;

    /** The minimum elevation. */
    protected static final float MIN_ELEVATION = FastMath.PI / 16.0f;

    /** The maximum elevation. */
    protected static final float MAX_ELEVATION = FastMath.HALF_PI;
}
