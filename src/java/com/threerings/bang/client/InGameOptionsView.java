//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BButton;
import com.jme.bui.BDecoratedWindow;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays options while a player is in a game.
 */
public class InGameOptionsView extends BDecoratedWindow
    implements ActionListener
{
    public InGameOptionsView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _modal = true;
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("options");

        add(createButton("resume_game"));
        add(createButton("leave_game"));
        add(createButton("quit"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        log.info("Action " + event);
        String action = event.getAction();
        if ("resume_game".equals(action)) {
            dismiss();

        } else if ("leave_game".equals(action)) {
            _ctx.getLocationDirector().moveBack();
            dismiss();

        } else if ("quit".equals(action)) {
            _ctx.getApp().stop();
        }
    }

    protected BButton createButton (String action)
    {
        BButton btn = new BButton(_msgs.get("m." + action), action);
        btn.addListener(this);
        return btn;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
