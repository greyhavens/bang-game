//
// $Id$

package com.threerings.bang.data.piece;

import java.awt.Point;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import com.samskivert.util.ArrayIntSet;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.RandomUtil;

import com.threerings.bang.client.sprite.BonusSprite;
import com.threerings.bang.client.sprite.PieceSprite;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.effect.BonusPointEffect;
import com.threerings.bang.data.effect.DefectEffect;
import com.threerings.bang.data.effect.DuplicateEffect;
import com.threerings.bang.data.effect.Effect;
import com.threerings.bang.data.effect.GrantSurpriseEffect;
import com.threerings.bang.data.effect.PlagueEffect;
import com.threerings.bang.data.effect.RepairEffect;
import com.threerings.bang.data.effect.SaintElmosEffect;

import com.threerings.bang.data.surprise.AreaRepair;
import com.threerings.bang.data.surprise.DustDevil;
import com.threerings.bang.data.surprise.Missile;

import static com.threerings.bang.Log.log;

/**
 * Represents an exciting bonus waiting to be picked up by a player on the
 * board. Bonuses may generate full-blown effects or just influence the
 * piece that picked them up.
 */
public class Bonus extends Piece
{
    /** Indicates the type of bonus. */
    public enum Type {
        UNKNOWN(0, // base weight
                0, // damage affinity
                0, // many pieces affinity
                0, // few pieces affinity
                0, // low power affinity
                0, // early-game affinity
                0) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return null;
            }
        },

        REPAIR(100, // base weight
               1.0, // damage affinity
               0.5, // many pieces affinity
               0.0, // few pieces affinity
               0.8, // low power affinity
               -0.6, // early-game affinity
               0.5) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new RepairEffect(piece.pieceId);
            }
        },

        DUPLICATE(75, // base weight
                  0.0, // damage affinity
                  -0.8, // many pieces affinity
                  1.0, // few pieces-affinity
                  0.5, // low power affinity
                  0.5, // early-game affinity
                  0.5) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new DuplicateEffect(piece.pieceId);
            }
        },

        DEFECT(50, // base weight
               0.0, // damage affinity
               -0.8, // many pieces affinity
               1.0, // few pieces-affinity
               1.0, // low power affinity
               -0.2, // early-game affinity
               0.7) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new DefectEffect(piece.owner);
            }
        },

        MISSILE(50, // base weight
                0.0, // damage affinity
                0.5, // many pieces affinity
                1.0, // few pieces affinity
                1.0, // low power affinity
                0.5, // early-game affinity
                1.0) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new GrantSurpriseEffect(piece.owner, new Missile());
            }
        },

        AREA_REPAIR(50, // base weight
                    1.0, // damage affinity
                    0.5, // many pieces affinity
                    0.0, // few pieces affinity
                    1.0, // low power affinity
                    0.0, // early-game affinity
                    0.5) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new GrantSurpriseEffect(piece.owner, new AreaRepair());
            }
        },

        DUST_DEVIL(50, // base weight
                   0.5, // damage affinity
                   0.0, // many pieces affinity
                   0.5, // few pieces affinity
                   0.0, // low power affinity
                   -0.25, // early-game affinity
                   0.4) // late-game affinity
        {
            public boolean isValid (BangObject bangobj) {
                // make sure there are some dead pieces on the board
                return bangobj.countDeadPieces() > 1;
            }

            public Effect affect (Piece piece) {
                return new GrantSurpriseEffect(piece.owner, new DustDevil());
            }
        },

        SAINT_ELMO(10, // base weight
                   0.5, // damage affinity
                   -0.5, // many pieces affinity
                   0.6, // few pieces affinity
                   0.0, // low power affinity
                   0.0, // early-game affinity
                   0.5) // late-game affinity
        {
            public boolean isValid (BangObject bangobj) {
                // make sure there are some dead pieces on the board
                return bangobj.countDeadPieces() > 2;
            }

            public Effect affect (Piece piece) {
                return new SaintElmosEffect(piece.owner);
            }
        },

        PLAGUE(10, // base weight
               0.0, // damage affinity
               0.0, // many pieces affinity
               0.4, // few pieces affinity
               0.4, // low power affinity
               0.0, // early-game affinity
               0.5) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new PlagueEffect(piece.owner);
            }
        },

        BONUS_POINT(25, // base weight
                    0.0, // damage affinity
                    -0.5, // many pieces affinity
                    0.0, // few pieces affinity
                    0.0, // low power affinity
                    0.5, // early-game affinity
                    -0.5) // late-game affinity
        {
            public Effect affect (Piece piece) {
                return new BonusPointEffect(piece.pieceId);
            }
        };

        private Type (int baseWeight, double damageAffinity,
                      double manyPiecesAffinity, double fewPiecesAffinity,
                      double lowPowerAffinity, double earlyGameAffinity,
                      double lateGameAffinity)
        {
            this.baseWeight = baseWeight;
            this.damageAffinity = damageAffinity;
            this.manyPiecesAffinity = manyPiecesAffinity;
            this.fewPiecesAffinity = fewPiecesAffinity;
            this.lowPowerAffinity = lowPowerAffinity;
            this.earlyGameAffinity = earlyGameAffinity;
            this.lateGameAffinity = lateGameAffinity;
        }

        /** Indicates the base weighting of this bonus. */
        public int baseWeight;

        /** Indicates that this bonus is more valuable to a player whose
         * average unit damage is high. */
        public double damageAffinity;

        /** Indicates that this bonus is more valuable to a player who has
         * a large number of pieces. */
        public double manyPiecesAffinity;

        /** Indicates that this bonus is more valuable to a player who has
         * a small number of pieces. */
        public double fewPiecesAffinity;

        /** Indicates that this bonus should be given only to players with
         * very little power compared to the average. */
        public double lowPowerAffinity;

        /** Indicates that this bonus should be favored during the early
         * game. */
        public double earlyGameAffinity;

        /** Indicates that this bonus should be favored during the later
         * game. */
        public double lateGameAffinity;

        public boolean isValid (BangObject bangobj)
        {
            return true;
        }

        public int getWeight (BangObject bangobj, double averagePower,
                              int averageDamage, int averagePieces)
        {
            // add contributions from each of our affinities
            int eweight = 0, ecount = 0;
            if (damageAffinity != 0) {
                eweight += (int)Math.round(averageDamage * damageAffinity);
                ecount++;
            }

            int maxedAP = Math.min(10, averagePieces);
            if (manyPiecesAffinity != 0) {
                eweight += (int)Math.round(10 * maxedAP * manyPiecesAffinity);
                ecount++;
            }
            if (fewPiecesAffinity != 0) {
                eweight += (int)Math.round(
                    10 * (11-maxedAP) * fewPiecesAffinity);
                ecount++;
            }

            if (lowPowerAffinity != 0) {
                double maxedPower = Math.min(1.0, averagePower);
                eweight += (int)Math.round(
                    100 * (1.0-maxedPower) * lowPowerAffinity);
                ecount++;
            }

            int scaledTurn = 100 * Math.min(bangobj.tick, 60) / 60;
            if (earlyGameAffinity != 0) {
                eweight += (int)Math.round(
                    (100 - scaledTurn) * earlyGameAffinity);
                ecount++;
            }
            if (lateGameAffinity != 0) {
                eweight += (int)Math.round(scaledTurn * lateGameAffinity);
                ecount++;
            }

            return baseWeight + (eweight / ecount);
        }

        public abstract Effect affect (Piece piece);
    };

    /** Unserialization constructor. */
    public Bonus ()
    {
    }

    /**
     * Creates a particular type of bonus piece.
     */
    public Bonus (Type type)
    {
        _type = type;
    }

    public Type getType ()
    {
        return _type;
    }

    /**
     * Called when a piece has landed on this bonus and is activating it,
     * this should return an object indicating the effect that the bonus
     * has on this piece or the entire board. Those effects will be
     * processed at the end of the tick.
     */
    public Effect affect (Piece piece)
    {
        return _type.affect(piece);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BonusSprite(_type.toString().toLowerCase());
    }

    @Override // documentation inherited
    public String info ()
    {
        return super.info() + " t:" + _type;
    }

    /**
     * Extends default behavior to serialize our bonus type.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        _type = Type.valueOf(Type.class, in.readUTF());
    }

    /**
     * Extends default behavior to serialize our bonus type.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeUTF(_type.name());
    }

    /**
     * Takes the various circumstances into effect and selects a bonus to
     * be placed on the board at the specified position which can be
     * reached on this same turn by the specfied set of players.
     */
    public static Bonus selectBonus (
        BangObject bangobj, Point bspot, ArrayIntSet reachers)
    {
        if (_choices == null) {
            EnumSet<Type> choices = EnumSet.allOf(Type.class);
            _choices = new Type[choices.size()];
            choices.toArray(_choices);
            _weights = new int[choices.size()];
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
        int avgdam = bangobj.getAveragePieceDamage(reachers);
        int avgpieces = bangobj.getAveragePieceCount(reachers);

        // now compute weightings for each of our bonuses
        StringBuffer buf = new StringBuffer();
        Arrays.fill(_weights, 0);
        for (int ii = 0; ii < _choices.length; ii++) {
            Type type = _choices[ii];
            if (!type.isValid(bangobj)) {
                continue;
            }
            _weights[ii] = type.getWeight(bangobj, avgpow, avgdam, avgpieces);
            _weights[ii] = Math.max(_weights[ii], 0);
            // record data for logging
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(type).append(" ").append(_weights[ii]);
        }

        log.info("Selecting bonus [turn=" + bangobj.tick +
                 ", avgpow=" + avgpow + ", avgdam=" + avgdam +
                 ", avgpc=" + avgpieces + ", reachers=" + reachers +
                 ", weights=(" + buf + ")].");

        // and select one at random
        int idx = RandomUtil.getWeightedIndex(_weights);
        return new Bonus(_choices[idx]);
    }

    /** The type of bonus we represent. */
    protected transient Type _type = Type.UNKNOWN;

    /** Used when selecting a random bonus. */
    protected static int[] _weights;

    /** Used when selecting a random bonus. */
    protected static Type[] _choices;
}
