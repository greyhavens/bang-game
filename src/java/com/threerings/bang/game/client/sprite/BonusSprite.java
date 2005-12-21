//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;

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
        _model = ctx.loadModel("bonuses", _type);
        _binding = _model.getAnimation("normal").bind(this, 0);
    }

    protected String _type;
}
