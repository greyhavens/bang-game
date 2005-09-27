//
// $Id$

package com.threerings.bang.bank.client;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bank.data.BankObject;

/**
 * Displays the main interface for the Bank.
 */
public class BankView extends BWindow
    implements PlaceView
{
    public BankView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeVStretch());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

        String qtitle = _ctx.xlate("bank", "m.quick_title");
        add(wrap(qtitle, _qsell = new QuickTransact(ctx, false),
                 _qbuy = new QuickTransact(ctx, true)), GroupLayout.FIXED);

        // add a blank container to suck up whitespace
        add(new BContainer());

        // add a row displaying our cash on hand and the back button
        BContainer bottom = new BContainer(GroupLayout.makeHStretch());
        add(bottom, GroupLayout.FIXED);

        bottom.add(new WalletLabel(ctx));
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        BankObject bankobj = (BankObject)plobj;
        _qbuy.init(bankobj);
        _qsell.init(bankobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    protected BContainer wrap (String title, BContainer left, BContainer right)
    {
        BContainer wrapper = new BContainer(new BorderLayout(5, 5));
        BLabel tlabel = new BLabel(title);
        tlabel.setHorizontalAlignment(BLabel.CENTER);
        tlabel.setLookAndFeel(BangUI.dtitleLNF);
        wrapper.add(tlabel, BorderLayout.NORTH);
        BContainer pair = new BContainer(GroupLayout.makeHStretch());
        pair.add(left);
        pair.add(right);
        wrapper.add(pair, BorderLayout.CENTER);
        return wrapper;
    }

    protected BangContext _ctx;
    protected QuickTransact _qbuy, _qsell;
}
