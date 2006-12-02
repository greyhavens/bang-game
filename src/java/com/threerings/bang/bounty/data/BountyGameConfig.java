//
// $Id$

package com.threerings.bang.bounty.data;

import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;

/**
 * Describes the configuration of a bounty game.
 */
public class BountyGameConfig extends SimpleStreamableObject
{
    /** The type of scenario to be played. */
    public String scenario;

    /** The board on which to play. */
    public String board;

    /** The number of players in the game. */
    public int players;

    /** The type and number of units be used by each player . */
    public String[][] teams;

    /** Any additional criterion to be met in addition to winning the game. */
    public ArrayList<Criterion> criterion = new ArrayList<Criterion>();
}
