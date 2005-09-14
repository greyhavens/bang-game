//
// $Id$

package com.threerings.bang.data;

/**
 * Enables a player to retain more money (per round) at the end of a game.
 */
public class Purse extends Item
{
    /** A default constructor used for serialization. */
    public Purse ()
    {
    }

    /**
     * Creates a purse that allows the player to retain up to the
     * specified amount of cash per round.
     */
    public Purse (int ownerId, int perRoundCash)
    {
        super(ownerId);
        _perRoundCash = perRoundCash;
    }

    /**
     * Returns the amount of money that a player holding this purse may
     * retain (per round) from a game.
     */
    public int getPerRoundCash ()
    {
        return _perRoundCash;
    }

    protected int _perRoundCash;
}
