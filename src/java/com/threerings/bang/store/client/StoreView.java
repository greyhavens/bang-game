//
// $Id$

package com.threerings.bang.store.client;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BWindow;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.StoreObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the General Store.
 */
public class StoreView extends BWindow
    implements PlaceView
{
    public StoreView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeHStretch());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
        _msgs = ctx.getMessageManager().getBundle("store");

        String townId = _ctx.getUserObject().townId;

        // the left column contains some fancy graphics
        BContainer left = new BContainer(GroupLayout.makeVert(GroupLayout.TOP));
        add(left, GroupLayout.FIXED);
        String path = "ui/" + townId + "/shopkeeper.png";
        left.add(new BLabel(new ImageIcon(_ctx.loadImage(path))));

        // then we have a blurb, cash on hand, the list of goods, and the goods
        // inspector all stacked one atop another
        BContainer main = new BContainer(GroupLayout.makeVStretch());
        add(main);

        _status = new BTextArea();
        _status.setPreferredSize(new Dimension(100, 100));
        _status.setBorder(new LineBorder(ColorRGBA.black));
        _status.setText(_msgs.get("m.intro_tip"));
        _status.setLookAndFeel(BangUI.dtitleLNF);
        main.add(_status, GroupLayout.FIXED);

        BContainer mcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        mcont.add(new WalletLabel(_ctx));
        main.add(mcont, GroupLayout.FIXED);

        // create our goods inspector first as the goods palette will need it
        _inspector = new GoodsInspector(_ctx, this, _status);

        // the display of goods for sale
        main.add(_goods = new GoodsPalette(_ctx, _inspector));

        // the bottom contains the goods inspector and "to town" button
        GroupLayout lay = GroupLayout.makeHStretch();
        lay.setOffAxisPolicy(GroupLayout.CONSTRAIN);
        lay.setOffAxisJustification(GroupLayout.BOTTOM);
        BContainer bottom = new BContainer(lay);
        bottom.add(_inspector);
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);
        main.add(bottom, GroupLayout.FIXED);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        // configure our goods palette and inspector
        _goods.init((StoreObject)plobj);
        _inspector.init((StoreObject)plobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    /**
     * Called by the palette when a good has been purchased.
     */
    protected void goodPurchased ()
    {
        _goods.reinitGoods(false);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected GoodsPalette _goods;
    protected GoodsInspector _inspector;
    protected BTextArea _status;
}
