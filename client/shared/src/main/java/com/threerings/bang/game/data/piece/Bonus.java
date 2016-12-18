//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.io.IOException;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.RandomUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.BonusEffect;
import com.threerings.bang.game.data.effect.PuntEffect;

import static com.threerings.bang.Log.log;

/**
 * Represents an exciting bonus waiting to be picked up by a player on the board. Bonuses may
 * generate full-blown effects or just influence the piece that picked them up.
 */
public class Bonus extends Piece
{
    /** The index of the spot at which this bonus was spawned. */
    public short spot = (short)-1;

    /**
     * Takes the various circumstances into effect and selects a bonus to be placed on the board at
     * the specified position (which has been determined to be reachable on this same turn by the
     * supplied set of players).
     */
    public static Bonus selectBonus (BangObject bangobj, ArrayIntSet reachers)
    {
        BonusConfig[] configs = BonusConfig.getTownBonuses(bangobj.townId);

        // if no one can reach the spot, base our calculations on all the players instead
        if (reachers == null) {
            reachers = new ArrayIntSet();
            for (int ii = 0; ii < bangobj.players.length; ii++) {
                reachers.add(ii);
            }
        }

        // now compute some information on the reachers
        double avgpow = bangobj.getAveragePower(reachers);
        int avgdam = bangobj.getAverageUnitDamage(reachers);
        int avgunits = bangobj.getAverageUnitCount(reachers);
        int pointDiff = bangobj.getPointsDiff(reachers);

        // now compute weightings for each of our bonuses
        StringBuffer buf = new StringBuffer();
        int[] weights = new int[configs.length];
        for (int ii = 0; ii < configs.length; ii++) {
            BonusConfig config = configs[ii];
            // if this bonus's base weight is below the minimum for this game, skip it
            if (config.baseWeight < bangobj.minCardBonusWeight) {
                continue;
            }
            weights[ii] = config.getWeight(bangobj, avgpow, avgdam, avgunits, pointDiff);
            weights[ii] = Math.max(weights[ii], 0);
            // record data for logging
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(config.type).append(" ").append(weights[ii]);
        }

        log.debug("Selecting bonus", "turn", bangobj.tick, "avgpow", avgpow, "avgdam", avgdam,
                  "avgpc", avgunits, "pointDiff", pointDiff, "reachers", reachers, "weights", buf);

        // and select one at random
        return createBonus(configs[RandomUtil.getWeightedIndex(weights)]);
    }

    /**
     * Creates a bonus of the specified type.
     */
    public static Bonus createBonus (String type)
    {
        BonusConfig config = BonusConfig.getConfig(type);
        return (config == null) ? null : createBonus(config);
    }

    /**
     * Creates a bonus with the specified configuration.
     */
    public static Bonus createBonus (BonusConfig config)
    {
        return createBonus(config, -1);
    }

    /**
     * Creates a bonus with the specified configuration.
     */
    public static Bonus createBonus (BonusConfig config, int owner)
    {
        if (config == null) {
            return null;
        }

        try {
            Bonus bonus;
            if (config.bonusClass != null) {
                bonus = (Bonus)Class.forName(config.bonusClass).newInstance();
            } else {
                bonus = new Bonus();
            }
            bonus.init(config);
            bonus.owner = owner;
            return bonus;

        } catch (Exception e) {
            log.warning("Failed to instantiate custom bonus class", "class", config.bonusClass, e);
            return null;
        }
    }

    /**
     * Handy function for checking if this piece is a bonus and of the
     * specified type.
     */
    public static boolean isBonus (Piece piece, String type)
    {
        return (piece instanceof Bonus) &&
            ((Bonus)piece).getConfig().type.equals(type);
    }

    /**
     * Configures this bonus instance with the good stuff.
     */
    public void init (BonusConfig config)
    {
        _config = config;
    }

    /**
     * Returns the configuration for this bonus.
     */
    public BonusConfig getConfig ()
    {
        return _config;
    }

    /**
     * If this bonus is not randomly selected, but rather used by a scenario
     * for some specific purpose, we won't count it toward certain statistics.
     */
    public boolean isScenarioBonus ()
    {
        return (_config.baseWeight <= 0);
    }

    /**
     * Called when a piece has landed on this bonus and is activating it,
     * this should return an object indicating the effect that the bonus
     * has on this piece or the entire board. Those effects will be
     * processed at the end of the tick.
     */
    public BonusEffect affect (BangObject bangobj, Piece piece)
    {
        // ground-only bonuses do not affect airborne units
        if (_config.groundOnly && piece.isAirborne()) {
            return null;
        }
        try {
            BonusEffect effect = (BonusEffect)Class.forName(
                _config.effectClass).newInstance();
            effect.bonusId = pieceId;
            effect.init(piece);
            if (!_config.hidden) {
                effect.puntEffect =
                    PuntEffect.puntBonus(bangobj, this, piece.pieceId);
            }
            return effect;
        } catch (Exception e) {
            log.warning("Failed to instantiate effect class", "class", _config.effectClass, e);
            return null;
        }
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        return board.getElevation(tx, ty);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }

    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (_config.hidden ? -1 : 0);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BonusSprite(_config.type);
    }

    /**
     * Returns a valid location to drop/punt this bonus.
     */
    public Point getDropLocation (BangObject bangobj)
    {
        return bangobj.board.getOccupiableSpot(x, y, 10);
    }

    @Override // documentation inherited
    public String toString ()
    {
        return super.toString() + " t:" +
            (_config != null ? _config.type : "unknown");
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        init(BonusConfig.getConfig(in.readUTF()));
    }

    /** Writes some custom information for this piece. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeUTF(_config.type);
    }

    /** The configuration for the bonus we represent. */
    protected transient BonusConfig _config;
}
