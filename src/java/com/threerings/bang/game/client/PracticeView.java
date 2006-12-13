//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.data.BangObject;

/**
 * Displays an "End Practice Session" button.
 */
public class PracticeView extends BWindow
    implements ActionListener
{
    public PracticeView (BangContext ctx, BangObject bangobj)
    {
        super(ctx.getStyleSheet(), new BorderLayout());

        _ctx = ctx;
        _bangobj = bangobj;
        MessageBundle msgs = ctx.getMessageManager().getBundle("game");
        BButton btn = new BButton(
                msgs.get("m.end_practice"), this, "end_practice");
        btn.setStyleClass("big_button");
        add(btn, BorderLayout.CENTER);
    }

    // inherited from ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("end_practice".equals(action)) {
            _ctx.getLocationDirector().moveTo(_ctx.getBangClient().getPriorLocationOid());
        }
    }

    protected BangObject _bangobj;
    protected BangContext _ctx;
}
