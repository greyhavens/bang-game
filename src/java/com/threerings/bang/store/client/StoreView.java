//
// $Id$

package com.threerings.bang.store.client;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BWindow;
import com.jme.bui.util.Dimension;
import com.jme.renderer.ColorRGBA;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.StatusView;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays the main interface for the General Store.
 */
public class StoreView extends BWindow
    implements ActionListener, PlaceView
{
    public StoreView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        setBorder(new EmptyBorder(5, 5, 5, 5));
        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
        _msgs = ctx.getMessageManager().getBundle("store");

        // we cover the whole screen
        setBounds(0, 0, ctx.getDisplay().getWidth(),
                  ctx.getDisplay().getHeight());

        // display the status view when the player presses escape
        setModal(true);
        new StatusView(_ctx).bind(this);

        // TODO: the display of items for sale
        add(new BContainer(), BorderLayout.CENTER);

        BContainer side = new BContainer(GroupLayout.makeVert(GroupLayout.TOP));
        add(side, BorderLayout.EAST);

        // TODO: add the item inspector
        BContainer iinsp = new BContainer();
        iinsp.setPreferredSize(new Dimension(150, 200));
        side.add(iinsp);

        side.add(_descrip = new BLabel(""));
        side.add(_cost = new BLabel(""));
        side.add(_buy = new BButton(_msgs.get("m.buy"), "buy"));
        _buy.addListener(this);

        BButton btn;
        side.add(btn = new BButton(_msgs.get("m.back_to_town"), "back"));
        btn.addListener(this);

        add(_status = new BLabel(""), BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("back".equals(event.getAction())) {
            _ctx.clearPlaceView(this);

        } else if ("buy".equals(event.getAction())) {
            log.info("I'll take it!");
        }
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        // this is never actually called; the store is not a real place
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // this is never actually called; the store is not a real place
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BLabel _descrip;
    protected BLabel _cost;
    protected BButton _buy;
    protected BLabel _status;
}
