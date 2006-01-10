//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
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
    public PurseGood (int townIndex, int scripCost, int coinCost)
    {
        super(Purse.PURSE_TYPES[townIndex], scripCost, coinCost);
        _townIndex = townIndex;
    }

    /** A constructor only used during serialization. */
    public PurseGood ()
    {
    }

    /**
     * Returns the index of the town with which our purse is associated.
     */
    public int getTownIndex ()
    {
        return _townIndex;
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/purses/" + _type + ".png";
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
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
}
