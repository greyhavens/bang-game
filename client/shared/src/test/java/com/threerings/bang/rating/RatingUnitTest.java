//
// $Id$

package com.threerings.bang.rating;

import junit.framework.Test;
import junit.framework.TestCase;

import com.threerings.bang.data.Rating;

/**
 * Unit test for the {@link Rating} class.
 */
public class RatingUnitTest extends TestCase
{
    public RatingUnitTest ()
    {
        super(RatingUnitTest.class.getName());
    }

    public void runTest ()
    {
        for (int ii = 0; ii < DATA.length; ii++) {
            Rating[] ratings = new Rating[DATA[ii][0].length];
            for (int pidx = 0; pidx < ratings.length; pidx++) {
                ratings[pidx] = new Rating();
                ratings[pidx].rating = DATA[ii][1][pidx];
                ratings[pidx].experience = DATA[ii][2][pidx];
            }
            for (int pidx = 0; pidx < ratings.length; pidx++) {
                int newrat = Rating.computeRating(DATA[ii][0], ratings, pidx);
                int exprat = DATA[ii][3][pidx];
                assertTrue(ii + "." + pidx + ": " + newrat + " != " + exprat,
                           newrat == exprat);
//                 System.out.println(ii + "." + pidx + ": " +
//                                    DATA[ii][1][pidx] + " -> " + newrat);
            }
        }
    }

    public static Test suite ()
    {
        return new RatingUnitTest();
    }

    public static void main (String[] args)
    {
        RatingUnitTest test = new RatingUnitTest();
        test.runTest();
    }

    protected static int[][][] DATA = {
        // unable to compute
        {{ 5, -1 }, { 1400, 1400 }, { 5, 5 }, { -1, -1 }},
        // provisional beats provisional
        {{ 5, 1 }, { 1400, 1400 }, { 5, 5 }, { 1432, 1368 }},
        // provisional beats experienced
        {{ 5, 1 }, { 1400, 1400 }, { 0, 40 }, { 1432, 1376 }},
        // experienced beats provisional
        {{ 5, 1 }, { 1400, 1400 }, { 40, 0 }, { 1408, 1368 }},
        // experienced beats experienced
        {{ 5, 1 }, { 1400, 1200 }, { 40, 25 }, { 1408, 1192 }},
        // experienced beats experienced
        {{ 5, 1 }, { 1400, 1400 }, { 40, 40 }, { 1416, 1384 }},
        // high ranked experienced beats high ranked experienced
        {{ 5, 1 }, { 2200, 2200 }, { 40, 40 }, { 2212, 2188 }},
        // very high ranked experienced beats very high ranked experienced
        {{ 5, 1 }, { 2500, 2500 }, { 40, 40 }, { 2508, 2492 }},
        // simple three player
        {{ 5, 3, 1 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1232, 1200, 1168 }},
        // three player, winners tie
        {{ 5, 5, 1 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1216, 1216, 1168 }},
        // three player, losers tie
        {{ 5, 1, 1 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1232, 1184, 1184 }},
        // three player, missing one player
        {{ 5, -1, 1 }, { 1200, 2500, 1200 }, { 30, 30, 30 }, { 1216, -1, 1184 }},
        // three player, missing two players
        {{ 5, -1, -1 }, { 1200, 2500, 1200 }, { 30, 30, 30 }, { -1, -1, -1 }},
    };
}
