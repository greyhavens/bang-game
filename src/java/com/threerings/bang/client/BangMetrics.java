//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

/**
 * Defines sizes of things.
 */
public class BangMetrics
{
    /** The length of the edge of one board square. */
    public static final int SQUARE = 40;

    /** The dimensions of a single board tile. */
    public static final float TILE_SIZE = 10;

    /** Our definition of the left vector. */
    public static final Vector3f LEFT = new Vector3f(1, 0, 0);

    /** Our definition of the up vector. */
    public static final Vector3f UP = new Vector3f(0, 0, 1);

    /** The direction our models are facing (this has nothing to do with
     * the coordinate system or the up and left vectors). */
    public static final Vector3f FORWARD = new Vector3f(0, -1, 0);

    /** Java colors for each of the players. */
    public static final Color[] PIECE_COLORS = {
        Color.gray,
        new Color(0.08f, 0.33f, 1f, 1f),
        Color.red,
        Color.green,
        Color.yellow,
        new Color(1f, 0.47f, 0f, 1f),
        new Color(0.08f, 0.73f, 1f, 1f),
        new Color(0.43f, 0.12f, 1f, 1f),
    };

    /** JME colors for each of the players. */
    public static final ColorRGBA[] JPIECE_COLORS = {
        ColorRGBA.gray,
        new ColorRGBA(0.08f, 0.33f, 1f, 1f),
        ColorRGBA.red,
        ColorRGBA.green,
        new ColorRGBA(1, 1, 0, 1),
        new ColorRGBA(1f, 0.47f, 0f, 1f),
        new ColorRGBA(0.08f, 0.73f, 1f, 1f),
        new ColorRGBA(0.43f, 0.12f, 1f, 1f),
    };

    /** Darker JME colors for each of the players. */
    public static final ColorRGBA[] DARKER_COLORS = {
        JPIECE_COLORS[0].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[1].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[2].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[3].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[4].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[5].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[6].mult(ColorRGBA.darkGray),
        JPIECE_COLORS[7].mult(ColorRGBA.darkGray),
    };

    /** Color indices for the teams. */
    public static final int[][] TEAM_COLOR = { { 1, 6, 7 }, {2, 5}, {4} };

    /**
     * Get the Java piece color based on the current team lookup.
     */
    public static Color getPieceColor (int owner) {
        return PIECE_COLORS[colorLookup[owner+1]];
    }

    /**
     * Get the JME piece color based on the current team lookup.
     */
    public static ColorRGBA getJPieceColor (int owner) {
        return JPIECE_COLORS[colorLookup[owner+1]];
    }

    /**
     * Get the darker JME piece color based on the current team lookup.
     */
    public static ColorRGBA getDarkerPieceColor (int owner) {
        return DARKER_COLORS[colorLookup[owner+1]];
    }

    /**
     * Generates an int array of color indices for all players.
     */
    public static void generateColorLookup (int[] teams)
    {
        if (teams == null) {
            colorLookup = new int[] { 0, 1, 2, 3, 4 };
            return;
        }
        int[] teamsize = new int[teams.length];
        boolean[] isteam = new boolean[teams.length];
        int[] teamidx = new int[teams.length];
        colorLookup = new int[teams.length + 1];
        colorLookup[0] = 0;
        int maxsize = 0;
        int numteams = 0;
        for (int ii = 0; ii < teams.length; ii++) {
            int tidx = teams[ii];
            teamsize[tidx]++;
            if (teamsize[tidx] > maxsize) {
                maxsize++;
            }
            if (!isteam[tidx]) {
                numteams++;
                isteam[tidx] = true;
            }
            teamidx[ii] = teamsize[tidx] - 1;
        }

        // 1v1v1v1
        if (maxsize == 1) {
            for (int ii = 0; ii < teams.length; ii++) {
                colorLookup[ii+1] = teams[ii] + 1;
            }

        // coop game
        } else if (numteams == 1) {
            for (int ii = 0; ii < teams.length; ii++) {
                colorLookup[ii+1] = ii + 1;
            }

        // 3v1
        } else if (maxsize == 3) {
            for (int ii = 0; ii < teams.length; ii++) {
                colorLookup[ii+1] = TEAM_COLOR[(teamsize[teams[ii]] == 3 ? 0 : 2)][teamidx[ii]];
            }

        // 2v2 or 2v1
        } else if (numteams == 2) {
            int firstteam = 0;
            for (int ii = 0; ii < isteam.length; ii++) {
                if (isteam[ii]) {
                    firstteam = ii;
                    break;
                }
            }
            for (int ii = 0; ii < teams.length; ii++) {
                colorLookup[ii+1] = TEAM_COLOR[(firstteam == teams[ii] ? 0 : 1)][teamidx[ii]];
            }

        // 2v1v1
        } else {
            int firstteam = 0;
            for (int ii = 0; ii < isteam.length; ii++) {
                if (isteam[ii] && teamsize[ii] < 2) {
                    firstteam = ii;
                    break;
                }
            }

            for (int ii = 0; ii < teams.length; ii++) {
                int tidx = teams[ii];
                int cidx = teamsize[tidx] == 2 ? 0 : (tidx == firstteam ? 1 : 2);
                colorLookup[ii+1] = TEAM_COLOR[cidx][teamidx[ii]];
            }
        }
    }

    /** The color mapping from player indices to player color. */
    public static int[] colorLookup = { 0, 1, 2, 3, 4 };
}
