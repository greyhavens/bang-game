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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_type", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_townId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_scripCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_coinCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_priority", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_type", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_townId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_scripCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_coinCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_priority", this, out);
    }
}
