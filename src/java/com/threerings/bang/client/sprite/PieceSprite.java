//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.Iterator;
import java.util.List;

import com.jme.math.Vector3f;
import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.util.MathUtil;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of a particular piece on the board.
 */
public class PieceSprite extends Sprite
{
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
    public void init (BangContext ctx, Piece piece, short tick)
    {
        _piece = piece;
        _tick = tick;

        // create our sprite geometry
        createGeometry(ctx);

        // position ourselves properly to start
        setLocation(piece.x, piece.y, 0);

        // ensure that we do this the first time even if the sprite starts
        // out at zero zero
        updateCollisionTree();
    }

    /**
     * Configures this sprite's tile location.
     */
    public void setLocation (int tx, int ty, int elevation)
    {
        toWorldCoords(tx, ty, elevation, _temp);
        if (!_temp.equals(localTranslation)) {
            log.info("Moving to " + tx + ", " + ty + ", " + elevation);
            setLocalTranslation(new Vector3f(_temp));
            updateCollisionTree();
        }
    }

    /**
     * Called when we receive an event indicating that our piece was
     * updated in some way.
     */
    public void updated (BangBoard board, Piece piece, short tick)
    {
        int ox = _piece.x, oy = _piece.y;
        // note our new piece and the current tick
        Piece opiece = _piece;
        _piece = piece;
        _tick = tick;

        // move ourselves to our new location
        int nx = piece.x, ny = piece.y;
        if (nx != ox || ny != oy) {
            int elev = computeElevation(board, _piece.x, _piece.y);

            if (/* _mgr == null || */ _editorMode) {
                // if we're invisible or in the editor just warp there
                setLocation(_piece.x, _piece.y, elev);

            // TODO: append an additional path if we're currently moving
            } else if (!isMoving()) {
                List path = null;
                if (board != null) {
                    path = board.computePath(opiece, piece.x, piece.y);
                }
                if (path != null) {
                    // create a world coordinate path from the tile
                    // coordinates
                    Vector3f[] coords = new Vector3f[path.size()];
                    float[] durations = new float[path.size()-1];
                    int ii = 0, elevation = 0; // TODO: handle elevated paths
                    for (Iterator iter = path.iterator(); iter.hasNext(); ii++) {
                        Point p = (Point)iter.next();
                        coords[ii] = new Vector3f();
                        toWorldCoords(p.x, p.y, elevation, coords[ii]);
                        if (ii > 0) {
                            durations[ii-1] = 0.1f;
                        }
                    }
                    LineSegmentPath lspath =
                        new LineSegmentPath(this, coords, durations);
                    move(lspath);
                } else {
                    Vector3f start = toWorldCoords(ox, oy, 0, new Vector3f());
                    Vector3f end = toWorldCoords(nx, ny, 0, new Vector3f());
                    float duration = (float)
                        MathUtil.distance(ox, oy, nx, ny) * 3 / 1000f;
                    move(new LinePath(this, start, end, duration));
                }
            }
        }
    }

    /**
     * Called when our piece is removed from the board state.
     */
    public void removed ()
    {
//         // remove ourselves from the sprite manager and go away
//         if (_mgr != null) {
//             ((SpriteManager)_mgr).removeSprite(this);
//         }
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        updateCollisionTree();
    }

//     // documentation inherited
//     protected void init ()
//     {
//         super.init();
//     }

//     /**
//      * Computes a bounding rectangle around the specifeid piece's various
//      * segments. Assumes all segments are 1x1.
//      */
//     protected Rectangle computeBounds (Piece piece)
//     {
//         _unit.setLocation(SQUARE*piece.x, SQUARE*piece.y);
//         return _unit;
//     }

    /**
     * Called by the editor to make pieces warp to their new locations for
     * rapid draggability.
     */
    public static void setEditorMode (boolean editorMode)
    {
        _editorMode = editorMode;
    }

    /**
     * Sprites should create and attach their scene geometry by overriding
     * this method.
     */
    protected void createGeometry (BangContext ctx)
    {
    }

    /**
     * Computes the elevation for this piece at the specified location.
     */
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        return 0;
    }

    /**
     * Converts tile coordinates plus elevation into (3D) world
     * coordinates.
     */
    protected Vector3f toWorldCoords (int tx, int ty, int elev, Vector3f target)
    {
        target.x = tx * TILE_SIZE;
        target.y = ty * TILE_SIZE;
        target.z = elev * TILE_SIZE;
        return target;
    }

    protected Piece _piece;
    protected short _tick;

    protected boolean _selected;

    /** When activated, causes all pieces to warp instead of smoothly
     * follow a path. */
    protected static boolean _editorMode;

    /** Used for temporary calculations. */
    protected static Vector3f _temp = new Vector3f();

//     /** Used by {@link #_computeBounds}. */
//     protected static Rectangle _unit = new Rectangle(0, 0, SQUARE, SQUARE);
}
