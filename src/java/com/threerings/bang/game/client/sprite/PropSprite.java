//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;

import com.threerings.bang.client.Model;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a prop piece.
 */
public class PropSprite extends PieceSprite
{
    /** The ratio between radians and fine rotation units. */
    public static final float FINE_ROTATION_SCALE =
        (FastMath.PI * 0.25f) / 128;

    /** The ratio between world units and fine translation units. */
    public static final float FINE_POSITION_SCALE =
        (TILE_SIZE * 0.5f) / 128;

    public PropSprite (String type)
    {
        _config = PropConfig.getConfig(type);
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
    public boolean updatePosition (BangBoard board)
    {
        super.updatePosition(board);

        // update fine positioning as well
        Prop prop = (Prop)_piece;
        if (_fx != prop.fx || _fy != prop.fy) {
            setLocation(_px, _py, _elevation);
        }
        if (_forient != prop.forient) {
            setOrientation(_porient);
        }

        return false;
    }

    @Override // documentation inherited
    public void setLocation (int tx, int ty, int elevation)
    {
        // adjust by fine coordinates
        _elevation = elevation;
        toWorldCoords(tx, ty, elevation, _temp);
        Prop prop = (Prop)_piece;
        _temp.x += (_fx = prop.fx) * FINE_POSITION_SCALE;
        _temp.y += (_fy = prop.fy) * FINE_POSITION_SCALE;
        if (!_temp.equals(localTranslation)) {
            setLocalTranslation(new Vector3f(_temp));
        }
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        Quaternion quat = new Quaternion();
        Prop prop = (Prop)_piece;
        quat.fromAngleAxis(ROTATIONS[orientation] -
            (_forient = prop.forient) * FINE_ROTATION_SCALE, UP);
        setLocalRotation(quat);
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
//         // draw a footprint if we're in editor mode
//         if (_editorMode) {
//             Quad foot = new Quad("footprint", TILE_SIZE*_config.width,
//                                  TILE_SIZE*_config.height);
//             foot.setRenderState(RenderUtil.overlayZBuf);
//             foot.setRenderState(RenderUtil.blendAlpha);
//             foot.setSolidColor(FOOT_COLOR);
//             foot.setLightCombineMode(LightState.OFF);
//             foot.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
//             foot.setLocalTranslation(new Vector3f(0, 0, 0.1f));
//             attachChild(foot);
//         }

        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        _model = ctx.loadModel("props", _config.type);
        bindAnimation(ctx, _model.getAnimation("normal"), 0, null);
    }

    @Override // documentation inherited
    protected void centerWorldCoords (Vector3f coords)
    {
        // the piece width and height account for rotation
        coords.x += (TILE_SIZE*_piece.getWidth())/2;
        coords.y += (TILE_SIZE*_piece.getHeight())/2;
    }

    protected PropConfig _config;
    protected Model _model;
    protected Model.Binding _binding;

    protected int _fx, _fy, _forient;

//     protected static final ColorRGBA FOOT_COLOR =
//         new ColorRGBA(1, 1, 1, 0.5f);
}
