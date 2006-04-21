//
// $Id$

package com.threerings.bang.client;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.util.BangContext;

/**
 * Handles custom chat bits for Bang.
 */
public class BangChatDirector extends ChatDirector
{
    public BangChatDirector (BangContext ctx)
    {
        super(ctx, ctx.getMessageManager(), BangCodes.CHAT_MSGS);
        _ctx = ctx;
        // nothing currently doing, but eventually we'll have something
    }

    protected BangContext _ctx;
}
