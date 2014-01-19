//
// $Id$

package com.threerings.bang.data;

import java.sql.Date;
import java.util.Calendar;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.media.util.MathUtil;

import com.threerings.bang.game.data.scenario.ScenarioInfo;

/**
 * Contains the player's ratings and experience for a particular game scenario.
 */
public class Rating extends SimpleStreamableObject
    implements Cloneable
{
    /** The minimum rating value. */
    public static final int MINIMUM_RATING = 1000;

    /** The default rating value. */
    public static final int DEFAULT_RATING = 1200;

    /** The maximum rating value. */
    public static final int MAXIMUM_RATING = 3000;

    /** The scenario for which this rating applies (or {@link
     * ScenarioInfo#OVERALL_IDENT} for the player's overall rating. */
    public String scenario;

    /** The actual rating value. */
    public int rating = DEFAULT_RATING;

    /** The number of rounds of the scenario, (or games in total for the
     * overall rating) the player has played. */
    public int experience;

    /** The start day for a 7 day rated period, (or null for lifetime). */
    public Date week;

    /**
     * Returns the date representing the current week for use in Rating.
     */
    public static Date thisWeek ()
    {
        Calendar week = formatForWeek(Calendar.getInstance());
        week.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return new Date(week.getTimeInMillis());
    }

    /**
     * Returns the date representing the previous week for use in Rating.
     */
    public static Date getWeek (int delta)
    {
        Calendar week = formatForWeek(Calendar.getInstance());
        week.add(Calendar.WEEK_OF_YEAR, -delta);
        week.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return new Date(week.getTimeInMillis());
    }

    /**
     * Formats a calendar to sunday at midnight.
     */
    public static Calendar formatForWeek (Calendar cal)
    {
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

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
     * @param pidx the index of the player whose rating is to be calculated.
     *
     * @return the player's updated rating or -1 if none of the opponents could
     * be applicably rated against this player due to provisional/
     * non-provisional mismatch or lack of participation.
     */
    public static int computeRating (int[] scores, Rating[] ratings, int pidx)
    {
        float dR = 0; // the total delta rating
        int opponents = 0;

        for (int ii = 0; ii < scores.length; ii++) {
            if (pidx == ii || scores[ii] < 0) {
                continue;
            }

            // if we are non-provisional, and the opponent is provisional, we
            // max the opponent out at the default rating to avoid potentially
            // inflating a real rating with one that has a lot of uncertainty
            int opprat = ratings[ii].rating;
            if (!ratings[pidx].isProvisional() && ratings[ii].isProvisional()) {
                opprat = Math.min(opprat, DEFAULT_RATING);
            }
            float W = (scores[ii] == scores[pidx]) ? 0.5f :
                (scores[ii] > scores[pidx] ? 0f : 1f);
            // a score of 0 will always be considered a loss
            if (scores[pidx] == 0) {
                W = 0f;
            }
            dR += computeAdjustment(W, opprat, ratings[pidx]);
            opponents++;
        }

        // if we have no valid opponents, we cannot compute a rating;
        // similarly, if we did not play, do not return a rating
        if (opponents == 0 || scores[pidx] < 0) {
            return -1;
        }

        int nrat = Math.round(ratings[pidx].rating + dR/opponents);
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
     * K = if (experience < 20) then 64
     *     else if (rating < 2100 and experience >= 20) then 32
     *     else if (rating >= 2100 and rating < 2400 and experience >= 20)
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
     *
     * @return the adjustment to the player's rating.
     */
    public static float computeAdjustment (float W, int opprat, Rating rating)
    {
        // calculate We, the win expectancy
        float dR = opprat - rating.rating;
        float We = 1.0f / (float)(Math.pow(10.0f, (dR / 400.0f)) + 1);

        // calculate K, the score multiplier constant
        int K;
        if (rating.experience < 20) {
            K = 64;
        } else if (rating.rating < 2100) {
            K = 32; // experience >= 20
        } else if (rating.rating < 2400) {
            K = 24; // experience >= 20 && rating >= 2100
        } else {
            K = 16; // experience >= 20 && rating >= 2400
        }

        // compute and return the ratings adjustment
        return K * (W - We);
    }

    /**
     * Computes a player's updated rating for a cooperative game.
     *
     * @param pctile the percentile score that the team achieved for the round
     */
    public static int computeCoopRating (
        int pctile, Rating[] ratings, int pidx)
    {
        // map our percentile to a rating value and compute the team's average
        // rating
        int erat = MINIMUM_RATING +
            (pctile * (MAXIMUM_RATING - MINIMUM_RATING) / 100);
        int trat = 0;
        for (Rating rating : ratings) {
            trat += rating.rating;
        }
        trat = MathUtil.bound(MINIMUM_RATING, trat / ratings.length,
            MAXIMUM_RATING);

        // compute the K value. Low exp players get to move more quickly.
        float K;
        if (ratings[pidx].experience < 20) {
            if (ratings[pidx].experience < 10) {
                K = 500f; // 0-9 rounds
            } else {
                K = 250f; // 10-19 rounds
            }
        } else {
            K = 125f; // 20+ rounds
        }

        // compute the delta rating as a percentage of the team's
        // current rating (eg. they should have been 12% better or worse)
        float pctdiff = ((float)(erat - trat) / trat);

        // update the player's rating
        int nrat = Math.round(ratings[pidx].rating + pctdiff * K);

        // make sure the rating remains within a valid range
        return MathUtil.bound(MINIMUM_RATING, nrat, MAXIMUM_RATING);
    }

    /**
     * Returns true if this rating is provisional (experience < 20).
     */
    public boolean isProvisional ()
    {
        return (experience < 20);
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

    @Override // documentation inherited
    public int hashCode ()
    {
        return scenario.hashCode();
    }
}
