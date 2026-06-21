//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

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
        // fork Pyramid has no jME3 equivalent (migration map §2.3 REBUILD, 1 consumer); build a
        // simple square-based pyramid Mesh. The grey colour is set on the material in createGeometry
        // (the asset manager is not available until the context is set).
        _pyramid = new Geometry("marker", createPyramidMesh(TILE_SIZE/2, TILE_SIZE/2));
        attachChild(_pyramid);
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();
        if (_pyramid != null) {
            Material mat = new Material(_ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(ColorRGBA.Gray));
            _pyramid.setMaterial(mat);
        }
    }

    /** Builds a square-based pyramid mesh of the given base width and height. */
    protected static Mesh createPyramidMesh (float width, float height)
    {
        float h = width / 2;
        Vector3f apex = new Vector3f(0, 0, height);
        Vector3f[] base = new Vector3f[] {
            new Vector3f(-h, -h, 0), new Vector3f(h, -h, 0),
            new Vector3f(h, h, 0), new Vector3f(-h, h, 0),
        };
        // 4 triangular sides (apex + two base corners) + 2 base triangles
        Vector3f[] verts = new Vector3f[] {
            apex, base[0], base[1],
            apex, base[1], base[2],
            apex, base[2], base[3],
            apex, base[3], base[0],
            base[0], base[1], base[2],
            base[0], base[2], base[3],
        };
        int[] indices = new int[verts.length];
        for (int ii = 0; ii < indices.length; ii++) {
            indices[ii] = ii;
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3,
            BufferUtils.createFloatBuffer(verts));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();
        return mesh;
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
        setCullHint(CullHint.Always);
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
        setCullHint(CullHint.Inherit);
    }

    @Override // documentation inherited
    public boolean updatePosition (BangBoard board)
    {
        super.updatePosition(board);

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
        toWorldCoords(tx, ty, elevation, _temp);
        Viewpoint vp = (Viewpoint)_piece;
        _temp.x += vp.fx * PropSprite.FINE_POSITION_SCALE;
        _temp.y += vp.fy * PropSprite.FINE_POSITION_SCALE;
        _temp.z += TILE_SIZE * 0.5f;
        setLocalTranslation(new Vector3f(_temp));
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        Viewpoint vp = (Viewpoint)_piece;
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(ROTATIONS[orientation] -
            vp.forient * PropSprite.FINE_ROTATION_SCALE, UP);
        _rot.fromAngleNormalAxis(vp.pitch * PropSprite.FINE_ROTATION_SCALE,
            LEFT);
        rot.multLocal(_rot);
        setLocalRotation(rot);
    }
    
    /**
     * Returns the direction in which this sprite is "pointing".
     */
    public Vector3f getViewDirection ()
    {
        return getLocalRotation().mult(FORWARD);
    }

    /**
     * Returns the orientation of the view.
     */
    public Quaternion getViewRotation ()
    {
        Quaternion lrot = getLocalRotation(),
            vrot = new Quaternion();
        vrot.fromAxes(lrot.mult(LEFT), lrot.mult(UP), lrot.mult(FORWARD));
        vrot.normalizeLocal();
        return vrot;
    }
    
    /**
     * Updates the camera frame based on the location and orientation of the
     * sprite.
     */
    protected void updateBoundCamera ()
    {
        _boundcam.setFrame(getLocalTranslation(), getViewRotation());
    }

    /** The editor pyramid marker geometry, if any. */
    protected Geometry _pyramid;

    /** The camera to which this viewpoint is bound, if any. */
    protected Camera _boundcam;

    /** Temporary rotation result. */
    protected Quaternion _rot = new Quaternion();
}
