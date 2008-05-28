//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.Vector3f;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

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
    public void init (BangContext ctx, BangBoardView view, PieceSprite sprite, Observer obs)
    {
        _ctx = ctx;
        _view = view;
        _sprite = sprite;
        _pos = sprite.getLocalTranslation();
        _observer = obs;
        didInit();
    }

    /**
     * Initializes this effect and prepares it for display at the specified
     * tile coordinates.
     */
    public void init (BangContext ctx, BangBoardView view, int x, int y, Observer obs)
    {
        _ctx = ctx;
        _view = view;
        float tx = (x + 0.5f) * TILE_SIZE,
            ty = (y + 0.5f) * TILE_SIZE,
            tz = _view.getTerrainNode().getHeightfieldHeight(tx, ty);
        _pos = new Vector3f(tx, ty, tz);
        _observer = obs;
        didInit();
    }

    /**
     * Initializes this effect and prepares it for display at the specified
     * tile coordinates.
     */
    public void init (BangContext ctx, BangBoardView view, Vector3f pos, Observer obs)
    {
        _ctx = ctx;
        _view = view;
        _pos = pos;
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
     */
    public abstract void display ();

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

    /**
     * Sets the target's local translation
     */
    public Vector3f getPosition()
    {
        return _pos;
    }

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected Vector3f _pos;
    protected PieceSprite _sprite;
    protected Observer _observer;
}
