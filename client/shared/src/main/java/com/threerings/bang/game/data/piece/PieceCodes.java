//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Codes an constants used by {@link Piece} and its friends.
 */
public interface PieceCodes
{
    /** An orientation code. */
    public static final int NORTH = 0;

    /** An orientation code. */
    public static final int EAST = 1;

    /** An orientation code. */
    public static final int SOUTH = 2;

    /** An orientation code. */
    public static final int WEST = 3;

    /** Contains all the four directions. */
    public static final int[] DIRECTIONS = { NORTH, EAST, SOUTH, WEST };

    /** The directions in line with the x axis. */
    public static final int[] X_DIRECTIONS = { WEST, EAST };

    /** The directions in line with the y axis. */
    public static final int[] Y_DIRECTIONS = { NORTH, SOUTH };

    /** The delta applied to x if we are moving in each direction. */
    public static final int[] DX = { 0, 1, 0, -1 };

    /** The delta applied to y if we are moving in each direction. */
    public static final int[] DY = { -1, 0, 1, 0 };

    /** Clockwise. */
    public static final int CW = 0;

    /** Counter-clockwise. */
    public static final int CCW = 1;
}
