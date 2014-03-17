//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.jme.model.EmissionController;

import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BoardView;

/**
 * The superclass of emissions whose models may be (but aren't necessarily)
 * associated with {@link PieceSprite}s.
 */
public abstract class SpriteEmission extends EmissionController
{
    /**
     * Provides the emission with relevant Bang references.
     */
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        _ctx = ctx;
        _view = view;
        _sprite = sprite;
    }

    /**
     * Sets whether to use this emission during animation.
     */
    public void setActiveEmission (boolean active)
    {
        _active = active;
    }

    /**
     * Returns the active state.
     */
    public boolean isActiveEmission ()
    {
        return _active;
    }
    
    /** The application context, or <code>null</code> for none. */
    protected BasicContext _ctx;
    
    /** The board view containing the piece sprite, or <code>null</code> for
     * none. */
    protected BoardView _view;
    
    /** The piece sprite that loaded the model, or <code>null</code> for
     * none. */
    protected PieceSprite _sprite;

    /** The active state. */
    protected boolean _active = true;
    
    private static final long serialVersionUID = 1;
}
