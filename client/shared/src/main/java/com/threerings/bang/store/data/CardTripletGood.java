//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.card.Card;

/**
 * Represents a three pack of a known card type that is for sale.
 */
public class CardTripletGood extends Good
{
    /**
     * Creates a good representing the specified card.
     */
    public CardTripletGood (String cardType, String townId, int scripCost, int coinCost,
                            Badge.Type qualifier)
    {
        super("card_trip_" + cardType, townId, scripCost, coinCost, CARD_TRIPLET_PRIORITY);
        _cardType = cardType;
        _qualifier = qualifier;
    }

    /** A constructor only used during serialization. */
    public CardTripletGood ()
    {
    }

    /**
     * Returns the type of card created by this good.
     */
    public String getCardType ()
    {
        return _cardType;
    }

    /**
     * Sets the quantity of this good.  This is used on a client side for displaying how many of
     * this good is owned by the player.
     */
    public void setQuantity (int quantity)
    {
        _quantity = quantity;
    }

    @Override // from Good
    public String getIconPath ()
    {
        return Card.newCard(_cardType).getIconPath("card_pack");
    }

    @Override // from Good
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        // if this card pack has a badge qualifier, make sure the user holds that badge (and allow
        // admins/support access to everything)
        return _qualifier == null || user.tokens.isSupport() || user.holdsBadge(_qualifier);
    }

    @Override // from Good
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + _cardType);
    }

    @Override // from Good
    public String getTip ()
    {
        String msg = MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + _cardType);
        msg = MessageBundle.compose("m.card_trip_tip", msg);
        msg = MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
        msg = MessageBundle.compose("m.card_tip_cont", msg, "m." + _cardType + "_tip");
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, msg);
    }

    @Override // from Good
    public String getToolTip ()
    {
        String msg = getTip();
        msg = MessageBundle.compose("m.card_tool_tip", msg, MessageBundle.taint("" + _quantity));
        return MessageBundle.qualify(BangCodes.CARDS_MSGS, msg);
    }

    protected String _cardType;
    protected Badge.Type _qualifier;

    protected transient int _quantity;

    protected static final int CARD_TRIPLET_PRIORITY = 4;
}
