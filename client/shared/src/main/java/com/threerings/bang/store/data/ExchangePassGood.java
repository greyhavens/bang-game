//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.ExchangePass;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;

/**
 * Used to sell the Exhange Pass.
 */
public class ExchangePassGood extends Good
{
    /** There is just a token gold cost to get the player to buy gold. */
    public static final int COIN_COST = 1;
    public static final int SCRIP_COST = 0;

    /**
     * Creates a good representing the exhange pass.
     */
    public ExchangePassGood (String townId)
    {
        super("exchange_pass", townId, SCRIP_COST, COIN_COST, EXCHANGE_PRIORITY);
    }

    public ExchangePassGood ()
    {
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/passes/exchange.png";
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.exchange_pass_tip");
    }

    @Override // documentation inherited
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        return !user.canExchange();
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new ExchangePass(playerId);
    }

    protected static final int EXCHANGE_PRIORITY = 1;
}
