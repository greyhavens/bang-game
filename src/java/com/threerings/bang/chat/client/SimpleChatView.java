//
// $Id$

package com.threerings.bang.chat.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollingList;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.chat.client.SimpleChatView.SimpleMessage;
import com.threerings.bang.client.PlayerPopupMenu;
import com.jmex.bui.event.BEvent;

/**
 * Displays chat in a tab that's only names and messages.
 */
public class SimpleChatView extends BScrollingList<SimpleMessage, BLabel>
    implements ChatTab
{
    public SimpleChatView (BangContext ctx, Dimension size)
    {
        super();

        _ctx = ctx;
        _vport.setStyleClass("comic_chat_viewport");
        setPreferredSize(size);
    }

    // documentation inherited from interface ChatTab
    public void appendSent (String msg)
    {
        String filtered = _ctx.getChatDirector().filter(msg, null, true);
        if(!StringUtil.isBlank(filtered)) {
            appendSpoken(_ctx.getUserObject().handle, filtered);
        }
    }

    // documentation inherited from interface ChatTab
    public void appendReceived (UserMessage msg)
    {
        appendSpoken((Handle)msg.speaker, msg.message);
    }

    // documentation inherited from interface ChatTab
    public void appendSystem (ChatMessage msg)
    {
        String formated = SystemChatView.format(_ctx, msg);
        String style = SystemChatView.getAttentionLevel(msg) + "_chat_label";
        addValue(new SimpleMessage(null, formated, style), true);
    }

    // documentation inherited from interface ChatTab
    public void clear ()
    {
        removeValues();
    }

    @Override // from BScrollingList
    protected BLabel createComponent (SimpleMessage msg)
    {
        return msg.build();
    }

    protected void appendSpoken (Handle speaker, String message)
    {
        String formatted = "<" + speaker + "> " + message;
        String style = _ctx.getUserObject().handle.equals(speaker) ? 
            "sent_chat" : "received_chat";
        addValue(new SimpleMessage(speaker, formatted, style), true);
    }

    protected class SimpleMessage
    {
        public Handle handle;
        public String message;
        public String style;

        public SimpleMessage (Handle handle, String message, String style)
        {
            this.handle = handle;
            this.message = message;
            this.style = style;
        }

        public BLabel build ()
        {
            if (handle == null) {
                return new BLabel(message, style);
            }
            return new BLabel(message, style) {
                public boolean dispatchEvent (BEvent event) {
                    return PlayerPopupMenu.checkPopup(_ctx, getWindow(), event, handle, true) ||
                        super.dispatchEvent(event);
                }
            };
        }
    }

    protected BangContext _ctx;
}
