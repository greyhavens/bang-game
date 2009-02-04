//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TrainTicket;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.store.data.Good;

/**
 * Makes available a ticket that provides access to a town.
 */
public class TicketGood extends Good
{
    public TicketGood ()
    {
    }

    public TicketGood (String townId, int scripCost, int coinCost)
    {
        super(townId + "_ticket", townId, scripCost, coinCost, TICKET_PRIORITY);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/tickets/" + getTownId() + ".png";
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
                MessageBundle.compose("m.train_ticket", "m." + getTownId()));
    }

    @Override // documentation inherited
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
            MessageBundle.compose("m.train_ticket_tip", "m." + getTownId()));
    }

    @Override // documentation inherited
    public boolean honorsGoldPass ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean isAvailable (PlayerObject user)
    {
        return user.holdsTicket(getTownId());
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new TrainTicket(playerId, BangUtil.getTownIndex(getTownId()));
    }

    @Override // documentation inherited
    public boolean wouldCreateItem (Item item)
    {
        if (!(item instanceof TrainTicket)) {
            return false;
        }
        return ((TrainTicket)item).getTownId().equals(getTownId());
    }

    protected static final int TICKET_PRIORITY = 10;
}
