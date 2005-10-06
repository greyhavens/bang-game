//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.util.MathUtil;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of a particular piece on the board.
 */
public class PieceSprite extends Sprite
{
    /**
     * Called by the editor to make pieces warp to their new locations for
     * rapid draggability.
     */
    public static void setEditorMode (boolean editorMode)
    {
        _editorMode = editorMode;
    }

    /** Returns the piece associated with this sprite. */
    public Piece getPiece ()
    {
        return _piece;
    }

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
    public void init (BasicContext ctx, SoundGroup sounds,
                      Piece piece, short tick)
    {
        _piece = piece;
        _tick = tick;

        // create our sprite geometry
        createGeometry(ctx);
        setAnimationSpeed(20);
        setAnimationActive(false);

        // create any sounds associated with this sprite
        createSounds(sounds);

        // position ourselves properly to start
        setLocation(piece.x, piece.y, 0);
        setOrientation(piece.orientation);

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
            setLocalTranslation(new Vector3f(_temp));
//             log.info("Moving to " + tx + ", " + ty + ", " + elevation +
//                      ": " + _temp);
            updateCollisionTree();
        }
    }

    /**
     * Configures this sprite's orientation.
     */
    public void setOrientation (int orientation)
    {
        // if we're moving, assume the path will do the right thing when
        // we arrive
        if (!isMoving()) {
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(ROTATIONS[orientation], UP);
            getLocalRotation().set(quat);
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
            moveSprite(board, opiece, piece);
        }

        // if we're rotated (which only happens in the editor), we need to
        // rotate our model
        if (opiece.orientation != piece.orientation) {
            setOrientation(piece.orientation);
            // now reset our location and it will adjust our centering
            int elev = computeElevation(board, _piece.x, _piece.y);
            setLocation(piece.x, piece.y, elev);
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

        // if there are any queued effects, run them
        if (_effects != null) {
            for (int ii = 0; ii < _effects.size(); ii++) {
                _effects.get(ii).display(this);
            }
            _effects.clear();
        }
    }

    /**
     * Sprites should create and attach their scene geometry by overriding
     * this method.
     */
    protected void createGeometry (BasicContext ctx)
    {
    }

    /**
     * Sprites can create and pre-load sounds they will need by overriding
     * this method.
     */
    protected void createSounds (SoundGroup sounds)
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
     * Called when a sprite has been updated with a new location. The
     * default implementation simply relocates the sprite instantly but
     * derived classes will want to compute a path and animate the sprite
     * traveling between its old and new locations.
     */
    protected void moveSprite (BangBoard board, Piece opiece, Piece npiece)
    {
        int elev = computeElevation(board, npiece.x, npiece.y);
        setLocation(npiece.x, npiece.y, elev);
    }

    /** Converts tile coordinates plus elevation into (3D) world
     * coordinates. */
    protected Vector3f toWorldCoords (int tx, int ty, int elev, Vector3f target)
    {
        target.x = tx * TILE_SIZE;
        target.y = ty * TILE_SIZE;
        target.z = elev * TILE_SIZE;
        centerWorldCoords(target);
        return target;
    }

    /** Adjusts the coordinates to the center of the piece's footprint. */
    protected void centerWorldCoords (Vector3f coords)
    {
        coords.x += TILE_SIZE/2;
        coords.y += TILE_SIZE/2;
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

    protected static float[] ROTATIONS = {
        0, // NORTH
        FastMath.PI/2, // EAST
        FastMath.PI, // SOUTH
        3*FastMath.PI/2, // WEST
    };
}
