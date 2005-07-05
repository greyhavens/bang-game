//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;

import com.threerings.bang.client.Model;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.util.BangContext;

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
    protected void createGeometry (BangContext ctx)
    {
        // create our alpha state if need be
        if (_alpha == null) {
            _alpha = ctx.getRenderer().createAlphaState();
            _alpha.setBlendEnabled(true);
            _alpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
            _alpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
            _alpha.setEnabled(true);
        }

        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        _model = ctx.getModelCache().getModel("props", _config.type);
        Node[] meshes = _model.getMeshes("standing");
        for (int ii = 0; ii < meshes.length; ii++) {
            attachChild(meshes[ii]);
            meshes[ii].updateRenderState();
        }

        // draw a footprint if we're in editor mode
        if (_editorMode) {
            attachChild(new Quad("footprint", TILE_SIZE*_config.width,
                                 TILE_SIZE*_config.height));
        }

        setRenderState(_alpha);
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

    protected static AlphaState _alpha;
}
