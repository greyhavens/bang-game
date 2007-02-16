//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.station.data.StationCodes;

/**
 * Represents a train ticket purchased by the player giving them access to a
 * particular town.
 */
public class TrainTicket extends Item
{
    /** A blank constructor used during unserialization. */
    public TrainTicket ()
    {
    }

    /** Creates a new ticket for the specified town. */
    public TrainTicket (int ownerId, int townIndex)
    {
        super(ownerId);
        _townIndex = townIndex;
    }

    /**
     * Returns the index of the town to which this ticket provides access.
     */
    public int getTownIndex ()
    {
        return _townIndex;
    }

    /**
     * Returns the town to which this ticket provides access.
     */
    public String getTownId ()
    {
        return BangCodes.TOWN_IDS[_townIndex];
    }

    /**
     * Returns the cost of this ticket in scrip.
     */
    public int getScripCost ()
    {
        return StationCodes.TICKET_SCRIP[getTownIndex()];
    }

    /**
     * Returns the cost of this ticket in coins.
     */
    public int getCoinCost ()
    {
        return StationCodes.TICKET_COINS[getTownIndex()];
    }

    @Override // documentation inherited
    public String getName ()
    {
        String msg = MessageBundle.qualify(
            BangCodes.BANG_MSGS, "m." + getTownId());
        msg = MessageBundle.compose(
            getItemId() == 0 ? "m.temporary_ticket" : "m.train_ticket", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        String msg = MessageBundle.qualify(
            BangCodes.BANG_MSGS, "m." + getTownId());
        msg = MessageBundle.compose("m.train_ticket_tip", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/tickets/" + getTownId() + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) &&
            ((TrainTicket)other)._townIndex == _townIndex;
    }

    protected int _townIndex;
}
