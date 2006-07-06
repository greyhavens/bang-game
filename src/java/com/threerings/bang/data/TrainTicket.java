//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

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
    public TrainTicket (int ownerId, String townId)
    {
        super(ownerId);
        _townId = townId;
    }

    /**
     * Returns the town to which this ticket provides access.
     */
    public String getTownId ()
    {
        return _townId;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.taint(_townId); // TODO
    }

    @Override // documentation inherited
    public String getTooltip ()
    {
        return ""; // TODO
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return ""; // TODO
    }

    protected String _townId;
}
