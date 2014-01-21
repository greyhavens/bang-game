//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;

/**
 * Represents a purse that is for sale.
 */
public class PurseGood extends Good
{
    /** The scrip cost of purses indexed by town. */
    public static final int[] SCRIP_COST = { 1500, 3000, 5000, 7500, 15000 };

    /** The coin cost of purses indexed by town. */
    public static final int[] COIN_COST = { 1, 2, 4, 5, 8 };

    /**
     * Creates a good representing the purse associated with the specified town.
     */
    public PurseGood (int townIndex)
    {
        super(Purse.PURSE_TYPES[townIndex+1], BangCodes.TOWN_IDS[townIndex],
                SCRIP_COST[townIndex], COIN_COST[townIndex], PURSE_PRIORITY);
        // annoyingly Purse maintains townIndex+1 not actual townIndex
        _townIndex = townIndex+1;
    }

    /** A constructor only used during serialization. */
    public PurseGood ()
    {
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return Purse.getIconPath(_townIndex);
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return Purse.getDescrip(_townIndex);
    }

    @Override // from Good
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        // make sure they have the previous town's purse
        return (user.getPurse().getTownIndex() == _townIndex-1);
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new Purse(playerId, _townIndex);
    }

    protected int _townIndex;

    protected static final int PURSE_PRIORITY = 15;
}
