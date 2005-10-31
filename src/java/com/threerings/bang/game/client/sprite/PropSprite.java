//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;

import com.threerings.bang.client.Model;
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
    public PropSprite (String type)
    {
        _config = PropConfig.getConfig(type);
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
        _model = ctx.getModelCache().getModel("props", _config.type);
        Node[] meshes = _model.getAnimation("normal").getMeshes(0);
        for (int ii = 0; ii < meshes.length; ii++) {
            attachChild(meshes[ii]);
            meshes[ii].updateRenderState();
        }

        setRenderState(RenderUtil.blendAlpha);
        updateRenderState();
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

//     protected static final ColorRGBA FOOT_COLOR =
//         new ColorRGBA(1, 1, 1, 0.5f);
}
