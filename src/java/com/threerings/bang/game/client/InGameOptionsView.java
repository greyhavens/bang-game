//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

/**
 * Displays options while a player is in a game.
 */
public class InGameOptionsView extends BDecoratedWindow
    implements ActionListener
{
    public InGameOptionsView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _modal = true;
        _ctx = ctx;

        MessageBundle msgs = ctx.getMessageManager().getBundle("options");
        add(new BButton(msgs.get("m.resume_game"), this, "dismiss"));
        add(new BButton(msgs.get("m.leave_game"), this, "leave_game"));
        add(new BButton(msgs.get("m.quit"), this, "quit"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("leave_game".equals(action)) {
            _ctx.getLocationDirector().moveBack();
            dismiss();
        } else if ("dismiss".equals(action)) {
            dismiss();
        } else if ("quit".equals(action)) {
            _ctx.getApp().stop();
        }
    }

    protected BangContext _ctx;
}
