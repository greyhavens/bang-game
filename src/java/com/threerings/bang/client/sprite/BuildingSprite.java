//
// $Id$

package com.threerings.bang.client.sprite;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;

import com.threerings.bang.client.Model;
import com.threerings.bang.data.BuildingConfig;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a building piece.
 */
public class BuildingSprite extends PieceSprite
{
    public BuildingSprite (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        BuildingConfig config = BuildingConfig.getConfig(_type);
        float width = TILE_SIZE*config.width, height = TILE_SIZE*config.height;

        // our models are centered at the origin, but we need to shift
        // them to the center of the building's footprint
        Vector3f offset = new Vector3f(width/2, height/2, 0);
        _model = ctx.getModelCache().getModel("buildings", _type);
        Node[] meshes = _model.getMeshes("standing");
        for (int ii = 0; ii < meshes.length; ii++) {
            meshes[ii].setLocalTranslation(offset);
            attachChild(meshes[ii]);
            meshes[ii].updateRenderState();
        }

        // draw a footprint if we're in editor mode
        if (_editorMode) {
            Quad foot = new Quad("footprint", width, height);
            foot.setLocalTranslation(new Vector3f(width/2, height/2, 0.5f));
            attachChild(foot);
        }
    }

    protected String _type;
    protected Model _model;
}
