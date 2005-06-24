//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.image.BufferedImage;

import com.jme.math.FastMath;
import com.jme.scene.Node;
import com.jme.scene.shape.Dome;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends PieceSprite
{
    public BonusSprite (String type)
    {
        _type = type;
        addController(new Spinner(this, FastMath.PI/2));
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        super.createGeometry(ctx);

        // load up the model for this bonus
        Model model = ctx.getModelCache().getModel("bonuses", _type);
        Node[] meshes = model.getMeshes("standing");
        // TEMP: cope with bonuses for which we yet have no model
        if (meshes[0].getName().startsWith("error")) {
            System.err.println("Using error geom for " + _type + ".");
            // create some simple temporary geometry
            Dome geom =
                new Dome("bonus", 10, 10, TILE_SIZE/2);
            attachChild(geom);
            BufferedImage image =
                ctx.loadImage("bonuses/" + _type + "/icon.png");
            geom.setRenderState(RenderUtil.createTexture(ctx, image));
            geom.updateRenderState();
        } else {
            for (int ii = 0; ii < meshes.length; ii++) {
                attachChild(meshes[0]);
            }
        }
    }

    protected String _type;
}
