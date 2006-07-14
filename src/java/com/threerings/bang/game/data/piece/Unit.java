//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.ArrayList;
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
import com.threerings.bang.game.data.effect.ExpireHindranceEffect;
import com.threerings.bang.game.data.effect.ExpireInfluenceEffect;
import com.threerings.bang.game.data.effect.HoldEffect;
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

    /** Any hindrance currently acting on this unit. */
    public Hindrance hindrance;

    /** Type of thing being held, or null for nothing. */
    public String holding;

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

    /**
     * Configures this piece with a new hindrance.
     */
    public void setHindrance (Hindrance hindrance, short tick)
    {
        hindrance.init(tick);
        this.hindrance = hindrance;
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
        return duplicate(bangobj, _config.dupeType);
    }

    /**
     * Creates a new unit of a specific type that is otherwise an exact
     * duplicate of this unit.
     */
    public Unit duplicate (BangObject bangobj, String unitType)
    {
        Unit dup = getUnit(unitType);
        dup.owner = owner;
        dup.lastActed = lastActed;
        dup.damage = damage;
        dup.assignPieceId(bangobj);
        return dup;
    }

    @Override // documentation inherited
    public boolean expireWreckage (short tick)
    {
        return (tick >= _respawnTick) || super.expireWreckage(tick);
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
            (bonus.getConfig().holdable ?  holding == null : true);
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
    public String getName ()
    {
        return UnitConfig.getName(_config.type);
    }
    
    @Override // documentation inherited
    public void init ()
    {
        super.init();

        // configure our last acted tick according to our initiative
        lastActed = (short)(-1 * _config.initiative);
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        if ((moving ? isFlyer() : isAirborne()) && isAlive()) {
            return computeAreaFlightElevation(board, tx, ty);
        } else {
            return super.computeElevation(board, tx, ty, moving);
        }
    }

    /**
     * Finds the maximum flying elevation over a grid.
     */
    public int computeAreaFlightElevation (BangBoard board, int tx, int ty)
    {
        int nsElevation = Math.min(computeFlightElevation(board, tx - 1, ty),
                computeFlightElevation(board, tx + 1, ty));
        int ewElevation = Math.min(computeFlightElevation(board, tx, ty - 1),
                computeFlightElevation(board, tx, ty + 1));
        int elevation = Math.max(computeFlightElevation(board, tx, ty),
                Math.max(nsElevation, ewElevation));
        return elevation;
    }
    
    /**
     * Computes the elevation of for a flying unit.
     */
    public int computeFlightElevation (BangBoard board, int tx, int ty)
    {
        int groundel = Math.max(board.getWaterLevel(),
            board.getMaxElevation(tx, ty)) +
                (int)(FLYER_GROUND_HEIGHT * board.getElevationUnitsPerTile()),
            propel = board.getMaxElevation(tx, ty) +
                (int)(FLYER_PROP_HEIGHT * board.getElevationUnitsPerTile());
        return Math.max(groundel, propel);
    }

    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return true;
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
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        ShotEffect shot = null;
        if (hindrance != null) {
            shot = hindrance.shoot(bangobj, this, target, scale);
        }
        if (shot == null) {
            shot = super.shoot(bangobj, target, scale);
        }
        return shot;
    }

    @Override // documentation inherited
    public Effect maybeInteract (Piece other)
    {
        if (other instanceof Bonus && canActivateBonus((Bonus)other)) {
            return ((Bonus)other).affect(this);
        } else if (other instanceof Teleporter) {
            return ((Teleporter)other).affect(this);
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
            shot = shoot(bangobj, shooter, _config.returnFire/100f);
            shot.type = ShotEffect.RETURN_FIRE;
            this.damage = oldDamage;
        }
        return shot;
    }

    @Override // documentation inherited
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        return holding != null ?
            HoldEffect.dropBonus(bangobj, this, shooterId, holding) : null;
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        ArrayList<Effect> effects = new ArrayList<Effect>();
        if (influence != null && influence.isExpired(tick)) {
            ExpireInfluenceEffect effect = new ExpireInfluenceEffect();
            effect.init(this);
            effects.add(effect);
        }
        if (hindrance != null && hindrance.isExpired(tick)) {
            ExpireHindranceEffect effect = new ExpireHindranceEffect();
            effect.init(this);
            effects.add(effect);
        }
        return effects;
    }

    @Override // documentation inherited
    public String attackInfluenceIcon ()
    {
        if (influence != null && influence.didAdjustAttack()) {
            return influence.getName();
        }
        return null;
    }

    @Override // documentation inherited
    protected int getTicksPerMove ()
    {
        int ticks = (influence == null) ? super.getTicksPerMove() :
            influence.adjustTicksPerMove(super.getTicksPerMove());
        return (hindrance == null) ? ticks :
            hindrance.adjustTicksPerMove(ticks);
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        int distance = (influence == null) ? _config.sightDistance :
            influence.adjustSightDistance(_config.sightDistance);
        return (hindrance == null) ? distance :
            hindrance.adjustSightDistance(distance);
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        int distance = (influence == null) ? _config.moveDistance :
            influence.adjustMoveDistance(_config.moveDistance);
        return (hindrance == null) ? distance :
            hindrance.adjustMoveDistance(distance);
    }

    @Override // documentation inherited
    public int getMinFireDistance ()
    {
        int distance = (influence == null) ? _config.minFireDistance :
            influence.adjustMinFireDistance(_config.minFireDistance);
        return (hindrance == null) ? distance :
            hindrance.adjustMinFireDistance(distance);
    }

    @Override // documentation inherited
    public int getMaxFireDistance ()
    {
        int distance = (influence == null) ? _config.maxFireDistance :
            influence.adjustMaxFireDistance(_config.maxFireDistance);
        return (hindrance == null) ? distance :
            hindrance.adjustMaxFireDistance(distance);
    }

    @Override // documentation inherited
    public void wasKilled (short tick)
    {
        super.wasKilled(tick);

        // influences and hindrances do not survive through death
        influence = null;
        hindrance = null;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return _config.make == UnitConfig.Make.HUMAN ||
               _config.make == UnitConfig.Make.SPIRIT;
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
    public boolean isAirborne ()
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
        if (hindrance != null) {
            cost = hindrance.adjustTraversalCost(terrain, cost);
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
    protected void toString (StringBuilder buf)
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
