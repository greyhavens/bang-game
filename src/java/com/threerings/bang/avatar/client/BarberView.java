//
// $Id$

package com.threerings.bang.avatar.client;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main barber interface wherein the player can create new "looks"
 * for their avatar and purchase them.
 */
public class BarberView extends BWindow
    implements PlaceView
{
    public BarberView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeHStretch());
        setStyleClass("main_view");

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

        final MessageBundle msgs = ctx.getMessageManager().getBundle(
            BarberCodes.BARBER_MSGS);

        String townId = _ctx.getUserObject().townId;

        // the left column contains some fancy graphics
        BContainer left = new BContainer(GroupLayout.makeVert(GroupLayout.TOP));
        add(left, GroupLayout.FIXED);
        String path = "ui/" + townId + "/barber.png";
        left.add(new BLabel(new ImageIcon(_ctx.loadImage(path))));

        // in the main area we have the main thing
        BContainer main = new BContainer(GroupLayout.makeVStretch());
        add(main);

        _status = new BTextArea();
        _status.setPreferredSize(new Dimension(100, 100));
        _status.setText(msgs.get("m.welcome"));
        _status.setStyleClass("dialog_title");
        main.add(_status, GroupLayout.FIXED);

        // put our new look and change clothes interfaces in tabs
        BTabbedPane tabs = new BTabbedPane(GroupLayout.CENTER);
        _newlook = new NewLookView(ctx, _status, false);
        tabs.addTab(msgs.get("m.new_look"), wrap(_newlook));
        _wearclothes = new WearClothingView(ctx, _status);
        tabs.addTab(msgs.get("m.wear_clothes"), wrap(_wearclothes));
        main.add(tabs);

        // add a row displaying our cash on hand and the back button
        BContainer bottom = new BContainer(GroupLayout.makeHStretch());
        main.add(bottom, GroupLayout.FIXED);

        bottom.add(new WalletLabel(ctx, false));
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        BarberObject barbobj = (BarberObject)plobj;
        _newlook.setBarberObject(barbobj);
        _wearclothes.setBarberObject(barbobj);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    /** UI helper function. */
    protected BContainer wrap (BContainer comp)
    {
        BContainer wrapper = GroupLayout.makeHBox(GroupLayout.CENTER);
        wrapper.add(comp);
        return wrapper;
    }

    protected BangContext _ctx;
    protected BTextArea _status;
    protected NewLookView _newlook;
    protected WearClothingView _wearclothes;
}
