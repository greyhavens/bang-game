//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

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
 * Represents an exciting bonus waiting to be picked up by a player on the
 * board. Bonuses may generate full-blown effects or just influence the
 * piece that picked them up.
 */
public class Bonus extends Piece
{
    /** The index of the spot at which this bonus was spawned. */
    public short spot = (short)-1;

    /**
     * Takes the various circumstances into effect and selects a bonus to
     * be placed on the board at the specified position which can be
     * reached on this same turn by the specfied set of players.
     */
    public static Bonus selectBonus (BangObject bangobj, ArrayIntSet reachers)
    {
        BonusConfig[] configs = BonusConfig.getTownBonuses(bangobj.townId);
        int[] weights = _weights.get(bangobj.townId);
        if (weights == null) {
            // create a scratch array of the appropriate size
            _weights.put(bangobj.townId, weights = new int[configs.length]);
        }

        // if no one can reach the spot, base our calculations on all the
        // players instead
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

        // now compute weightings for each of our bonuses
        StringBuffer buf = new StringBuffer();
        Arrays.fill(weights, 0);
        for (int ii = 0; ii < configs.length; ii++) {
            BonusConfig config = configs[ii];
// TODO: instantiate a prototype of the custom bonus class for each type?
//             if (!config.isValid(bangobj)) {
//                 continue;
//             }
            weights[ii] = config.getWeight(bangobj, avgpow, avgdam, avgunits);
            weights[ii] = Math.max(weights[ii], 0);
            // record data for logging
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(config.type).append(" ").append(weights[ii]);
        }

        log.fine("Selecting bonus [turn=" + bangobj.tick +
                 ", avgpow=" + avgpow + ", avgdam=" + avgdam +
                 ", avgpc=" + avgunits + ", reachers=" + reachers +
                 ", weights=(" + buf + ")].");

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
            return bonus;

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to instantiate custom bonus class " +
                    "[class=" + config.bonusClass + "].", e);
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
            log.log(Level.WARNING, "Failed to instantiate effect class " +
                    "[class=" + _config.effectClass + "].", e);
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
    public int getGoalRadius (Piece mover)
    {
        return (_config.hidden || (_config.holdable &&
            mover instanceof Unit && ((Unit)mover).holding != null)) ?
                -1 : 0;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BonusSprite(_config.type);
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

    /** Used when selecting a random bonus. */
    protected static HashMap<String,int[]> _weights =
        new HashMap<String,int[]>();
}
