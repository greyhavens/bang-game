//
// $Id$

package com.threerings.bang.tests.rating;

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
            for (int pidx = 0; pidx < DATA[ii][0].length; pidx++) {
                int newrat = Rating.computeRating(
                    DATA[ii][0], DATA[ii][1], DATA[ii][2], pidx);
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
        // provisional beats provisional
        {{ 25, 10 }, { 1400, 1400 }, { 5, 5 }, { 1432, 1368 }},
        // provisional beats experienced
        {{ 25, 10 }, { 1400, 1400 }, { 0, 40 }, { 1432, 1376 }},
        // experienced beats provisional
        {{ 25, 10 }, { 1400, 1400 }, { 40, 0 }, { 1408, 1368 }},
        // experienced beats experienced
        {{ 25, 10 }, { 1400, 1200 }, { 40, 25 }, { 1408, 1192 }},
        // experienced beats experienced
        {{ 25, 10 }, { 1400, 1400 }, { 40, 40 }, { 1416, 1384 }},
        // high ranked experienced beats high ranked experienced
        {{ 25, 10 }, { 2200, 2200 }, { 40, 40 }, { 2212, 2188 }},
        // very high ranked experienced beats very high ranked experienced
        {{ 25, 10 }, { 2500, 2500 }, { 40, 40 }, { 2508, 2492 }},
        // simple three player
        {{ 10, 7, 5 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1232, 1200, 1168 }},
        // three player, winners tie
        {{ 10, 10, 5 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1216, 1216, 1168 }},
        // three player, losers tie
        {{ 10, 5, 5 }, { 1200, 1200, 1200 }, { 0, 0, 0 }, { 1232, 1184, 1184 }},
    };
}
