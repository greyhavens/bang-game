//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.scene.BillboardNode;
import com.jme.scene.shape.Quad;
import com.jme.math.Vector3f;

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
        _billboard = new BillboardNode("icon_influence");
        _billboard.setLocalTranslation(new Vector3f(
                    0f, 0f, target.getHeight() - TILE_SIZE/2));

        Quad iconQuad = IconConfig.createIcon(
                    ctx, "influences/icons/" + _name + ".png", 
                    ICON_SIZE, ICON_SIZE);
        iconQuad.setLocalTranslation(new Vector3f(0f, TILE_SIZE/4, 0f));
        _billboard.attachChild(iconQuad);
        target.attachChild(_billboard);
    }

    @Override // documentation inherited
    public void destroy ()
    {
        if (_target != null) {
            _target.detachChild(_billboard);
        }
    }

    protected BillboardNode _billboard;

    protected String _name;

    protected static float ICON_SIZE = TILE_SIZE / 3;
}
