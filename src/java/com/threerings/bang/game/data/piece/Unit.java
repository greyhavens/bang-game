//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;

import static com.threerings.bang.Log.log;

/**
 * A base piece type for player units.
 */
public class Unit extends Piece
{
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
    public Unit duplicate ()
    {
        Unit dup = getUnit(_config.dupeType);
        dup.owner = owner;
        dup.lastActed = lastActed;
        dup.damage = damage;
        dup.assignPieceId();
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

    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return bonus.getConfig().type.equals("nugget") ? !benuggeted : true;
    }

    @Override // documentation inherited
    public int getCost ()
    {
        return _config.scripCost;
    }

    @Override // documentation inherited
    public boolean tick (short tick)
    {
        if (influence != null && influence.isExpired(tick)) {
            log.info("Expiring " + influence + ".");
            influence = null;
            return true;
        }
        return false;
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
    public int getFireDistance ()
    {
        return (influence == null) ? _config.fireDistance :
            influence.adjustFireDistance(_config.fireDistance);
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return _config.make == UnitConfig.Make.HUMAN;
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target)
    {
        // if we do no damage to this type of target, it is not valid
        return super.validTarget(target) && (computeDamage(target) > 0);
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
    public int traversalCost (Terrain terrain)
    {
        int cost;
        // flyers are unaffected by terrain adjustments
        if (isFlyer()) {
            cost = BangBoard.BASE_TRAVERSAL;
        } else {
            cost = super.traversalCost(terrain) +
                _config.movementAdjust[terrain.ordinal()];
        }
        if (influence != null) {
            cost = influence.adjustTraversalCost(terrain, cost);
        }
        return cost;
    }

    @Override // documentation inherited
    public void wasKilled ()
    {
        super.wasKilled();
        benuggeted = false;
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

    protected transient UnitConfig _config;
    protected transient short _respawnTick = -1;
}
