//
// $Id$

package com.threerings.bang.data.piece;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.util.ArrayIntSet;
import com.threerings.util.RandomUtil;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Bonus;

import static com.threerings.bang.Log.log;

/**
 * Knows about all possible bonuses and does the necessary math to decide
 * which bonus will be deployed when the game wants to do so.
 */
public class BonusFactory
{
    public BonusFactory ()
    {
        _choices.add(new Suitability(new Bonus(Bonus.Type.REPAIR),
                                     100, // base weight
                                     1.0, // damage affinity
                                     0.5, // many pieces affinity
                                     0.0, // few pieces affinity
                                     0.8, // low power affinity
                                     -0.6, // early-game affinity
                                     0.5)); // late-game affinity

        _choices.add(new Suitability(new Bonus(Bonus.Type.DUPLICATE),
                                     100, // base weight
                                     0.0, // damage affinity
                                     -0.8, // many pieces affinity
                                     1.0, // few pieces-affinity
                                     0.5, // low power affinity
                                     0.5, // early-game affinity
                                     0.5)); // late-game affinity

        _choices.add(new Suitability(new Bonus(Bonus.Type.DEFECT),
                                     50, // base weight
                                     0.0, // damage affinity
                                     -0.8, // many pieces affinity
                                     1.0, // few pieces-affinity
                                     1.0, // low power affinity
                                     -0.2, // early-game affinity
                                     0.7)); // late-game affinity

        _choices.add(new Suitability(new Bonus(Bonus.Type.MISSILE_SURPRISE),
                                     50, // base weight
                                     0.0, // damage affinity
                                     0.5, // many pieces affinity
                                     1.0, // few pieces affinity
                                     1.0, // low power affinity
                                     0.5, // early-game affinity
                                     1.0)); // late-game affinity

        _choices.add(new Suitability(new Bonus(Bonus.Type.REPAIR_SURPRISE),
                                     50, // base weight
                                     1.0, // damage affinity
                                     0.5, // many pieces affinity
                                     0.0, // few pieces affinity
                                     1.0, // low power affinity
                                     0.0, // early-game affinity
                                     0.5)); // late-game affinity

        _weights = new int[_choices.size()];
    }

    /**
     * Takes the various circumstances into effect and selects a bonus to
     * be placed on the board at the specified position which can be
     * reached on this same turn by the specfied set of players.
     */
    public Bonus selectBonus (
        BangObject bangobj, Point bspot, ArrayIntSet reachers)
    {
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

        log.info("Selecting bonus [turn=" + bangobj.tick +
                 ", avgpow=" + avgpow + ", avgdam=" + avgdam +
                 ", avgpc=" + avgpieces +
                 ", reachers=" + reachers + "].");

        // now compute weightings for each of our bonuses
        Arrays.fill(_weights, 0);
        for (int ii = 0; ii < _choices.size(); ii++) {
            Suitability s = _choices.get(ii);
            _weights[ii] = s.getWeight(bangobj, avgpow, avgdam, avgpieces);
            _weights[ii] = Math.max(_weights[ii], 0);
            log.info(s.bonus.getType() + " weight " + _weights[ii]);
        }

        // and select one at random
        int idx = RandomUtil.getWeightedIndex(_weights);
        return (Bonus)_choices.get(idx).bonus.clone();
    }

    protected static class Suitability
    {
        public Bonus bonus;

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

        public Suitability (Bonus bonus, int baseWeight, double damageAffinity,
                            double manyPiecesAffinity, double fewPiecesAffinity,
                            double lowPowerAffinity, double earlyGameAffinity,
                            double lateGameAffinity)
        {
            this.bonus = bonus;
            this.baseWeight = baseWeight;
            this.damageAffinity = damageAffinity;
            this.manyPiecesAffinity = manyPiecesAffinity;
            this.fewPiecesAffinity = fewPiecesAffinity;
            this.lowPowerAffinity = lowPowerAffinity;
            this.earlyGameAffinity = earlyGameAffinity;
            this.lateGameAffinity = lateGameAffinity;
        }

        public int getWeight (BangObject bangobj, double averagePower,
                              int averageDamage, int averagePieces)
        {
            // add contributions from each of our affinities
            int weight = baseWeight;
            weight += (int)Math.round(averageDamage * damageAffinity);

            int maxedAP = Math.min(10, averagePieces);
            weight += (int)Math.round(10 * maxedAP * manyPiecesAffinity);
            weight += (int)Math.round(10 * (11-maxedAP) * fewPiecesAffinity);

            double maxedPower = Math.min(1.0, averagePower);
            weight += (int)Math.round(
                100 * (1.0-maxedPower) * lowPowerAffinity);

            int scaledTurn = 100 * Math.min(bangobj.tick, 60) / 60;
            weight += (int)Math.round((100 - scaledTurn) * earlyGameAffinity);
            weight += (int)Math.round(scaledTurn * lateGameAffinity);

            return weight;
        }
    }

    protected ArrayList<Suitability> _choices = new ArrayList<Suitability>();
    protected int[] _weights;
}
