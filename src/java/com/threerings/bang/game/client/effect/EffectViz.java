//
// $Id$

package com.threerings.bang.game.client.effect;

import java.awt.Point;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.piece.Piece;
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
    public void init (BangContext ctx, BangBoardView view, Vector3f localTranslation,
                      Observer obs)
    {
        _ctx = ctx;
        _view = view;
        _pos = localTranslation;
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
     * Gets the target's piece sprite.
     */
    protected PieceSprite getTargetSprite()
    {
        return (_target != null) ? _view.getPieceSprite(_target) : null;
    }


    /**
     * Sets the target's local translation
     */
    public final Vector3f getLocalTranslation()
    {
        if (_target != null) {
            return (Vector3f)getTargetSprite().getLocalTranslation().clone();
        } else if (_coords != null) {
            float tx = (_coords.x + 0.5f) * TILE_SIZE,
                ty = (_coords.y + 0.5f) * TILE_SIZE,
                tz = _view.getTerrainNode().getHeightfieldHeight(tx, ty);
            return new Vector3f(tx, ty, tz);
        } else {
            return (Vector3f)_pos.clone();
        }
    }

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected Point _coords;
    protected Vector3f _pos;
    protected Piece _target;
    protected Observer _observer;
}
