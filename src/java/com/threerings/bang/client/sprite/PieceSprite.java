//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.samskivert.swing.Label;

import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.MathUtil;

import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of a particular piece on the board.
 */
public class PieceSprite extends Sprite
{
    public PieceSprite (int width, int height)
    {
        super(width, height);
    }

    public PieceSprite ()
    {
        this(SQUARE-3, SQUARE-3);
    }

    /** Returns the id of the piece associated with this sprite. */
    public int getPieceId ()
    {
        return _piece.pieceId;
    }

    /** Indicates to this piece that it is selected by the user. Triggers
     * a special "selected" rendering mode. */
    public void setSelected (boolean selected)
    {
        if (_selected != selected) {
            _selected = selected;
            invalidate();
        }
    }

    /**
     * Returns true if this sprite can be clicked and selected, false if
     * not.
     */
    public boolean isSelectable ()
    {
        return false;
    }

    /**
     * Called when we are first created and immediately before we are
     * added to the display.
     */
    public void init (ToyBoxContext ctx, Piece piece, short tick)
    {
        _piece = piece;
        _tick = tick;

        // create our piece id label if we've not already
        _idLabel = new Label("" + piece.pieceId, Color.black, null);

        // position ourselves properly
        setLocation(SQUARE * piece.x, SQUARE * piece.y);
    }

    /**
     * Called when we receive an event indicating that our piece was
     * updated in some way.
     */
    public void updated (Piece piece, short tick)
    {
        // note our new piece and the current tick
        _piece = piece;
        _tick = tick;

        // move ourselves to our new location
        int nx = piece.x * SQUARE, ny = piece.y * SQUARE;
        if (nx != _ox || ny != _oy) {
            if (_mgr != null && !(_piece instanceof BigPiece)) {
                long duration = (long)MathUtil.distance(_ox, _oy, nx, ny) * 3;
                move(new LinePath(_ox, _oy, nx, ny, duration));
            } else {
                // if we're invisible just warp there
                setLocation(nx, ny);
            }
        } else {
            invalidate();
        }
    }

    /**
     * Called when our piece is removed from the board state.
     */
    public void removed ()
    {
        // remove ourselves from the sprite manager and go away
        ((SpriteManager)_mgr).removeSprite(this);
    }

    // documentation inherited
    protected void init ()
    {
        super.init();

        // lay out our piece id label
        _idLabel.layout(_mgr.getMediaPanel());
    }

    /**
     * Computes a bounding rectangle around the specifeid piece's various
     * segments. Assumes all segments are 1x1.
     */
    protected Rectangle computeBounds (Piece piece)
    {
        _unit.setLocation(SQUARE*piece.x, SQUARE*piece.y);
        return _unit;
    }

    protected Piece _piece;
    protected short _tick;

    protected boolean _selected;
    protected Label _idLabel;

    /** Used by {@link #_computeBounds}. */
    protected static Rectangle _unit = new Rectangle(0, 0, SQUARE, SQUARE);
}
