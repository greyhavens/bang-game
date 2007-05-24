//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

/**
 * Allows a player to use the gold exchange.
 */
public class ExchangePass extends Item
{
    /** Creates the pass. */
    public ExchangePass (int ownerId)
    {
        super(ownerId);
    }

    /** Blank construction for serialization. */
    public ExchangePass ()
    {
    }

    @Override // documentation inherited
    public String getName()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.exchange_pass");
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(
                BangCodes.GOODS_MSGS, "m.exchange_pass_tip");
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/passes/exchange.png";
    }
}
