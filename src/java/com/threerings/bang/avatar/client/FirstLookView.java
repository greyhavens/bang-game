//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BTextArea;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;

/**
 * Displays an interface via which the player can create their first (and
 * default) avatar look.
 */
public class FirstLookView extends BDecoratedWindow
    implements ActionListener
{
    public FirstLookView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        _status = new BTextArea(_msgs.get("m.first_look_tip"));
        _status.setPreferredWidth(500);

        BTextArea intro = new BTextArea(_msgs.get("m.first_look_intro"));
        intro.setPreferredWidth(500);
        add(intro, GroupLayout.FIXED);
        add(new NewLookView(ctx, _status, true));
        add(_status, GroupLayout.FIXED);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        BButton done;
        buttons.add(done = new BButton(_msgs.get("m.done")));
        done.addListener(this);
        add(buttons, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        dismiss();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BTextArea _status;
}
