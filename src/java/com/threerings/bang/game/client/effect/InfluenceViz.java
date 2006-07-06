//
// $Id$

package com.threerings.bang.game.client.effect;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;

/**
 * A base class for visualizations of influences.
 */
public abstract class InfluenceViz
{
    /**
     * Initializes this influence visualization and adds it to the specified
     * sprite.
     */
    public void init (BasicContext ctx, PieceSprite target)
    {
        _ctx = ctx;
        _target = target;
    }
    
    /**
     * Removes this visualization.
     */
    public void destroy ()
    {
    }
    
    protected BasicContext _ctx;
    protected PieceSprite _target;
}
