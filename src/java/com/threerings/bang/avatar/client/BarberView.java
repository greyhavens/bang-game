//
// $Id$

package com.threerings.bang.avatar.client;

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
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

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
        super(ctx.getLookAndFeel(), GroupLayout.makeHStretch());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

        final MessageBundle msgs = ctx.getMessageManager().getBundle("barber");

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
        _status.setBorder(new LineBorder(ColorRGBA.black));
        _status.setText(msgs.get("m.welcome"));
        _status.setLookAndFeel(BangUI.dtitleLNF);
        main.add(_status, GroupLayout.FIXED);

        // TODO: the avatar configuration interface
        main.add(new BContainer());

        // add a row displaying our cash on hand and the back button
        BContainer bottom = new BContainer(GroupLayout.makeHStretch());
        main.add(bottom, GroupLayout.FIXED);

        bottom.add(new WalletLabel(ctx));
        bottom.add(new TownButton(ctx), GroupLayout.FIXED);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    protected BangContext _ctx;
    protected BTextArea _status;
}
