//
// $Id$

package com.threerings.bang.store.client;

import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.util.Dimension;
import com.jme.renderer.ColorRGBA;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.StatusView;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.StoreObject;

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

        // the display of items for sale
        _goods = new BContainer(new TableLayout(4, 5, 5));
        add(_goods, BorderLayout.CENTER);

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
            _ctx.getLocationDirector().leavePlace();

        } else if ("buy".equals(event.getAction())) {
            log.info("I'll take it!");
        }
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        // populate our salable goods
        StoreObject stobj = (StoreObject)plobj;

        // TODO: sort the goods by type
        for (Iterator iter = stobj.goods.iterator(); iter.hasNext(); ) {
            Good good = (Good)iter.next();
            _goods.add(new BLabel(_msgs.xlate(good.getName())));
        }
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BContainer _goods;
    protected BLabel _descrip;
    protected BLabel _cost;
    protected BButton _buy;
    protected BLabel _status;
}
