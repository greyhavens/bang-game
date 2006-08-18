//
// $Id$

package com.threerings.bang.game.client.effect;

import java.awt.Point;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;

/**
 * A base class for effect visualizations.
 */
public abstract class EffectViz
{
    public interface Observer
    {
        public void effectDisplayed ();
    }        

    /**
     * Initializes this effect and prepares it for display on the specified
     * piece.
     */
    public void init (BangContext ctx, BangBoardView view, Piece target,
                      Observer obs)
    {
        _ctx = ctx;
        _view = view;
        _target = target;
        _observer = obs;
        didInit();
    }

    /**
     * Initializes this effect and prepares it for display at the specified
     * tile coordinates.
     */
    public void init (BangContext ctx, BangBoardView view, int x, int y,
                      Observer obs)
    {
        _ctx = ctx;
        _view = view;
        _coords = new Point(x, y);
        _observer = obs;
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
     *
     * @param target the target sprite, if the visualization targets a piece
     */
    public abstract void display (PieceSprite target);
    
    /**
     * When the effect display is completed (or nearly so), this method should
     * be called to inform our observer which will then update the associated
     * piece.
     */
    protected void effectDisplayed ()
    {
        if (_observer != null) {
            _observer.effectDisplayed();
        }
    }

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected Piece _target;
    protected Point _coords;
    protected Observer _observer;
}
