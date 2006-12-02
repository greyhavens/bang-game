//
// $Id$

package com.threerings.bang.bounty.data;

import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;

/**
 * Describes the configuration of a particular bounty.
 */
public class BountyConfig extends SimpleStreamableObject
{
    /** The town in which this bounty is available. */
    public String townId;

    /** The amount of script required to purchase access to this bounty. */
    public int scripCost;

    /** The number of coins required to purchase access to this bounty. */
    public int coinCost;

    /** The configuration of the individual bounty games. */
    public ArrayList<BountyGameConfig> games = new ArrayList<BountyGameConfig>();

    /** Whether or not the bounty games must be played in order. */
    public boolean inOrder;

    /** The rewards earned by completing this bounty. */
    public ArrayList<Reward> rewards = new ArrayList<Reward>();
}
