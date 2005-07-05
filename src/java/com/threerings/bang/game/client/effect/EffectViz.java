//
// $Id$

package com.threerings.bang.game.client.effect;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;

/**
 * A base class for effect visualizations.
 */
public abstract class EffectViz
{
    /**
     * Called by the board view after creating an effect to provide the
     * effect with needed references.
     */
    public void init (BangContext ctx, BangBoardView view,
                      Piece otarget, Piece ntarget)
    {
        _ctx = ctx;
        _view = view;
        _otarget = otarget;
        _ntarget = ntarget;

        didInit();
    }

    /**
     * Called to allow the effect to perform any initialization it might
     * need prior to display.
     */
    protected void didInit ()
    {
    }

    /**
     * Triggers the actual effect display.
     */
    public abstract void display (PieceSprite target);

    /**
     * When the effect display is completed (or nearly so), this method
     * should be called to allow any piece affected by the display to be
     * updated.
     */
    protected void effectDisplayed ()
    {
        if (_otarget != null && _ntarget != null) {
            _view.pieceUpdated(_otarget, _ntarget);
        }
    }

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected Piece _otarget, _ntarget;
}
