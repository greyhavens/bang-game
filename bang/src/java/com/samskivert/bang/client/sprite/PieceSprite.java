//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.samskivert.swing.Label;

import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.util.LinePath;

import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;
import static com.samskivert.bang.client.BangMetrics.*;

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
        this(SQUARE-4, SQUARE-4);
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
    public void init (Piece piece)
    {
        _piece = piece;

        // create our piece id label if we've not already
        _idLabel = new Label("" + piece.pieceId, Color.black, null);

        // position ourselves properly
        setLocation(SQUARE * piece.x[0] + 2,
                    SQUARE * piece.y[0] + 2);
    }

    /**
     * Called when we receive an event indicating that our piece was
     * updated in some way.
     */
    public void updated (Piece piece)
    {
        // note our new piece
        _piece = piece;

        // move ourselves to our new location
        move(new LinePath(_bounds.x, _bounds.y,
                          piece.x[0] * SQUARE + 2,
                          piece.y[0] * SQUARE + 2, 250L));
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

    /** Paints an indicator of this piece's remaining energy. */
    protected void paintEnergy (Graphics2D gfx)
    {
        gfx.setColor(Color.orange);
        if (_piece.energy >= _piece.energyPerStep()) {
            gfx.fillRect(_bounds.x, _bounds.y, 4, 4);
        }
        if (_piece.energy >= 2*_piece.energyPerStep()) {
            gfx.fillRect(_bounds.x+_bounds.width-4, _bounds.y, 4, 4);
        }
        if (_piece.energy >= 3*_piece.energyPerStep()) {
            gfx.fillRect(_bounds.x+_bounds.width-4,
                         _bounds.y+_bounds.height-4, 4, 4);
        }
        if (_piece.energy >= 4*_piece.energyPerStep()) {
            gfx.fillRect(_bounds.x, _bounds.y+_bounds.height-4, 4, 4);
        }
    }

    /**
     * Computes a bounding rectangle around the specifeid piece's various
     * segments. Assumes all segments are 1x1.
     *
     * @param ulidx the index of the segment in the upper left.
     */
    protected Rectangle computeBounds (Piece piece)
    {
        int leftx = piece.x[0], uppery = piece.y[0];
        Rectangle rect = new Rectangle(SQUARE*piece.x[0], SQUARE*piece.y[0],
                                       SQUARE, SQUARE);
        for (int ii = 1; ii < piece.x.length; ii++) {
            _unit.setLocation(SQUARE*piece.x[ii], SQUARE*piece.y[ii]);
            rect.add(_unit);
            if (piece.x[ii] < leftx) {
                leftx = piece.x[ii];
            }
            if (piece.y[ii] < uppery) {
                uppery = piece.y[ii];
            }
        }
        rect.setLocation(SQUARE * leftx, SQUARE * uppery);
        return rect;
    }

    protected Piece _piece;
    protected boolean _selected;
    protected Label _idLabel;

    /** Used by {@link #_computeBounds}. */
    protected static Rectangle _unit = new Rectangle(0, 0, SQUARE, SQUARE);
}
