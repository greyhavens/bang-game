//
// $Id$

package com.threerings.bang.chat.client;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.data.ChatMessage;

/**
 * An interface for chat views that are used by PlaceChatView.
 */
public interface ChatTab
{
    /**
     * Appends a message sent by the local user (only used in circumstances where a player's chat
     * is not normally echoed back to them).
     */
    public void appendSent (String msg);

    /**
     * Appends a message received from the chat director.
     */
    public void appendReceived (UserMessage msg);

    /**
     * Appends a message received from the system.
     */
    public void appendSystem (ChatMessage msg);

    /**
     * Clears out all displayed messages.
     */
    public void clear ();
}
