//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;
import com.threerings.media.util.AStarPathUtil;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.PuntEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public abstract class Piece
    implements Streamable, Cloneable, Savable, DSet.Entry, PieceCodes
{
    /** Used by {@link #willShoot} */
    public static final Effect[] NO_EFFECTS = new Effect[0];

    /** Uniquely identifies each piece in the game. */
    public int pieceId;

    /** The player index of the owner of this piece or -1 if it is not an owned piece. */
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

    /** The scenarioId for this piece (of null for all). */
    public String scenId;

    /** The piece's last occupied location. */
    public transient short lastX, lastY;

    /** The pieces teleporter moves. */
    public transient PointSet teleMoves;

    /**
     * Combines the supplied x and y coordintes into a single integer.
     */
    public static int coord (int x, int y)
    {
        return (x << 16) | y;
    }

    /**
     * Returns the cost to purchase this piece.
     */
    public int getCost ()
    {
        return 100;
    }

    /**
     * Returns true if this piece is still active and playable.
     */
    public boolean isAlive ()
    {
        return (damage < 100);
    }

    /**
     * Returns the number of ticks that must elapse before this piece can again be moved.
     */
    public short ticksUntilMovable (short tick)
    {
        return (short)Math.max(0, getTicksPerMove() - (tick-lastActed));
    }

    /**
     * Called on every tick to allow a unit to lose hit points or regenerate hit points
     * automatically.
     *
     * @param tick the current game tick.
     * @param bangobj the current game object.
     * @param pieces all the pieces on the board in easily accessible form.
     *
     * @return a list of effects to apply to the unit as a result of having been ticked or null.
     */
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        return null;
    }

    /**
     * Called on a piece when it has been added to the board.
     */
    public void wasAdded (BangObject bangobj)
    {
    }

    /**
     * Called on a piece when it has been maximally damaged.
     */
    public void wasKilled (short tick)
    {
        lastActed = tick;
    }

    /**
     * Called on a piece when it has killed another player's piece (computer controlled units like
     * logging robots do not count).
     */
    public void didKill ()
    {
    }

    /**
     * Called on a piece when it has been damaged.
     */
    public void wasDamaged (int newDamage)
    {
        damage = newDamage;
    }

    /**
     * Returns whether or not this piece should be removed from the board when maximally damaged.
     */
    public boolean removeWhenDead ()
    {
        return false;
    }

    /**
     * By default we expire wreckage after some number of turns.
     */
    public boolean expireWreckage (short tick)
    {
        return (tick - lastActed > WRECKAGE_EXPIRY);
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
     * Returns true if these two pieces intersect at their current coordinates.
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

    /** Returns the width of this piece in tiles. */
    public int getWidth ()
    {
        return 1;
    }

    /** Returns the length of this piece in tiles. */
    public int getLength ()
    {
        return 1;
    }

    /** Returns the height of this piece in tiles. */
    public float getHeight ()
    {
        return 1f;
    }

    /**
     * Returns true if this piece should be removed from the board when the board is downloaded or
     * loaded from the cache (i.e., it's a marker or similar placeholder, or it's not used in the
     * current game).
     */
    public boolean removeFromBoard (BangObject bangobj)
    {
        return !isValidScenario(bangobj.scenario.getIdent());
    }

    /** Returns true if this piece is valid for this scenario. */
    public boolean isValidScenario (String scenarioId)
    {
        return (scenId == null || scenarioId == null || scenId.equals(scenarioId));
    }

    /** Returns the elevation of this piece in the board's elevation units. */
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        return computeElevation(board, tx, ty, false);
    }

    /**
     * Returns the elevation of this piece in the board's elevation units.
     *
     * @param moving is true if this elevation is part of a movement path
     */
    public int computeElevation (BangBoard board, int tx, int ty, boolean moving)
    {
        return board.getHeightfieldElevation(tx, ty);
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
        return 1;
    }

    /**
     * Returns the minimum number of tiles away that this piece can fire.
     */
    public int getMinFireDistance ()
    {
        return 1;
    }

    /**
     * Returns the maximum number of tiles away that this piece can fire.
     */
    public int getMaxFireDistance ()
    {
        return 1;
    }

    /**
     * Returns true if the board shadow should be regenerated on this piece's removal.
     */
    public boolean rebuildShadow ()
    {
        return false;
    }

    /**
     * Returns true if the specified target is in range of attack of this piece.
     */
    public boolean targetInRange (int nx, int ny, int tx, int ty)
    {
        int dist = getDistance(nx, ny, tx, ty);
        return (dist >= getMinFireDistance() && dist <= getMaxFireDistance());
    }

    /**
     * Returns the "tile" distance between this and the specified piece.
     */
    public int getDistance (Piece other)
    {
        return (other == null) ? Integer.MAX_VALUE : getDistance(other.x, other.y);
    }

    /**
     * Returns the "tile" distance between this piece and the specified location.
     */
    public int getDistance (int tx, int ty)
    {
        return getDistance(x, y, tx, ty);
    }

    /**
     * Returns the Manhattan distance between two points.
     */
    public static int getDistance (int x, int y, int tx, int ty)
    {
        return Math.abs(x - tx) + Math.abs(y - ty);
    }

    /**
     * Gets the cost of traversing this category of terrain in tenths of a movement point.
     */
    public int traversalCost (TerrainConfig terrain)
    {
        return terrain.traversalCost;
    }

    /**
     * Returns a translatable name for this piece (or <code>null</code> if none exists).
     */
    public String getName ()
    {
        return null;
    }

    /**
     * Returns the stepper used to compute paths for this type of piece.
     */
    public AStarPathUtil.Stepper getStepper ()
    {
        return _pieceStepper;
    }

    /**
     * Allows the piece to do any necessary initialization before the game starts.
     */
    public void init ()
    {
        // start with zero damage
        damage = 0;
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
     * Instructs the piece to rotate clockwise if direction is {@link Piece#CW} and
     * counter-clockwise if it is {@link Piece#CCW}.
     *
     * @return true if rotation is supported and the piece rotated, false if it is not supported
     * (pieces longer than one segment cannot be rotated).
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
     * Returns true if this piece can pass non-traversable tiles during movement, false otherwise.
     */
    public boolean isFlyer ()
    {
        return false;
    }

    /**
     * Returns true if this piece can remain on non-traversable tiles after movement, false
     * otherwise.
     */
    public boolean isAirborne ()
    {
        return false;
    }

    /**
     * Returns true if this piece cannot travel through other pieces.
     */
    public boolean isCorporeal ()
    {
        return true;
    }

    /**
     * Returns true if this piece can be pushed due to an attack.
     */
    public boolean canBePushed ()
    {
        return false;
    }

    /**
     * Selects the shortest move that puts us within range of firing on the specified target.
     *
     * @param any if true we don't care about the best shot location, just that there is at least
     * one valid shot location.
     */
    public Point computeShotLocation (BangBoard board, Piece target, PointSet moveSet, boolean any)
    {
        return computeShotLocation(board, target, moveSet, any, new PointSet());
    }

    /**
     * Selects the shortest move that puts us within range of firing on the specified target.
     *
     * @param any if true we don't care about the best shot location, just that there is at least
     * one valid shot location.
     * @param preferredSet a set of points we'd prefer to move to
     */
    public Point computeShotLocation (BangBoard board, Piece target, PointSet moveSet,
                                      boolean any, PointSet preferredSet)
    {
        int minfdist = getMinFireDistance(), maxfdist = getMaxFireDistance();
        int moves = Integer.MAX_VALUE, pmoves = Integer.MAX_VALUE;

        Point spot = null, prefer = null;

        // first check if we can fire without moving (assuming our current location is in our move
        // set)
        if (moveSet.contains(x, y)) {
            int tdist = target.getDistance(x, y);
            if (tdist >= minfdist && tdist <= maxfdist && checkLineOfSight(board, x, y, target)) {
                spot = new Point(x, y);
                if (preferredSet.isEmpty() || preferredSet.contains(x, y) || any) {
                    return spot;
                }
                moves = 0;
            }
        }

        // next search the move set for the closest location
        for (int ii = 0, ll = moveSet.size(); ii < ll; ii++) {
            int px = moveSet.getX(ii), py = moveSet.getY(ii);
            int dist = getDistance(px, py);
            int tdist = target.getDistance(px, py);
            if (tdist >= minfdist && tdist <= maxfdist && checkLineOfSight(board, px, py, target)) {
                if (dist < pmoves && preferredSet.contains(px, py)) {
                    pmoves = dist;
                    if (prefer == null) {
                        prefer = new Point();
                    }
                    prefer.setLocation(px, py);
                } else if (dist < moves && prefer == null) {
                    moves = dist;
                    if (spot == null) {
                        spot = new Point();
                    }
                    spot.setLocation(px, py);
                } else {
                    continue;
                }
                if (any) {
                    break;
                }
            }
        }

        return (prefer != null) ? prefer : spot;
    }

    /**
     * Returns a PointSet with all the tiles that could be shot by this unit based on the supplied
     * coordinate.
     */
    public PointSet computeShotRange (BangBoard board, int dx, int dy)
    {
        PointSet ps = new PointSet();
        int minfdist = getMinFireDistance(), maxfdist = getMaxFireDistance();
        int x1 = dx - maxfdist, x2 = dx + maxfdist, y1 = dy - maxfdist, y2 = dy + maxfdist;
        Rectangle playarea = board.getPlayableArea();
        for (int xx = x1; xx <= x2; xx++) {
            for (int yy = y1; yy <= y2; yy++) {
                int dist = getDistance(dx, dy, xx, yy);
                if (dist < minfdist || dist > maxfdist || !playarea.contains(xx, yy)) {
                    continue;
                }
                if (checkLineOfSight(board, dx, dy, xx, yy)) {
                    ps.add(xx, yy);
                }
            }
        }
        return ps;
    }

    /**
     * Creates any effects that must be applied prior to applying the {@link ShotEffect} that
     * results from this piece shooting another.
     */
    public Effect[] willShoot (BangObject bangobj, Piece target, ShotEffect shot)
    {
        return NO_EFFECTS;
    }

    /**
     * Creates an effect that will "shoot" the specified target piece.
     *
     * @param scale a value that should be used to scale the damage done.
     */
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        // create a basic shot effect
        int damage = computeScaledDamage(bangobj, target, scale);
        ShotEffect shot = generateShotEffect(bangobj, target, damage);
        // give the target a chance to deflect the shot
        return target.deflect(bangobj, this, shot, scale);
    }

    /**
     * Gives a unit a chance to "deflect" a shot, by replacing a normal shot effect with one
     * deflected to a different location.
     *
     * @return the original effect if no deflection is desired or a new shot effect that has been
     * properly deflected.
     */
    public ShotEffect deflect (BangObject bangobj, Piece shooter, ShotEffect effect, float scale)
    {
        // default is no deflection
        return effect;
    }

    /**
     * Returns true if this piece will deflect the shot.
     */
    public boolean willDeflect (BangObject bangobj, Piece shooter)
    {
        return false;
    }

    /**
     * When a unit shoots another piece, the unit may also do collateral damage to nearby units.
     * This method should return effects indicating such damage. <em>Note:</em> the piece is
     * responsible for calling {@link Effect#init} on those effects before returning them.
     */
    public Effect[] collateralDamage (BangObject bangobj, Piece target, int damage)
    {
        return null;
    }

    /**
     * If a target returns fire when shot, this method should return the appropriate shot effect to
     * enforce that.
     *
     * @param damage the amount of damage done by the initial shooter (the piece may or may not
     * account for this when returning fire).
     */
    public ShotEffect returnFire (BangObject bangobj, Piece shooter, int damage)
    {
        return null;
    }

    /**
     * Allows the piece to produce an effect to deploy immediately before it dies.
     *
     * @param shooterId the id of the piece shooting or otherwise damaging this piece, or
     * <code>-1</code> for none
     */
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        return null;
    }

    /**
     * Called after a piece dies to see if there are any post death effects.
     */
    public Effect didDie (BangObject bangobj)
    {
        return null;
    }

    /**
     * Called when a piece dies to see if they crash into anything.
     */
    public Point maybeCrash (BangObject bangobj, int shooter)
    {
        return null;
    }

    /**
     * Returns true if this piece prevents other pieces from occupying the same square, or false if
     * it can colocate.
     */
    public boolean preventsOverlap (Piece lapper)
    {
        return true;
    }

    /**
     * Some pieces interact with other pieces, which takes place via this method. An effect should
     * be returned communicating the nature of the interaction.
     */
    public Effect[] maybeInteract (BangObject bangobj, Piece other)
    {
        if (other instanceof Bonus) {
            Bonus bonus = (Bonus)other;
            if (!bonus.getConfig().hidden) {
                return new Effect[] { PuntEffect.puntBonus(bangobj, bonus, pieceId) };
            }
        }
        return NO_EFFECTS;
    }

    /**
     * Determines whether the specified moving piece will help achieve the scenario's goals by
     * moving onto or next to this piece.
     *
     * @return -1 for no relevance, 0 if the mover scores by landing on this piece, or +1 if the
     * mover scores by landing next to this piece
     */
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return -1;
    }

    /**
     * Determines whether the specified moving piece can attain its goal if the goal radius is +1
     * even if there is a fence in the way.
     */
    public boolean getFenceBlocksGoal ()
    {
        return false;
    }

    // documentation inherited from interface Savable
    public Class<?> getClassTag ()
    {
        return getClass();
    }

    // documentation inherited from interface Savable
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        pieceId = capsule.readInt("pieceId", 0);
        x = capsule.readShort("x", (short)0);
        y = capsule.readShort("y", (short)0);
        orientation = capsule.readShort("orientation", (short)0);
        scenId = capsule.readString("scenId", null);
    }

    // documentation inherited from interface Savable
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(pieceId, "pieceId", 0);
        capsule.write(x, "x", (short)0);
        capsule.write(y, "y", (short)0);
        capsule.write(orientation, "orientation", (short)0);
        capsule.write(scenId, "scenId", null);
    }

    /**
     * Creates the appropriate derivation of {@link PieceSprite} to render this piece.
     */
    public PieceSprite createSprite ()
    {
        return new PieceSprite();
    }

    /**
     * Return true if we want to updated the sprite every tick.
     */
    public boolean updateSpriteOnTick ()
    {
        return false;
    }

    /**
     * This is normally not needed, but is used by the editor to assign piece IDs to new pieces.
     */
    public void assignPieceId (BangObject bangobj)
    {
        _key = null;
        pieceId = ++bangobj.maxPieceId;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        if (_key == null) {
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
        return other instanceof Piece && pieceId == ((Piece)other).pieceId;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    /** Converts our orientation to a human readable string. */
    public String orientationToString ()
    {
        return (orientation >= 0) ? ORIENT_CODES[orientation] : ("" + orientation);
    }

    /**
     * Generates a brief string description of this piece.
     */
    public String toString ()
    {
        return infoType() + " id:" + pieceId + " o:" + owner +
            " x:" + x + " y:" + y + " d:" + damage;
    }

    /**
     * Generates a brief string description of this piece.
     */
    public String toFullString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Returns true if we can and should fire upon this target. Note that this does not check to
     * see whether the target is in range.
     */
    public boolean validTarget (BangObject bangobj, Piece target, boolean allowSelf)
    {
        boolean valid = (isAlive() && target.isTargetable() && target.isAlive());
        if (valid && !allowSelf) {
            valid = !target.isSameTeam(bangobj, this);
        }
        return valid;
    }

    /**
     * Returns true if this piece can be targeted.
     */
    public boolean isTargetable ()
    {
        return false;
    }

    /**
     * Returns true if this piece may at some point be targetable.
     */
    public boolean willBeTargetable ()
    {
        return isTargetable();
    }

    /**
     * Returns a specialized handler for non-scenario, non-team piece ai.
     */
    public String getLogic ()
    {
       return null;
    }

    /**
     * Returns the team of this piece's owner or -1 if it has no owner or its owner is not on a
     * team.
     */
    public int getTeam (BangObject bangobj)
    {
        return bangobj.getTeam(owner);
    }

    /**
     * Returns true if this piece is on the same team as the target.
     */
    public boolean isSameTeam (BangObject bangobj, Piece target)
    {
        int team = getTeam(bangobj);
        return (team == -1 ? target.owner == owner : target.getTeam(bangobj) == team);
    }

    /**
     * Use this to update a piece owner so the team will always stay in sync.
     */
    public void setOwner (BangObject bangobj, int owner)
    {
        this.owner = owner;
    }

    /**
     * Determines whether this piece has the necessary line of sight to fire upon the specified
     * target from the given location.
     */
    public boolean checkLineOfSight (
        BangBoard board, int tx, int ty, Piece target)
    {
        int units = board.getElevationUnitsPerTile();
        int e1 = computeElevation(board, tx, ty) + (int)(getHeight()*0.5f*units);
        int e2 = target.computeElevation(board, target.x, target.y) +
            (int)(target.getHeight()*0.5f*units);
        return board.checkLineOfSight(tx, ty, e1, target.x, target.y, e2);
    }

    /**
     * Determines whether this piece has the necessary line of sight to fire upon the specified
     * tile from the given location.
     */
    public boolean checkLineOfSight (BangBoard board, int tx, int ty, int dx, int dy)
    {
        int units = board.getElevationUnitsPerTile();
        int e1 = computeElevation(board, tx, ty) + (int)(getHeight()*0.5f*units);
        int e2 = computeElevation(board, dx, dy) + (int)(getHeight()*0.5f*units);
        return board.checkLineOfSight(tx, ty, e1, dx, dy, e2);
    }

    /**
     * Computes the actual damage done if this piece were to fire on the specified target,
     * accounting for this piece's current damage level and other limiting factors.
     *
     * @param scale a value that should be used to scale the damage after all other factors have
     * been considered.
     */
    public int computeScaledDamage (BangObject bangobj, Piece target, float scale)
    {
        // compute the damage we're doing to this piece
        int ddamage = computeDamage(target);

        // scale the damage by our own damage level; but always fire as if we have at least half
        // hit points
        int undamage = Math.max(50, 100-damage);
        ddamage = (ddamage * undamage) / 100;

        // account for any other pieces which have attack adjustments
        if (bangobj != null) {
            _attackIcons = new ArrayList<String>();
            for (Piece p : bangobj.pieces) {
                ddamage = p.adjustPieceAttack(this, ddamage, _attackIcons);
            }
        }

        // account for any influences on the attacker or defender
        ddamage = adjustAttack(target, ddamage);
        ddamage = Math.max(0, target.adjustDefend(this, ddamage));

        // finally scale the damage by the desired value
        return Math.round(scale * ddamage);
    }

    /**
     * Adjusts the attack of other pieces.
     */
    public int adjustPieceAttack (Piece attacker, int damage, ArrayList<String> attackIcons)
    {
        // by default do nothing
        return damage;
    }

    /**
     * Returns the attack influence icon or null if no attack influence.  Used after a call to
     * {@link #computeScaledDamage}.
     */
    public String[] attackInfluenceIcons ()
    {
        return null;
    }

    /**
     * Returns the defend influence icon or null if no attack influence.  Used after a call to
     * {@link #computeScaledDamage}.
     */
    public String[] defendInfluenceIcons (Piece target)
    {
        if (!(target instanceof Unit)) {
            return null;
        }

        Unit unit = (Unit)target;
        ArrayList<String> icons = new ArrayList<String>();
        for (Influence influence : unit.getInfluences()) {
            if (influence != null && influence.didAdjustDefend()) {
                icons.add(influence.getName());
            }
        }
        if (icons.size() > 0) {
            return icons.toArray(new String[icons.size()]);
        }
        return null;
    }

    /**
     * Called on both client and server to notify the piece that it moved of its own volition.
     */
    public void didMove (int steps)
    {
    }

    /**
     * Returns the frequency with which this piece can move.
     */
    protected int getTicksPerMove ()
    {
        return 4;
    }

    /**
     * Computes the new orientation for this piece were it to travel from its current coordinates
     * to the specified coordinates.
     */
    protected int computeOrientation (int nx, int ny)
    {
        int hx = x, hy = y;

        // if it is purely a horizontal or vertical move, simply orient in the direction of the
        // move
        if (nx == hx) {
            return (ny > hy) ? SOUTH : NORTH;
        } else if (ny == hy) {
            return (nx > hx) ? EAST : WEST;
        }

        // otherwise try to behave naturally: moving forward first if possible and turning sensibly
        // to reach locations behind us
        switch (orientation) {
        case NORTH: return (ny < hy) ? ((nx > hx) ? EAST : WEST) : SOUTH;
        case SOUTH: return (ny > hy) ? ((nx > hx) ? EAST : WEST) : NORTH;
        case EAST:  return (nx > hx) ? ((ny > hy) ? SOUTH : NORTH) : WEST;
        case WEST:  return (nx < hx) ? ((ny > hy) ? SOUTH : NORTH) : EAST;
        default: return NORTH; // erm, this shouldn't happen
        }
    }

    /**
     * Called by {@link #position} after it has confirmed that we are in fact changing position and
     * not NOOPing or setting our location for the first time. Derived pieces that want to
     * customize their position handling should override this method.
     */
    protected void updatePosition (int nx, int ny)
    {
        // determine our new orientation
        orientation = (short)computeOrientation(nx, ny);
        lastX = x;
        lastY = y;
        x = (short)nx;
        y = (short)ny;
    }

    /**
     * Called to allow derived classes to update their bounds when the piece has been repositioned
     * or reoriented.
     */
    protected void recomputeBounds ()
    {
    }

    /** Helper function for {@link #info}. */
    protected String infoType ()
    {
        String cname = getClass().getName();
        return cname.substring(cname.lastIndexOf(".")+1);
    }

    /**
     * Returns the number of percentage points of damage this piece does to pieces of the specified
     * type.
     */
    protected int computeDamage (Piece target)
    {
        log.warning(getClass() + " requested to damage " + target.getClass() + "?");
        return 10;
    }

    /**
     * Performs any necessary adjustments to this piece's attack.
     */
    protected int adjustAttack (Piece target, int damage)
    {
        return damage;
    }

    /**
     * Performs any necessary adjustments to this piece's defense.
     */
    protected int adjustDefend (Piece shooter, int damage)
    {
        return damage;
    }

    /**
     * Performs any necessary adjustments to this piece's proximity defense.
     */
    protected int adjustProxDefend (Piece target, int damage)
    {
        return damage;
    }

    /**
     * Generate a shot effect for this piece.
     */
    protected ShotEffect generateShotEffect (BangObject bangobj, Piece target, int damage)
    {
        return new ShotEffect(
            this, target, damage, attackInfluenceIcons(), defendInfluenceIcons(target));
    }

    protected transient Integer _key;

    protected transient ArrayList<String> _attackIcons;

    /** The default path-finding stepper. Allows movement in one of the four directions. */
    protected static AStarPathUtil.Stepper _pieceStepper = new AStarPathUtil.Stepper() {
        public void considerSteps (int x, int y) {
	    considerStep(x, y - 1, 1);
	    considerStep(x - 1, y, 1);
	    considerStep(x + 1, y, 1);
	    considerStep(x, y + 1, 1);
        }
    };

    /** The number of ticks until wreckage expires. */
    protected static final int WRECKAGE_EXPIRY = 6;

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
