//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.Purse;

/**
 * Represents a purse that is for sale.
 */
public class PurseGood extends Good
{
    /**
     * Creates a good representing the purse associated with the specified
     * town.
     */
    public PurseGood (int townIndex, int scripCost, int goldCost)
    {
        super(PURSE_TYPES[townIndex], scripCost, goldCost);
        _townIndex = townIndex;
    }

    /**
     * Returns the index of the town with which our purse is associated.
     */
    public int getTownIndex ()
    {
        return _townIndex;
    }

    @Override // documentation inherited
    public boolean isAvailable (BangUserObject user)
    {
        // make sure they don't already have a better purse
        return (user.getPurse().getTownIndex() < _townIndex);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        String msg = MessageBundle.tcompose(
            "m.purse_tip", String.valueOf(Purse.PER_ROUND_CASH[_townIndex]));
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    protected int _townIndex;

    protected static final String[] PURSE_TYPES = {
        "default" /* not used */, "frontier_purse", "indian_purse",
        "boom_purse", "ghost_purse", "gold_purse" };
}
