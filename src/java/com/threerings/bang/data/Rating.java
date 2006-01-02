//
// $Id$

package com.threerings.bang.data;

import com.threerings.media.util.MathUtil;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.ScenarioCodes;

/**
 * Contains the player's ratings and experience for a particular game scenario.
 */
public class Rating
    implements DSet.Entry, Cloneable
{
    /** The minimum rating value. */
    public static final int MINIMUM_RATING = 1000;

    /** The default rating value. */
    public static final int DEFAULT_RATING = 1200;

    /** The maximum rating value. */
    public static final int MAXIMUM_RATING = 3000;

    /** The scenario for which this rating applies (or {@link
     * ScenarioCodes#OVERALL} for the player's overall rating. */
    public String scenario;

    /** The actual rating value. */
    public int rating = DEFAULT_RATING;

    /** The number of rounds of the scenario, (or games in total for the
     * overall rating) the player has played. */
    public int experience;

    /**
     * Computes a player's updated rating using a modified version of the
     * FIDE/ELO system. The rating adjustment is computed for the player versus
     * each opponent individually and these adjustments are summed and scaled
     * by one over the number of opponents to create the final rating
     * adjustment, which is then added to the player's previous rating and
     * bounded to the rating range. <em>Note:</em> provisional players (those
     * with experience of less than 20) will be treated as having, at most, the
     * default rating when used as an opponent in calculatons for a
     * non-provisional player.
     *
     * @param scores the player's relative scores in the match. A player with a
     * higher score will be rated as defeating the player in question, a player
     * with the same score will be rated as drawing the player in question and
     * a player with a lower score will be rated as losing to the player in
     * question. If an opponent's score is less than zero, they will be assumed
     * not to have participated and will be ignored.
     * @param ratings the pre-match ratings of each of the opponents.
     * @param exps the pre-match experience levels of each of the
     * opponents.
     * @param pidx the index of the player whose rating is to be calculated.
     *
     * @return the player's updated rating or -1 if none of the opponents could
     * be applicably rated against this player due to provisional/
     * non-provisional mismatch or lack of participation.
     */
    public static int computeRating (
        int[] scores, int[] ratings, int[] exps, int pidx)
    {
        float dR = 0; // the total delta rating
        int opponents = 0;

        for (int ii = 0; ii < scores.length; ii++) {
            if (pidx == ii | scores[ii] < 0) {
                continue;
            }

            // if we are non-provisional, and the opponent is provisional, we
            // max the opponent out at the default rating to avoid potentially
            // inflating a real rating with one that has a lot of uncertainty
            int opprat = ratings[ii];
            if (exps[ii] < 20) {
                opprat = Math.min(opprat, DEFAULT_RATING);
            }
            float W = (scores[ii] == scores[pidx]) ? 0.5f :
                (scores[ii] > scores[pidx] ? 0f : 1f);
            dR += computeAdjustment(W, opprat, ratings[pidx], exps[pidx]);
            opponents++;
        }

        int nrat = (int)Math.round(ratings[pidx] + dR/opponents);
        return MathUtil.bound(MINIMUM_RATING, nrat, MAXIMUM_RATING);
    }

    /**
     * Computes a ratings adjustment for the given player, using a modified
     * version of the FIDE Chess rating system as:
     *
     * <pre>
     * adjustment = K(W - We)
     *
     * where:
     *
     * K = if (sessions < 20) then 64
     *     else if (rating < 2100 and sessions >= 20) then 32
     *     else if (rating >= 2100 and rating < 2400 and sessions >= 20)
     *          then 24
     *     else 16
     * W = score for the game just completed, as 1.0, 0.5, and 0.0 for a
     * win, draw, or loss, respectively.
     * dR = opponent's rating minus player's rating.
     * We = expected score (win expectancy) as determined by:
     *
     *     We = 1 / (10^(dR/400) + 1)
     * </pre>
     *
     * @param W the win value the game in question (1.0 means the player won,
     * 0.5 means they drew, 0 means they lost).
     * @param opprat the opponent's current rating.
     * @param rating the player's current rating.
     * @param sessions the number of rated games played by this player in the
     * past.
     *
     * @return the adjustment to the player's rating.
     */
    public static float computeAdjustment (
        float W, int opprat, int rating, int sessions)
    {
        // calculate We, the win expectancy
        float dR = opprat - rating;
        float We = 1.0f / (float)(Math.pow(10.0f, (dR / 400.0f)) + 1);

        // calculate K, the score multiplier constant
        int K;
        if (sessions < 20) {
            K = 64;
        } else if (rating < 2100) {
            K = 32; // sessions >= 20
        } else if (rating < 2400) {
            K = 24; // sessions >= 20 && rating >= 2100
        } else {
            K = 16; // sessions >= 20 && rating >= 2400
        }

        // compute and return the ratings adjustment
        return K * (W - We);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("WTF?");
        }
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return scenario;
    }
}
