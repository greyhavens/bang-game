//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

/**
 * Represents an invitation to be pardners.
 */
public class PardnerInvite extends Notification
{
    /** The name of the user that sent the invitation. */
    public Handle handle;

    /** The message associated with the invitation. */
    public String message;

    /**
     * Creates a new gang invitation on the server.
     */
    public PardnerInvite (Handle handle, String message, ResponseHandler handler)
    {
        super(handler);
        this.handle = handle;
        this.message = message;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public PardnerInvite ()
    {
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return "INVITE" + handle.toString();
    }

    @Override // documentation inherited
    public Handle getSource ()
    {
        return handle;
    }

    // documentation inherited
    public String getText ()
    {
        return MessageBundle.compose(
            "m.pardner_invite",
            MessageBundle.tcompose("m.pardner_invite_available", handle),
            MessageBundle.taint(handle), MessageBundle.taint(message));
    }

    @Override // documentation inherited
    public String[] getResponses ()
    {
        return new String[] { "m.pardner_accept", "m.pardner_reject" };
    }
}
