//
// $Id$

package com.threerings.bang.client.sprite;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;

import com.threerings.bang.client.Model;
import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BuildingConfig;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a building piece.
 */
public class BuildingSprite extends PieceSprite
{
    public BuildingSprite (String type)
    {
        _config = BuildingConfig.getConfig(type);
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        // our models are centered at the origin, but we need to shift
        // them to the center of the building's footprint
        _model = ctx.getModelCache().getModel("buildings", _config.type);
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
    }

    @Override // documentation inherited
    protected void centerWorldCoords (Vector3f coords)
    {
        // the piece width and height account for rotation
        coords.x += (TILE_SIZE*_piece.getWidth())/2;
        coords.y += (TILE_SIZE*_piece.getHeight())/2;
    }

    protected BuildingConfig _config;
    protected Model _model;
}
