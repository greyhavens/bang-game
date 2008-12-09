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

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays a notification that the specified item requires coins and directs the uesr toward
 * purchasing them.
 */
public class NeedCoinsView extends BDecoratedWindow
    implements ActionListener
{
    public static final int WIDTH_HINT = 350;

    public NeedCoinsView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);
        ((GroupLayout)getLayoutManager()).setGap(20);

        _ctx = ctx;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setTitle(msgs.get("m.ncoins_title"));

        add(new BLabel(msgs.get("m.ncoins_info")));

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(25);
        bcont.add(new BButton(msgs.get("m.ncoins_to_bank"), this, "to_bank"));
        bcont.add(new BButton(msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // always clear
        _ctx.getBangClient().clearPopup(this, true);

        // only head to the bank if requested
        if ("to_bank".equals(event.getAction())) {
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.bankOid);
        }
    }

    protected BangContext _ctx;
}
