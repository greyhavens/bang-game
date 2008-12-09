//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Notification;

/**
 * Represents an invitation to be a member of a gang.
 */
public class GangInvite extends Notification
{
    /** The name of the user that sent the invitation. */
    public Handle inviter;

    /** The name of the gang to which the user is invited. */
    public Handle gang;

    /** The message associated with the invitation. */
    public String message;

    /**
     * Creates a new gang invitation on the server.
     */
    public GangInvite (
        Handle inviter, Handle gang, String message, ResponseHandler handler)
    {
        super(handler);
        this.inviter = inviter;
        this.gang = gang;
        this.message = message;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public GangInvite ()
    {
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return "GANG" + gang.toString();
    }

    @Override // documentation inherited
    public Handle getSource ()
    {
        return inviter;
    }

    @Override // documentation inherited
    public String getBundle ()
    {
        return GangCodes.GANG_MSGS;
    }

    @Override // documentation inherited
    public String getTitle ()
    {
        return "t.gang_invite";
    }

    // documentation inherited
    public String getText ()
    {
        return MessageBundle.tcompose("m.gang_invite", inviter, message, gang);
    }

    @Override // documentation inherited
    public String[] getResponses ()
    {
        return new String[] { "m.invite_accept", "m.invite_reject" };
    }
}
