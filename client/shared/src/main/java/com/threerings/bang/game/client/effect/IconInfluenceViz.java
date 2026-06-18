//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.IconConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An influence visualiztion that uses a floating icon.
 */
public class IconInfluenceViz extends InfluenceViz
{
    public IconInfluenceViz (String name)
    {
        _name = name;
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        // jME3: fork BillboardNode -> a Node with a BillboardControl (screen-facing orientation).
        _billboard = new Node("icon_influence");
        _billboard.addControl(new BillboardControl());
        _billboard.setLocalTranslation(new Vector3f(
                    0f, 0f, target.getHeight() - TILE_SIZE/2));

        Geometry iconQuad = IconConfig.createIcon(
                    ctx, "influences/icons/" + _name + ".png",
                    ICON_SIZE, ICON_SIZE);
        // jME3 Quad is corner-origin (0,0)-(w,h); the fork Quad was centered on its origin. Recenter
        // the quad on the billboard pivot (then lift it by TILE_SIZE/4) so it reads as a small upright
        // icon floating beside the unit's head rather than an off-pivot quad.
        iconQuad.setLocalTranslation(new Vector3f(
                    -ICON_SIZE/2f, TILE_SIZE/4 - ICON_SIZE/2f, 0f));
        _billboard.attachChild(iconQuad);
        target.attachChild(_billboard);
        // force the billboard's transforms/bounds to refresh so the BillboardControl orients it
        // (screen-facing/upright) on the first render instead of lying flat in the board plane.
        _billboard.updateGeometricState();
    }

    @Override // documentation inherited
    public void destroy ()
    {
        if (_target != null) {
            _target.detachChild(_billboard);
        }
    }

    protected Node _billboard;

    protected String _name;

    protected static float ICON_SIZE = TILE_SIZE / 3;
}
