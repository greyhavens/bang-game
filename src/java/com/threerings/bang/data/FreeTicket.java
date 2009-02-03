//
// $Id$

package com.threerings.bang.data;

import java.sql.Timestamp;
import java.text.DateFormat;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangUtil;

/**
 * Represents a free ticket that grants access to a particular town for 24 hours from the time
 * of activation.
 */
public class FreeTicket extends Item
{
    /**
     * Check if a user qualifies for a free ticket.
     */
    public static FreeTicket checkQualifies (PlayerObject user, int townIdx)
    {
        townIdx++;

        // for now we're only giving out free ITP tickets
        if (townIdx != BangUtil.getTownIndex(BangCodes.INDIAN_POST) ||
            // check if they already have this free ticket
            user.stats.containsValue(StatType.FREE_TICKETS, BangCodes.TOWN_IDS[townIdx]) ||
            // check if they've already bought the ticket
            user.holdsTicket(BangCodes.TOWN_IDS[townIdx]) ||
            // make sure they have purchased the previous town ticket
            (townIdx > 1 && !user.holdsTicket(BangCodes.TOWN_IDS[townIdx - 1]))) {
            return null;
        }

        // see if they qualify
        if (user.stats.getIntStat(StatType.GAMES_PLAYED) >= FREE_ITP_REQUIREMENT) {
            return new FreeTicket(user.playerId, townIdx);
        }

        return null;
    }

    /** Blank constructor used during unserialization. */
    public FreeTicket ()
    {
    }

    /**
     * Creates a new free ticket for the specified town.
     */
    public FreeTicket (int ownerId, int townIndex)
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
     * Activates the ticket for 24 hour access.
     *
     * @return true if this ticket is not already activated.
     */
    public boolean activate (long timestamp)
    {
        if (isActivated()) {
            return false;
        }
        _activated = timestamp;
        return true;
    }

    /**
     * Returns true if this ticket has expired.
     */
    public boolean isExpired (long timestamp)
    {
        return (_activated != 0 && timestamp - _activated > EXPIRATION_TIME);
    }

    /**
     * Returns a timestamp for when the ticket will expire.
     */
    public Timestamp getExpire ()
    {
        return isActivated() ? new Timestamp(_activated + EXPIRATION_TIME) : null;
    }

    /**
     * Returns true if this ticket has been activated.
     */
    public boolean isActivated ()
    {
        return _activated != 0;
    }

    /**
     * Gets a formated date/time string for the expiration.
     */
    public String getExpireString ()
    {
        Timestamp expire = getExpire();
        return (expire == null ? null : DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT).format(getExpire()));
    }

    @Override // documentation inherited
    public String getName ()
    {
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + getTownId());
        msg = MessageBundle.compose("m.free_ticket", msg);
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        String msg = MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + getTownId());
        if (isActivated()) {
            msg = MessageBundle.compose(
                    "m.free_ticket_activated", msg, MessageBundle.taint(getExpireString()));
        } else {
            msg = MessageBundle.compose("m.free_ticket_unactivated", msg);
        }
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/tickets/free_" + getTownId() + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((FreeTicket)other)._townIndex == _townIndex;
    }

    protected int _townIndex;

    protected long _activated;

    /** The length of time a free ticket will work (24 hours). */
    protected static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L;

    /** Number of ranked games played to qualify for free ITP ticket. */
    protected static final int FREE_ITP_REQUIREMENT = 10;
}
