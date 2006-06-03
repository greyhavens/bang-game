//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ExpireInfluenceEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * A base piece type for player units.
 */
public class Unit extends Piece
{
    /** The player to whom this unit will return on respawn or -1 if it should
     * not be respawned. */
    public int originalOwner = -1;

    /** Any influence currently acting on this unit. */
    public Influence influence;

    /** Indicates whether this unit is carrying a nugget. */
    public boolean benuggeted;

    /**
     * Instantiates a unit of the specified type.
     */
    public static Unit getUnit (String type)
    {
        UnitConfig config = UnitConfig.getConfig(type);
        Unit unit = null;
        try {
            if (config.unitClass != null) {
                unit = (Unit)Class.forName(config.unitClass).newInstance();
            } else {
                unit = new Unit();
            }
            unit.init(config);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create unit [type=" + type +
                    ", class=" + config.unitClass + "].", e);
        }
        return unit;
    }

    /**
     * Configures this piece with a new influence.
     */
    public void setInfluence (Influence influence, short tick)
    {
        influence.init(tick);
        this.influence = influence;
    }

    /** Returns the type of the unit. */
    public String getType ()
    {
        return _config.type;
    }

    /** Returns our unit configuration. */
    public UnitConfig getConfig ()
    {
        return _config;
    }

    /**
     * Creates a new unit that is an exact duplicate of this unit, unless
     * this unit is a Big Shot in which case a suitable substitute type is
     * created. The unit will occupy a default location and must be moved
     * before being added to the game.
     */
    public Unit duplicate (BangObject bangobj)
    {
        Unit dup = getUnit(_config.dupeType);
        dup.owner = owner;
        dup.lastActed = lastActed;
        dup.damage = damage;
        dup.assignPieceId(bangobj);
        return dup;
    }

    /**
     * Returns the tick on which this unit should respawn or -1 if it
     * should not be respawned.
     */
    public short getRespawnTick ()
    {
        return _respawnTick;
    }

    /**
     * Configures this unit with a respawn tick.
     */
    public void setRespawnTick (short tick)
    {
        _respawnTick = tick;
    }

    /**
     * Indicates whether or not this piece can activate bonuses.
     */
    public boolean canActivateBonus (Bonus bonus)
    {
        return isAlive() &&
            (bonus.getConfig().type.equals(NuggetEffect.NUGGET_BONUS) ?
             !benuggeted : true);
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        init(UnitConfig.getConfig(in.readUTF()));
    }

    /** Writes some custom information for this piece. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeUTF(_config.type);
    }

    /**
     * Returns the difference between the base damage and damage
     * after modifiers.
     */
    public int computeDamageDiff (Piece target)
    {
        return computeDamage(target) - _config.damage;
    }

    @Override // documentation inherited
    public void init ()
    {
        super.init();

        // configure our last acted tick according to our initiative
        lastActed = (short)(-1 * _config.initiative);
    }

