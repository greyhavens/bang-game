//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.Iterator;
import java.util.List;

import com.samskivert.swing.Label;

import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.LineSegmentPath;
import com.threerings.media.util.MathUtil;

import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.data.BangBoard;
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
        this(SQUARE, SQUARE);
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
    public void updated (BangBoard board, Piece piece, short tick)
    {
        // note our new piece and the current tick
        Piece opiece = _piece;
        _piece = piece;
        _tick = tick;

        // move ourselves to our new location
        int nx = piece.x * SQUARE, ny = piece.y * SQUARE;
        if (nx != _ox || ny != _oy) {
            if (_mgr == null || _editorMode) {
                // if we're invisible just warp there
                setLocation(nx, ny);

            } else {
                List path = null;
                if (board != null) {
                    path = board.computePath(opiece, piece.x, piece.y);
                }
                if (path != null) {
                    // convert the tile coordinates to screen coordinates
                    for (Iterator iter = path.iterator(); iter.hasNext(); ) {
                        Point p = (Point)iter.next();
                        p.x *= SQUARE;
                        p.y *= SQUARE;
                    }
                    LineSegmentPath lspath = new LineSegmentPath(path);
                    lspath.setVelocity(1/3f);
                    move(lspath);
                } else {
                    long duration = (long)
                        MathUtil.distance(_ox, _oy, nx, ny) * 3;
                    move(new LinePath(_ox, _oy, nx, ny, duration));
                }
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
        if (_mgr != null) {
            ((SpriteManager)_mgr).removeSprite(this);
        }
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

    /**
     * Called by the editor to make pieces warp to their new locations for
     * rapid draggability.
     */
    public static void setEditorMode (boolean editorMode)
    {
        _editorMode = editorMode;
    }

    protected Piece _piece;
    protected short _tick;

    protected boolean _selected;
    protected Label _idLabel;

    /** When activated, causes all pieces to warp instead of smoothly
     * follow a path. */
    protected static boolean _editorMode;

    /** Used by {@link #_computeBounds}. */
    protected static Rectangle _unit = new Rectangle(0, 0, SQUARE, SQUARE);
}
