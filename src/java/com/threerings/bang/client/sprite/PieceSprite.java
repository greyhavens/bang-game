//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme.math.Vector3f;
import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.util.MathUtil;

import com.threerings.bang.client.effect.EffectViz;
import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

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

    /** Indicates to this piece that it is selected by the user. May
     * someday trigger a special "selected" rendering mode, but presently
     * does nothing. */
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
        setAnimationSpeed(20);
        setAnimationActive(false);

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
        // note our new piece and the current tick
        Piece opiece = _piece;
        _piece = piece;
        _tick = tick;

        // move ourselves to our new location
        if (piece.x != opiece.x || piece.y != opiece.y) {
            int elev = computeElevation(board, _piece.x, _piece.y);

            if (/* _mgr == null || */ _editorMode) {
                // if we're invisible or in the editor just warp there
                setLocation(_piece.x, _piece.y, elev);

            // TODO: append an additional path if we're currently moving
            } else if (!isMoving()) {
                Path path = createPath(board, opiece, piece);
                if (path != null) {
                    setAnimationActive(true);
                    move(path);
                } else {
                    setLocation(_piece.x, _piece.y, elev);
                }
            }
        }
    }

    /**
     * Queues up an effect for visualization on this sprite when it has
     * stopped moving.
     */
    public void queueEffect (EffectViz effect)
    {
        if (isMoving()) {
            log.info("Queueing effect [piece=" + _piece.info() +
                     ", effect=" + effect + "].");
            if (_effects == null) {
                _effects = new ArrayList<EffectViz>();
            }
            _effects.add(effect);
        } else {
            effect.display(this);
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

        // turn off our movement animation
        setAnimationActive(false);

        // if there are any queued effects, run them
        if (_effects != null) {
            for (int ii = 0; ii < _effects.size(); ii++) {
                _effects.get(ii).display(this);
            }
            _effects.clear();
        }
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
     * Creates a path that will be used to move this piece from the
     * specified old position to the new one.
     */
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        List path = null;
        if (board != null) {
            path = board.computePath(opiece, npiece.x, npiece.y);
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
                    durations[ii-1] = 0.2f;
                }
            }
            return new LineSegmentPath(this, coords, durations);

        } else {
            Vector3f start = toWorldCoords(
                opiece.x, opiece.y, 0, new Vector3f());
            Vector3f end = toWorldCoords(npiece.x, npiece.y, 0, new Vector3f());
            float duration = (float)MathUtil.distance(
                opiece.x, opiece.y, npiece.x, npiece.y) * .003f;
            return new LinePath(this, start, end, duration);
        }
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
    protected ArrayList<EffectViz> _effects;

    /** When activated, causes all pieces to warp instead of smoothly
     * follow a path. */
    protected static boolean _editorMode;

    /** Used for temporary calculations. */
    protected static Vector3f _temp = new Vector3f();

//     /** Used by {@link #_computeBounds}. */
//     protected static Rectangle _unit = new Rectangle(0, 0, SQUARE, SQUARE);
}
