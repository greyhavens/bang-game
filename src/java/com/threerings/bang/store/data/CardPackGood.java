//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.coin.server.persist.CoinTransaction;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

/**
 * Represents a pack of cards that are for sale.
 */
public class CardPackGood extends Good
{
    /** The identifier of a custom message delivered to the player's user
     * object when they buy a pack of cards. */
    public static final String PURCHASED_CARDS = "PurchasedCards";

    /**
     * Creates a good representing a pack of cards of the specified size.
     */
    public CardPackGood (int size, int scripCost, int coinCost)
    {
        super("card_pack" + size, scripCost, coinCost);
        _size = size;
    }

    /** A constructor only used during serialization. */
    public CardPackGood ()
    {
    }

    /**
     * Returns the number of cards in the pack.
     */
    public int getSize ()
    {
        return _size;
    }

    @Override // from Good
    public String getIconPath ()
    {
        return "goods/cards/" + _type + ".png";
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        // anyone can buy a pack of cards
        return true;
    }

    @Override // from Good
    public String getTip ()
    {
        String msg = MessageBundle.tcompose("m.card_tip", String.valueOf(_size));
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // from Good
    public int getCoinType ()
    {
        return CoinTransaction.CARD_PURCHASE;
    }

    protected int _size;
}
