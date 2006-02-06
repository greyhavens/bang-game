//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Pyramid;
import com.jme.scene.state.LightState;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Viewpoint;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a viewpoint in the editor.
 */
public class ViewpointSprite extends PieceSprite
{
    public ViewpointSprite ()
    {
        // only show up in editor mode
        if (!_editorMode) {
            return;
        }

        Pyramid pyramid = new Pyramid("marker", TILE_SIZE/2, TILE_SIZE/2);
        pyramid.getLocalTranslation().set(0f, 0f, TILE_SIZE/2);
        pyramid.setSolidColor(ColorRGBA.gray);
        pyramid.setModelBound(new BoundingBox());
        pyramid.updateModelBound();
        attachChild(pyramid);
    }

    /**
     * Hides this sprite and binds the camera to the viewpoint.
     */
    public void bindCamera (Camera camera)
    {
        if (_boundcam == camera) {
            return;
        }

        if (_boundcam != null) {
            unbindCamera();
        }
        _boundcam = camera;
        setCullMode(CULL_ALWAYS);
        setIsCollidable(false);
        updateBoundCamera();
    }

    /**
     * Unbinds the camera from this viewpoint.
     */
    public void unbindCamera ()
    {
        if (_boundcam == null) {
            return;
        }
        _boundcam = null;
        setCullMode(CULL_INHERIT);
        setIsCollidable(true);
    }

    @Override // documentation inherited
    public boolean updatePosition (BangBoard board)
    {
        super.updatePosition(board);

        // update fine positioning as well
        Viewpoint vp = (Viewpoint)_piece;
        if (_fx != vp.fx || _fy != vp.fy) {
            setLocation(_px, _py, _elevation);
        }
        if (_forient != vp.forient || _pitch != vp.pitch) {
            setOrientation(_porient);
        }

        // copy the new state to the bound camera
        if (_boundcam != null) {
            updateBoundCamera();
        }

        return false;
    }

    @Override // documentation inherited
    public void setLocation (int tx, int ty, int elevation)
    {
        // adjust by fine coordinates
        _elevation = elevation;
        toWorldCoords(tx, ty, elevation, _temp);
        Viewpoint vp = (Viewpoint)_piece;
        _temp.x += (_fx = vp.fx) * PropSprite.FINE_POSITION_SCALE;
        _temp.y += (_fy = vp.fy) * PropSprite.FINE_POSITION_SCALE;
        getLocalTranslation().set(_temp);
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        Viewpoint vp = (Viewpoint)_piece;
        getLocalRotation().fromAngleNormalAxis(ROTATIONS[orientation] -
            (_forient = vp.forient) * PropSprite.FINE_ROTATION_SCALE, UP);
        _rot.fromAngleNormalAxis(
            (_pitch = vp.pitch) * PropSprite.FINE_ROTATION_SCALE, LEFT);
        getLocalRotation().multLocal(_rot);
    }

    @Override // documentation inherited
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        return ((Viewpoint)_piece).elevation;
    }

    /**
     * Returns the direction in which this sprite is "pointing".
     */
    public Vector3f getViewDirection ()
    {
        return getLocalRotation().mult(FORWARD);
    }

    /**
     * Updates the camera frame based on the location and orientation of the
     * sprite.
     */
    protected void updateBoundCamera ()
    {
        _temp.set(getLocalTranslation());
        _temp.z += TILE_SIZE / 2;
        _rot.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        getLocalRotation().mult(_rot, _rot);
        _boundcam.setFrame(_temp, _rot);
    }

    /** The camera to which this viewpoint is bound, if any. */
    protected Camera _boundcam;

    /** The displayed fine x, fine y, fine orientation, and pitch. */
    protected int _fx, _fy, _forient, _pitch;

    /** Temporary rotation result. */
    protected Quaternion _rot = new Quaternion();
}
