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
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.PuntEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A base piece type for player units.
 */
public class Unit extends Piece
{
    /** The player to whom this unit will return on respawn or -1 if it should
     * not be respawned. */
    public int originalOwner = -1;

    /** Any influence currently acting on this unit. This is not serialized,
     * but will be filled in at the appropriate time on the client and server
     * by effects. */
    public transient Influence influence;

    /** Any hindrance currently acting on this unit. This is not serialized,
     * but will be filled in at the appropriate time on the client and server
     * by effects. */
    public transient Hindrance hindrance;

    /** Type of thing being held, or null for nothing. */
    public String holding;

    /** An additional influence caused by the thing being held. */
    public transient Influence holdingInfluence;

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
        dup.setOwner(bangobj, owner);
        dup.lastActed = lastActed;
        dup.damage = damage;
        dup.assignPieceId(bangobj);
        return dup;
    }

    @Override // documentation inherited
    public void setOwner (BangObject bangobj, int owner)
    {
        super.setOwner(bangobj, owner);
        team = bangobj.scenario.getTeam(owner);
    }

    @Override // documentation inherited
    public boolean expireWreckage (short tick)
    {
        return (_respawnTick > 0 && tick >= _respawnTick) ||
            super.expireWreckage(tick);
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
        return isAlive();
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
    public int computeDamageDiff (BangObject bangobj, Piece target)
    {
        return (target.willDeflect(bangobj, this) ? 0 : 
                computeDamage(target) - _config.damage);
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
            return board.getElevation(tx, ty);
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
            board.getMaxHeightfieldElevation(tx, ty)) +
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
    public boolean checkLineOfSight (
        BangBoard board, int tx, int ty, int dx, int dy)
    {
        // range units are not restricted to line of sight
        return (_config.mode == UnitConfig.Mode.RANGE ||
            super.checkLineOfSight(board, tx, ty, dx, dy));
    }

    @Override // documentation inherited
    public Effect[] willShoot (
            BangObject bangobj, Piece target, ShotEffect shot)
    {
        if (influence != null) {
            return influence.willShoot(bangobj, target, shot);
        }
        return NO_EFFECTS;
    }

    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        ShotEffect shot = null;
        if (hindrance != null) {
            shot = hindrance.shoot(bangobj, this, target, scale);
        }
        if (shot == null) {
            shot = unitShoot(bangobj, target, scale);
        }
        return shot;
    }

    /**
     * Used for overriding the shoot function.
     */
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        return super.shoot(bangobj, target, scale);
    }

    @Override // documentation inherited
    public Effect[] maybeInteract (BangObject bangobj, Piece other)
    {
        ArrayList<Effect> effects = new ArrayList<Effect>();
        if (other instanceof Bonus) {
            Bonus bonus = (Bonus)other;
            if (canActivateBonus(bonus)) {
                if (bonus.getConfig().holdable && holding != null) {
                    effects.add(HoldEffect.dropBonus(
                            bangobj, this, -1, holding)); 
                }
                effects.add(bonus.affect(this));
            } else if (!bonus.getConfig().hidden) {
                effects.add(PuntEffect.puntBonus(bangobj, bonus, pieceId));
            }
        } else if (other instanceof Teleporter) {
            effects.add(((Teleporter)other).affect(this));
        }
        return effects.toArray(new Effect[effects.size()]);
    }

    @Override // documentation inherited
    public ShotEffect returnFire (
        BangObject bangobj, Piece shooter, int newDamage)
    {
        ShotEffect shot = null;
        int oldDamage = this.damage;
        if (_config.returnFire > 0 && newDamage < 100 &&
            targetInRange(x, y, shooter.x, shooter.y)) {
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
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        if (!isAlive()) {
            return null;
        }
        ArrayList<Effect> effects = new ArrayList<Effect>();
        if (influence != null && influence.isExpired(tick)) {
            ExpireInfluenceEffect effect = influence.createExpireEffect();
            effect.init(this);
            effects.add(effect);
        }
        if (hindrance != null) {
            if (hindrance.isExpired(tick)) {
                ExpireInfluenceEffect effect = hindrance.createExpireEffect();
                effect.init(this);
                effects.add(effect);
            } else {
                Effect effect = hindrance.tick();
                if (effect != null) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    @Override // documentation inherited
    public String[] attackInfluenceIcons ()
    {
        if (_attackIcons == null) {
            _attackIcons = new ArrayList<String>();
        }
        if (influence != null && influence.didAdjustAttack()) {
            _attackIcons.add(influence.getName());
        }
        if (holdingInfluence != null && holdingInfluence.didAdjustAttack()) {
            _attackIcons.add(holdingInfluence.getName());
        }
        if (hindrance != null && hindrance.didAdjustAttack()) {
            _attackIcons.add(hindrance.getName());
        }
        String[] icons = null;
        if (_attackIcons.size() > 0) {
            icons = _attackIcons.toArray(new String[_attackIcons.size()]);
        }
        _attackIcons = null;
        return icons;
    }

    @Override // documentation inherited
    protected int getTicksPerMove ()
    {
        int ticks = (influence == null) ? super.getTicksPerMove() :
            influence.adjustTicksPerMove(super.getTicksPerMove());
        ticks = (holdingInfluence == null) ?
            ticks : holdingInfluence.adjustTicksPerMove(ticks);
        return (hindrance == null) ? ticks :
            hindrance.adjustTicksPerMove(ticks);
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        int distance = (influence == null) ? _config.sightDistance :
            influence.adjustSightDistance(_config.sightDistance);
        distance = (holdingInfluence == null) ?
            distance : holdingInfluence.adjustSightDistance(distance);
        return (hindrance == null) ? distance :
            hindrance.adjustSightDistance(distance);
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        int distance = (influence == null) ? _config.moveDistance :
            influence.adjustMoveDistance(_config.moveDistance);
        distance = (holdingInfluence == null) ?
            distance : holdingInfluence.adjustMoveDistance(distance);
        return (hindrance == null) ? distance :
            hindrance.adjustMoveDistance(distance);
    }

    @Override // documentation inherited
    public int getMinFireDistance ()
    {
        int distance = (influence == null) ? _config.minFireDistance :
            influence.adjustMinFireDistance(_config.minFireDistance);
        distance = (holdingInfluence == null) ?
            distance : holdingInfluence.adjustMinFireDistance(distance);
        return (hindrance == null) ? distance :
            hindrance.adjustMinFireDistance(distance);
    }

    @Override // documentation inherited
    public int getMaxFireDistance ()
    {
        int distance = (influence == null) ? _config.maxFireDistance :
            influence.adjustMaxFireDistance(_config.maxFireDistance);
        distance = (holdingInfluence == null) ?
            distance : holdingInfluence.adjustMaxFireDistance(distance);
        return (hindrance == null) ? distance :
            hindrance.adjustMaxFireDistance(distance);
    }

    /**
     * Returns the amount of damage caused to trees by the unit when it is
     * next to them.
     */
    public int getTreeProximityDamage (TreeBed bed)
    {
        return TREE_PROXIMITY_DAMAGE;
    }

    @Override // documentation inherited
    public void wasAdded (BangObject bangobj)
    {
        super.wasAdded(bangobj);
        hindrance = bangobj.globalHindrance;
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
    public void wasDamaged (int newDamage)
    {
        super.wasDamaged(newDamage);
        if (hindrance != null) {
            hindrance.wasDamaged(damage);
        }
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return _config.make == UnitConfig.Make.HUMAN ||
               _config.make == UnitConfig.Make.SPIRIT;
    }

    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        // if we do no damage to this type of target, it is not valid
        return super.validTarget(bangobj, target, allowSelf) &&
            (computeDamage(target) > 0) && (hindrance == null || 
                hindrance.validTarget(this, target, allowSelf));
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
    public boolean isCorporeal ()
    {
        boolean corporeal = super.isCorporeal();
        if (influence != null) {
            corporeal = influence.adjustCorporeality(corporeal);
        }
        if (holdingInfluence != null) {
            corporeal = holdingInfluence.adjustCorporeality(corporeal);
        }
        if (hindrance != null) {
            corporeal = hindrance.adjustCorporeality(corporeal);
        }
        return corporeal;
    }

    @Override // documentation inherited
    public boolean canBePushed ()
    {
        return true;
    }

    /**
     * Compute the valid moves for this piece.
     */
    public void computeMoves (
            BangBoard board, PointSet moves, PointSet attacks)
    {
        board.computeMoves(this, moves, attacks);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite(_config.type);
    }

    @Override // documentation inherited
    public boolean updateSpriteOnTick ()
    {
        return true;
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
        if (holdingInfluence != null) {
            cost = holdingInfluence.adjustTraversalCost(terrain, cost);
        }
        if (hindrance != null) {
            cost = hindrance.adjustTraversalCost(terrain, cost);
        }
        return cost;
    }

    /**
     * Generate a move effect for this unit.
     */
    public MoveEffect generateMoveEffect (
            BangObject bangobj, int nx, int ny, Piece target)
    {
        MoveEffect meffect = new MoveEffect();
        meffect.init(this);
        meffect.nx = (short)nx;
        meffect.ny = (short)ny;
        return meffect;
    }

    /**
     * Called on the server to give the unit a chance to generate an effect
     * to deploy after it has moved of its own volition.
     */
    public Effect maybeGeneratePostMoveEffect (int steps)
    {
        return (hindrance == null) ?
            null : hindrance.maybeGeneratePostMoveEffect(steps);
    }

    /**
     * Called on the server to give the unit a chance to generate an effect
     * to deploy after it has been ordered to move/shoot.
     */
    public Effect maybeGeneratePostOrderEffect ()
    {
        return (hindrance == null) ?
            null : hindrance.maybeGeneratePostOrderEffect();
    }

    @Override // documentation inherited
    public void didMove (int steps)
    {
        if (hindrance != null) {
            hindrance.didMove(steps, lastActed);
        }
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

    /**
     * Returns true if shooting this target will kill it.
     */
    public boolean killShot (BangObject bangobj, Piece target)
    {
        _killShot = true;
        int damage = computeScaledDamage(bangobj, target, 1f);
        _killShot = false;
        return damage + target.damage >= 100;
    }

    @Override // documentation inherited
    public int adjustAttack (Piece target, int damage)
    {
        damage = (influence == null || 
                (_killShot && !influence.showClientAdjust())) ?
            damage : influence.adjustAttack(target, damage);
        damage = (holdingInfluence == null ||
                (_killShot && !holdingInfluence.showClientAdjust())) ?
            damage : holdingInfluence.adjustAttack(target, damage);
        return (hindrance == null ||
                (_killShot && !hindrance.showClientAdjust())) ?
            damage : hindrance.adjustAttack(target, damage);
    }

    @Override // documentation inherited
    public int adjustDefend (Piece shooter, int damage)
    {
        damage = (influence == null ||
                (_killShot && !influence.showClientAdjust())) ?
            damage : influence.adjustDefend(shooter, damage);
        damage = (holdingInfluence == null ||
                (_killShot && !influence.showClientAdjust())) ?
            damage : holdingInfluence.adjustDefend(shooter, damage);
        return (hindrance == null ||
                (_killShot && !influence.showClientAdjust())) ?
            damage : hindrance.adjustDefend(shooter, damage);
    }

    @Override // documentation inherited
    public int adjustProxDefend (Piece shooter, int damage)
    {
        return (influence == null ||
                (_killShot && !influence.showClientAdjust())) ?
            damage : influence.adjustDefend(shooter, damage);
    }

    @Override // documentation inherited
    protected String infoType ()
    {
        return _config.type;
    }

    protected transient UnitConfig _config;
    protected transient short _respawnTick = -1;

    /** Set to true if we're calculating a kill shot. */
    protected transient boolean _killShot = false;

    /** The height above ground at which flyers fly (in tile lengths). */
    protected static final float FLYER_GROUND_HEIGHT = 1f;

    /** The height above props at which flyers fly (in tile lengths). */
    protected static final float FLYER_PROP_HEIGHT = 0.25f;

    /** The base amount by which units next to trees decrease their damage and
     * encourage them to grow. */
    protected static final int TREE_PROXIMITY_DAMAGE = -10;
}
