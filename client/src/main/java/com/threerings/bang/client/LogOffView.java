//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * A window that pops up if the client logs off with a message.
 */
public class LogOffView extends BDecoratedWindow
    implements ActionListener
{
    public LogOffView (BangContext ctx, String msg)
    {
        super(ctx.getStyleSheet(), ctx.xlate(BangCodes.BANG_MSGS, "l." + msg + "_title"));

        _ctx = ctx;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setPreferredSize(new Dimension(600, 200));

        add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m." + msg)));
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(25);
        bcont.add(_quit = new BButton(msgs.get("m.quit"), this, "quit"));
        bcont.add(_restart = new BButton(msgs.get("m.restart"), this, "restart"));
        add(bcont, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("restart".equals(event.getAction())) {
            _restart.setEnabled(false);
            if (!BangClient.relaunchGetdown(_ctx, 500L)) {
                log.info("Failed to restart Bang, exiting");
                _ctx.getApp().stop();
            }
        } else if ("quit".equals(event.getAction())) {
            _quit.setEnabled(false);
            _ctx.getApp().stop();
        }
    }

    protected BangContext _ctx;
    protected BButton _quit, _restart;
}
