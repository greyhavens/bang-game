//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.shape.Box;
import com.jme.scene.state.LightState;

import com.threerings.openal.SoundGroup;

import com.threerings.jme.model.Model;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a prop piece.
 */
public class PropSprite extends PieceSprite
{
    /** The ratio between radians and fine rotation units. */
    public static final float FINE_ROTATION_SCALE =
        (FastMath.PI * 0.25f) / 128;

    /** The ratio between radians and the coarse rotation units used for pitch
     * and roll values. */
    public static final float COARSE_ROTATION_SCALE = FastMath.PI / 128;

    /** The ratio between world units and fine translation units. */
    public static final float FINE_POSITION_SCALE =
        (TILE_SIZE * 0.5f) / 128;

    public PropSprite (String type)
    {
        _config = PropConfig.getConfig(type);
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        // set the scale before locking the model's transforms
        Prop p = (Prop)piece;
        setLocalScale(p.getScale());

        super.init(ctx, view, board, sounds, piece, tick);
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.STATIC;
    }

    @Override // documentation inherited
    public boolean isShadowable ()
    {
        return false;
    }

    @Override // documentation inherited
    public void setLocation (int tx, int ty, int elevation)
    {
        // adjust by fine coordinates
        toWorldCoords(tx, ty, elevation, _temp);
        Prop prop = (Prop)_piece;
        _temp.x += prop.fx * FINE_POSITION_SCALE;
        _temp.y += prop.fy * FINE_POSITION_SCALE;
        if (!_temp.equals(localTranslation)) {
            setLocalTranslation(new Vector3f(_temp));
        }
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        Prop prop = (Prop)_piece;
        getLocalRotation().fromAngles(new float[] {
            -prop.pitch * COARSE_ROTATION_SCALE,
            -prop.roll * COARSE_ROTATION_SCALE,
            ROTATIONS[orientation] - prop.forient * FINE_ROTATION_SCALE });
    }

    @Override // documentation inherited
    public boolean updatePosition (BangBoard board)
    {
        if (_editorMode) {
            setLocalScale(((Prop)_piece).getScale());
        }

        return super.updatePosition(board);
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        loadModel("props", _config.model, _config.variant);
    }

    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        // in the game, we can lock the bounds and transforms of props
        // because we know they won't move
        super.modelLoaded(model);
        if (!_editorMode) {
            updateWorldVectors();
            model.lockInstance();
        } else if (Boolean.parseBoolean(
            model.getProperties().getProperty("editor_handle"))) {
            // the prop requires a handle to manipulate it in the editor
            Box handle = new Box("handle", new Vector3f(), 0.5f, 0.5f, 0.5f);
            handle.setModelBound(new BoundingBox());
            handle.updateModelBound();
            handle.setLightCombineMode(LightState.OFF);
            attachChild(handle);
            handle.updateRenderState();
        }
    }

    @Override // documentation inherited
    protected void centerWorldCoords (Vector3f coords)
    {
        // the piece width and height account for rotation
        coords.x += (TILE_SIZE*_piece.getWidth())/2;
        coords.y += (TILE_SIZE*_piece.getLength())/2;
    }

    protected PropConfig _config;

//     protected static final ColorRGBA FOOT_COLOR =
//         new ColorRGBA(1, 1, 1, 0.5f);
}
