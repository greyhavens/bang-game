//
// $Id$

package com.threerings.bang.store.data;

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
    public CardPackGood (int size, String townId, int scripCost, int coinCost)
    {
        super("card_pack" + size, townId, scripCost, coinCost, CARD_PACK_PRIORITY);
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
    public boolean honorsGoldPass ()
    {
        return true;
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

    protected int _size;

    protected static final int CARD_PACK_PRIORITY = 5;
}