    @Override // documentation inherited
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        if (isFlyer() && isAlive()) {
            return computeFlightElevation(board, tx, ty);
        } else {
            return super.computeElevation(board, tx, ty);
        }
    }
    
    /**
     * Computes the elevation of for a flying unit.
     */
    public int computeFlightElevation (BangBoard board, int tx, int ty)
    {
        int groundel = Math.max(board.getWaterLevel(),
            super.computeElevation(board, tx, ty)) +
                (int)(FLYER_GROUND_HEIGHT * board.getElevationUnitsPerTile()),
            propel = board.getElevation(tx, ty) +
                (int)(FLYER_PROP_HEIGHT * board.getElevationUnitsPerTile());
        return Math.max(groundel, propel);
    }
    
    @Override // documentation inherited
    public boolean checkLineOfSight (
        BangBoard board, int tx, int ty, Piece target)
    {
        // range units are not restricted to line of sight
        return (_config.mode == UnitConfig.Mode.RANGE ||
            super.checkLineOfSight(board, tx, ty, target));
    }
    
    @Override // documentation inherited
    public Effect willShoot (BangObject bangobj, Piece target, ShotEffect shot)
    {
        return null;
    }

    @Override // documentation inherited
    public Effect maybeInteract (Piece other)
    {
        if (other instanceof Bonus && canActivateBonus((Bonus)other)) {
            return ((Bonus)other).affect(this);
        }
        return super.maybeInteract(other);
    }

    @Override // documentation inherited
    public ShotEffect returnFire (
        BangObject bangobj, Piece shooter, int newDamage)
    {
        ShotEffect shot = null;
        int oldDamage = this.damage;
        if (_config.returnFire > 0 && newDamage < 100 &&
            targetInRange(shooter.x, shooter.y)) {
            // return fire shots are always executed at 75% health
            this.damage = 75;
            shot = shoot(bangobj, shooter);
            shot.type = ShotEffect.RETURN_FIRE;
            this.damage = oldDamage;
            // scale the damage if so specified in the unit config
            shot.newDamage = (_config.returnFire * shot.newDamage) / 100;
        }
        return shot;
    }

    @Override // documentation inherited
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        return benuggeted ?
            NuggetEffect.dropNugget(bangobj, this, shooterId) : null;
    }
    
    @Override // documentation inherited
    public Effect tick (short tick, BangBoard board, Piece[] pieces)
    {
        if (influence != null && influence.isExpired(tick)) {
            ExpireInfluenceEffect effect = new ExpireInfluenceEffect();
            effect.init(this);
            return effect;
        }
        return null;
    }

    @Override // documentation inherited
    protected int getTicksPerMove ()
    {
        return (influence == null) ? super.getTicksPerMove() :
            influence.adjustTicksPerMove(super.getTicksPerMove());
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return (influence == null) ? _config.sightDistance :
            influence.adjustSightDistance(_config.sightDistance);
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return (influence == null) ? _config.moveDistance :
            influence.adjustMoveDistance(_config.moveDistance);
    }

    @Override // documentation inherited
    public int getMinFireDistance ()
    {
        return (influence == null) ? _config.minFireDistance :
            influence.adjustMinFireDistance(_config.minFireDistance);
    }

    @Override // documentation inherited
    public int getMaxFireDistance ()
    {
        return (influence == null) ? _config.maxFireDistance :
            influence.adjustMaxFireDistance(_config.maxFireDistance);
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return _config.make == UnitConfig.Make.HUMAN;
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target, boolean allowSelf)
    {
        // if we do no damage to this type of target, it is not valid
        return super.validTarget(target, allowSelf) &&
            (computeDamage(target) > 0);
    }

    @Override // documentation inherited
    public boolean isFlyer ()
    {
        return _config.mode == UnitConfig.Mode.AIR;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite(_config.type);
    }

    @Override // documentation inherited
    public int traversalCost (TerrainConfig terrain)
    {
        int cost;
        // flyers are unaffected by terrain adjustments
        if (isFlyer()) {
            cost = BangBoard.BASE_TRAVERSAL;
        } else {
            cost = super.traversalCost(terrain) +
                _config.movementAdjust[terrain.category.ordinal()];
        }
        if (influence != null) {
            cost = influence.adjustTraversalCost(terrain, cost);
        }
        return cost;
    }

    /**
     * Provides the unit with its configuration.
     */
    protected void init (UnitConfig config)
    {
        _config = config;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // start with the baseline
        int damage = _config.damage;

        // now account for our damage  and their defense adjustments
        if (target instanceof Unit) {
            Unit utarget = (Unit)target;
            damage += _config.getDamageAdjust(utarget.getConfig());
            damage -= utarget.getConfig().getDefenseAdjust(_config);
        }

        return damage;
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append((_config == null) ? "?" : _config.type).append(", ");
        super.toString(buf);
    }

    @Override // documentation inherited
    protected String infoType ()
    {
        return _config.type;
    }

    protected transient UnitConfig _config;
    protected transient short _respawnTick = -1;
    
    /** The height above ground at which flyers fly (in tile lengths). */
    protected static final float FLYER_GROUND_HEIGHT = 1f;

    /** The height above props at which flyers fly (in tile lengths). */
    protected static final float FLYER_PROP_HEIGHT = 0.25f;
}
