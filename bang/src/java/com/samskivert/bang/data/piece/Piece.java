//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.media.util.AStarPathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.presents.dobj.DSet;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.Terrain;
import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.effect.ShotEffect;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

import static com.samskivert.bang.Log.log;

/**
 * Does something extraordinary.
 */
public abstract class Piece extends SimpleStreamableObject
    implements Cloneable, DSet.Entry, PieceCodes
{
    /** Used by {@link #checkSet} and other piece considerers. */
    public static interface Predicate
    {
        public boolean matches (Piece piece);
    }

    /** Used by {@link #maybeInteract}. */
    public enum Interaction { CONSUMED, ENTERED, INTERACTED, NOTHING };

    /** Uniquely identifies each piece in the game. */
    public int pieceId;

    /** The player index of the owner of this piece or -1 if it is not an
     * owned piece. */
    public int owner = -1;

    /** The tick on which this piece last acted. */
    public short lastActed;

    /** The current x location of this piece's segments. */
    public short x;

    /** The current y location of this piece's segments. */
    public short y;

    /** This piece's orientation. */
    public short orientation;

    /** The percentage damage this piece has taken. */
    public int damage;

    /** The energy level of this piece. */
    public int energy;

    /**
     * Returns true if this piece is still active and playable.
     */
    public boolean isAlive ()
    {
        return (energy > 0) && (damage < 100);
    }

    /**
     * Returns the number of ticks that must elapse before this piece can
     * again be moved.
     */
    public short ticksUntilMovable (short tick)
    {
        return (short)Math.max(0, getTicksPerMove() - (tick-lastActed));
    }

    /**
     * Returns the number of ticks that must elapse before this piece can
     * again be fired.
     */
    public short ticksUntilFirable (short tick)
    {
        return (short)Math.max(0, getTicksPerFire() - (tick-lastActed));
    }

    /**
     * Returns whether or not this piece should be removed from the board
     * when maximally damaged.
     */
    public boolean removeWhenDead ()
    {
        return false;
    }

    /**
     * Returns true if the specified coordinates intersect this piece.
     */
    public boolean intersects (int tx, int ty)
    {
        return (x == tx) && (y == ty);
    }

    /**
     * Returns true if this piece intersects the specified region.
     */
    public boolean intersects (Rectangle bounds)
    {
        return bounds.contains(x, y);
    }

    /**
     * Returns true if these two pieces intersect at their current
     * coordinates.
     */
    public boolean intersects (Piece other)
    {
        return other.intersects(x, y);
    }

    /**
     * Returns our combined x and y coordinate.
     */
    public int getCoord ()
    {
        return coord(x, y);
    }

    /** Returns the width of this piece. */
    public int getWidth ()
    {
        return 1;
    }

    /** Returns the height of this piece. */
    public int getHeight ()
    {
        return 1;
    }

    /** Returns the number of tiles that this piece can "see". */
    public int getSightDistance ()
    {
        return 5;
    }

    /**
     * Returns the number of tiles that this piece can move.
     */
    public int getMoveDistance ()
    {
        return 0;
    }

    /**
     * Returns the number of tiles away that this piece can fire.
     */
    public int getFireDistance ()
    {
        return 1;
    }

    /**
     * Gets the cost of traversing this terrain in tenths of a movement
     * point.
     */
    public int traversalCost (Terrain terrain)
    {
        return 10;
    }

    /** Returns a brief description of this piece. */
    public String info ()
    {
        String cname = getClass().getName();
        return cname.substring(cname.lastIndexOf(".")+1) + ":" +
            pieceId + "@" + x + "/" + y;
    }

    /** Returns the stepper used to compute paths for this type of piece. */
    public AStarPathUtil.Stepper getStepper ()
    {
        return _pieceStepper;
    }

    /**
     * Allows the piece to do any necessary initialization before the game
     * starts.
     */
    public void init ()
    {
        // set up our starting energy (only if it hasn't been otherwise
        // configured in the editor)
        if (energy == 0) {
            energy = startingEnergy();
        }

        // start with a random last moved tick
        lastActed = (short)(-1 * RandomUtil.getInt(getTicksPerMove()+1));
    }

    /**
     * Updates this pieces position and orientation.
     *
     * @return true if the piece's position changed, false if not.
     */
    public boolean position (int nx, int ny)
    {
        // avoid NOOP
        if (nx != x || ny != y) {
            updatePosition(nx, ny);
            recomputeBounds();
            return true;
        }
        return false;
    }

    /**
     * Instructs the piece to rotate clockwise if direction is {@link Piece#CW}
     * and counter-clockwise if it is {@link Piece#CCW}.
     *
     * @return true if rotation is supported and the piece rotated, false
     * if it is not supported (pieces longer than one segment cannot be
     * rotated).
     */
    public boolean rotate (int direction)
    {
        // update our orientation
        orientation = (short)((direction == CW) ? ((orientation + 1) % 4) :
                              ((orientation + 3) % 4));
        recomputeBounds();
        return true;
    }

    /**
     * Returns true if this piece is a flying piece, false if it is a
     * walking piece.
     */
    public boolean isFlyer ()
    {
        return false;
    }

    /**
     * Returns the energy consumed per step taken by this piece.
     */
    public int energyPerStep ()
    {
        return DEFAULT_ENERGY_PER_STEP;
    }

    /**
     * Instructs this piece to consume the energy needed to take the
     * specified number of steps.
     */
    public void consumeEnergy (int steps)
    {
        energy -= energyPerStep() * steps;
    }

    /**
     * Returns true if this piece has at least enough energy to take one
     * step, false if not.
     */
    public boolean canTakeStep ()
    {
        return energy >= energyPerStep();
    }

//     /**
//      * Returns true if this piece can consolidate this additional move
//      * into a single turn, receiving a "bonus" move for that turn.
//      */
//     public boolean canBonusMove (int x, int y)
//     {
//         return false;
//     }

    /**
     * Affects the target piece with damage.
     */
    public ShotEffect shoot (Piece target)
    {
        int hurt = computeDamage(target);
        // TEMP: scale all damage up
        hurt = 5 * hurt / 3;
        // scale the damage by our own damage level
        hurt = (hurt * (100-this.damage)) / 100;
        hurt = Math.max(1, hurt); // always do at least 1 point of damage
        ShotEffect shot = new ShotEffect();
        shot.shooterId = pieceId;
        shot.targetId = target.pieceId;
        shot.damage = hurt;
        log.info("Bang! " + shot);
        return shot;
    }

    /**
     * Returns true if this piece prevents other pieces from occupying the
     * same square, or false if it can colocate.
     */
    public boolean preventsOverlap (Piece lapper)
    {
        return true;
    }

    /**
     * Some pieces interact with other pieces, which takes place via this
     * method. Depending on the type of interaction, the piece can
     * indicate that it consumed the other piece, was consumed by it
     * (entered), simply interacted with it resulting in both pieces being
     * changed or did nothing at all. If the interaction results in an
     * effect being produced, the effect should be appended to the
     * supplied list.
     */
    public Interaction maybeInteract (Piece other, ArrayList<Effect> effects)
    {
        if (other instanceof Fuel && energy < 3*maximumEnergy()/4) {
            Fuel nibbly = (Fuel)other;
            int taken = nibbly.takeEnergy(this);
            energy = Math.min(maximumEnergy(), energy + taken);
            return nibbly.energy > 0 ?
                Interaction.INTERACTED : Interaction.CONSUMED;

        } else if (other instanceof Bonus) {
            Effect effect = ((Bonus)other).affect(this);
            if (effect != null) {
                effects.add(effect);
            }
            return Interaction.CONSUMED;
        }

        return Interaction.NOTHING;
    }

    /**
     * Returns true if this piece can traverse the board at the specified
     * coordinates.
     */
    public boolean canTraverse (BangBoard board, int tx, int ty)
    {
        return canTraverse(board.getTile(tx, ty));
    }

    /**
     * Creates the appropriate derivation of {@link PieceSprite} to render
     * this piece.
     */
    public PieceSprite createSprite ()
    {
        return new PieceSprite();
    }

    /**
     * This is normally not needed, but is used by the editor to assign
     * piece IDs to new pieces.
     */
    public void assignPieceId ()
    {
        _key = null;
        pieceId = 0;
        getKey();
    }

    /** Returns the percentage remaining of this piece's energy. */
    public int getPercentEnergy ()
    {
        return energy * 100 / maximumEnergy();
    }

    /**
     * Creates a new piece that is an exact duplicate of this piece. The
     * piece will occupy the same location and must be moved before being
     * added to the game.
     */
    public Piece duplicate ()
    {
        Piece dup = (Piece)clone();
        dup.assignPieceId();
        return dup;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            if (pieceId == 0) {
                pieceId = ++_nextPieceId;
            }
            _key = new Integer(pieceId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return pieceId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return pieceId == ((Piece)other).pieceId;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return (Piece)super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /** Converts our orientation to a human readable string. */
    public String orientationToString ()
    {
        return (orientation >= 0) ? ORIENT_CODES[orientation] :
            ("" + orientation);
    }

    /** Returns true if we can and should fire upon this target. */
    public boolean validTarget (Piece target)
    {
        return (target != null && target.owner != -1 &&
                target.owner != owner && target.energy > 0 &&
                target.damage < 100);
    }

    /** Returns the frequency with which this piece can move. */
    protected int getTicksPerMove ()
    {
        return 4;
    }

    /** Returns the frequency with which this piece can fire. */
    protected int getTicksPerFire ()
    {
        return 4;
    }

    /**
     * Computes the new orientation for this piece were it to travel from
     * its current coordinates to the specified coordinates.
     */
    protected int computeOrientation (int nx, int ny)
    {
        int hx = x, hy = y;

        // if it is purely a horizontal or vertical move, simply orient
        // in the direction of the move
        if (nx == hx) {
            return (ny > hy) ? SOUTH : NORTH;
        } else if (ny == hy) {
            return (nx > hx) ? EAST : WEST;
        }

        // otherwise try to behave naturally: moving forward first if
        // possible and turning sensibly to reach locations behind us
        switch (orientation) {
        case NORTH: return (ny < hy) ? ((nx > hx) ? EAST : WEST) : SOUTH;
        case SOUTH: return (ny > hy) ? ((nx > hx) ? EAST : WEST) : NORTH;
        case EAST:  return (nx > hx) ? ((ny > hy) ? SOUTH : NORTH) : WEST;
        case WEST:  return (nx < hx) ? ((ny > hy) ? SOUTH : NORTH) : EAST;
        // erm, this shouldn't happen
        default: return NORTH;
        }
    }

    /**
     * Called by {@link #position} after it has confirmed that we are in
     * fact changing position and not NOOPing or setting our location for
     * the first time. Derived pieces that want to customize their
     * position handling should override this method.
     */
    protected void updatePosition (int nx, int ny)
    {
        // determine our new orientation
        orientation = (short)computeOrientation(nx, ny);
        x = (short)nx;
        y = (short)ny;
    }

    /**
     * Called to allow derived classes to update their bounds when the
     * piece has been repositioned or reoriented.
     */
    protected void recomputeBounds ()
    {
    }

    /**
     * Returns true if this piece can traverse the specified type of
     * terrain.
     */
    protected boolean canTraverse (Terrain terrain)
    {
        return (terrain == Terrain.DIRT ||
                terrain == Terrain.LEAF_BRIDGE);
    }

    /** Returns the starting energy for pieces of this type. */
    protected int startingEnergy ()
    {
        return DEFAULT_STARTING_ENERGY * 50;
    }

    /** Returns the maximum energy this piece can possess. */
    protected int maximumEnergy ()
    {
        return DEFAULT_MAXIMUM_ENERGY * 50;
    }

    /**
     * Returns the number of percentage points of damage this piece does
     * to pieces of the specified type.
     */
    protected int computeDamage (Piece target)
    {
        log.warning(getClass() + " requested to damage " +
                    target.getClass() + "?");
        return 10;
    }

    /**
     * Combines the supplied x and y coordintes into a single integer.
     */
    public static int coord (short x, short y)
    {
        return (x << 16) | y;
    }

    protected transient Integer _key;

    /** Used to assign a unique id to each piece. */
    protected static int _nextPieceId;

    /** The default path-finding stepper. Allows movement in one of the
     * four directions. */
    protected static AStarPathUtil.Stepper _pieceStepper =
        new AStarPathUtil.Stepper() {
        public void considerSteps (int x, int y)
        {
	    considerStep(x, y - 1, 1);
	    considerStep(x - 1, y, 1);
	    considerStep(x + 1, y, 1);
	    considerStep(x, y + 1, 1);
        }
    };

    /** The default quantity of energy consumed to take a step. */
    protected static final int DEFAULT_ENERGY_PER_STEP = 10;

    /** The default starting quantity of energy. */
    protected static final int DEFAULT_STARTING_ENERGY = 100;

    /** The default maximum quantity of energy. */
    protected static final int DEFAULT_MAXIMUM_ENERGY = 250;

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_X_MAP = { 0, 1, 0, -1 };

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_Y_MAP = { -1, 0, 1, 0 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_X_MAP = { 0, -1, 0, 1 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_Y_MAP = { 1, 0, -1, 0 };

    /** Used by {@link #orientationToString}. */
    protected static final String[] ORIENT_CODES = { "N", "E", "S", "W" };
}
