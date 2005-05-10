//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BWindow;
import com.jme.bui.layout.BorderLayout;

import com.threerings.jme.chat.ChatView;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.util.BangContext;

/**
 * Displays the interface for a Bang! lobby.
 */
public class LobbyView extends BWindow
    implements PlaceView
{
    public LobbyView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout());
        _ctx = ctx;

        _chatwin = new BWindow(ctx.getLookAndFeel(), new BorderLayout());
        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        _chatwin.add(_chat, BorderLayout.CENTER);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        int width = _ctx.getDisplay().getWidth();
        _chatwin.setBounds(10, 20, width-20, 100);
        _ctx.getInputDispatcher().addWindow(_chatwin);
        _ctx.getInterface().attachChild(_chatwin.getNode());
        _chat.willEnterPlace(plobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        _chat.didLeavePlace(plobj);
        _ctx.getInputDispatcher().removeWindow(_chatwin);
        _ctx.getGeometry().detachChild(_chatwin.getNode());
    }

    protected BangContext _ctx;

    /** Contain various onscreen displays. */
    protected BWindow _chatwin;

    /** Displays chat. */
    protected ChatView _chat;
}
