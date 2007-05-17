//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.StringUtil;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

/**
 * Represents a notification delivered to the user that requires a response
 * (if only confirmation that the user received the notification).
 */
public abstract class Notification
    implements DSet.Entry
{
    /** The interface used to communicate the response on the server. */
    public interface ResponseHandler
    {
        /**
         * Handles the user's response to the notification.
         *
         * @param resp the response index
         * @param listener a listener to notify with confirmation of
         * success
         */
        public void handleResponse (int resp, InvocationService.ConfirmListener listener)
            throws InvocationException;
    }

    /** A commonly used response indicating acceptance (of an invitation, e.g.) */
    public static final int ACCEPT = 0;

    /** A commonly used response indicating rejection. */
    public static final int REJECT = 1;

    /** On the server, the handler that will receive the player's response. */
    public transient ResponseHandler handler;

    /** Set on the client when the user has responded to the notification. */
    public transient boolean responded;

    /**
     * Creates a new notification on the server.
     */
    public Notification (ResponseHandler handler)
    {
        this.handler = handler;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Notification ()
    {
    }

    /**
     * Returns the source of the notification, if it originated from a player.
     */
    public Handle getSource ()
    {
        return null;
    }

    /**
     * Returns the bundle to use to translate the notification messages.
     */
    public String getBundle ()
    {
        return BangCodes.BANG_MSGS;
    }

    /**
     * Returns the translatable title of the notification, or <code>null</code> for
     * none.
     */
    public String getTitle ()
    {
        return null;
    }

    /**
     * Returns the translatable text of the notification.
     */
    public abstract String getText ();

    /**
     * Returns the translatable options available to respond to the
     * notification.
     */
    public String[] getResponses ()
    {
        return new String[] { "m.ok" };
    }

    /**
     * Returns the delay in seconds before the buttons become active.
     */
    public int getEnabledDelay ()
    {
        return 0;
    }

    /**
     * Returns the index of the response to use when automatically denying
     * notifications from muted sources.
     */
    public int getRejectIndex ()
    {
        return REJECT;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[" + StringUtil.shortClassName(this) + ": ");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }
}
