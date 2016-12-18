//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.SteelWindow;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Ensures a user is over 13 before allowing them into interactive areas.
 */
public class CoppaView extends SteelWindow
    implements ActionListener
{
    public CoppaView (BangContext ctx)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.coppa_title"));
        setModal(true);
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
        _contents.setStyleClass("padded");

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);

        _contents.add(new BLabel(_msgs.get("m.coppa_info"), "dialog_text"));

        _buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _ctx.getBangClient().clearPopup(this, true);
        _ctx.getBangClient().resetTownView();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
