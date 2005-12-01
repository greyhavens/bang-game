//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.scene.Node;
import com.jme.scene.shape.Dome;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;

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
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);

        // load up the model for this bonus
        Model model = ctx.loadModel("bonuses", _type);
        Node[] meshes = model.getAnimation("normal").getMeshes(0);

        // TEMP: cope with bonuses for which we yet have no model
        if (meshes.length == 0 || meshes[0].getName().startsWith("error")) {
            // create some simple temporary geometry
            Dome geom =
                new Dome("bonus", 10, 10, TILE_SIZE/2);
            attachChild(geom);
            Quaternion rotate = new Quaternion();
            rotate.fromAngleAxis(FastMath.PI/2, LEFT);
            geom.setLocalRotation(rotate);

        } else {
            for (int ii = 0; ii < meshes.length; ii++) {
                attachChild(meshes[0]);
            }
        }
    }

    protected String _type;
}
