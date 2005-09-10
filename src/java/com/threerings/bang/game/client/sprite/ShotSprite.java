//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Node;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a fired shot.
 */
public class ShotSprite extends Sprite
{
    public ShotSprite (BangContext ctx)
    {
        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        _model = ctx.getModelCache().getModel("bonuses", "missile");
        Node[] meshes = _model.getAnimation("normal").getMeshes(0);
        for (int ii = 0; ii < meshes.length; ii++) {
            attachChild(meshes[ii]);
            meshes[ii].updateRenderState();
        }
        setLocalScale(0.5f);
    }

    protected Model _model;
}
