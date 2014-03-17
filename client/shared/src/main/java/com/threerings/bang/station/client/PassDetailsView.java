//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BComponent;
import com.jmex.bui.BButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.background.ImageBackground;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.FreeTicket;

/**
 * Display information about the day pass.
 */
public class PassDetailsView extends SteelWindow
    implements ActionListener
{
    public PassDetailsView (BangContext ctx, FreeTicket ticket, boolean station)
    {
        super(ctx, ctx.xlate(StationCodes.STATION_MSGS, MessageBundle.compose(
                        "m.visit", "m." + ticket.getTownId())));
        _ctx = ctx;
        _ticket = ticket;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(StationCodes.STATION_MSGS);
        setModal(true);
        _contents.setLayoutManager(new AbsoluteLayout());
        _contents.setPreferredSize(new Dimension(771, 422));
        _station = station;

        ItemIcon ticketIcon = new ItemIcon(ctx, ticket);
        ticketIcon.setEnabled(false);
        _contents.add(ticketIcon, new Point(140, 90));

        if (station) {
            _buttons.add(new BButton(msgs.get("m.dismiss"), this, "dismiss"));
        } else {
            BButton btn = new BButton(msgs.get("m.use_now"), this, "use_now");
            btn.setStyleClass("big_button");
            _contents.add(btn, new Rectangle(140, 40, 140, 35));
            _buttons.add(new BButton(msgs.get("m.remind_later"), this, "later"));
            _buttons.add(new BButton(msgs.get("m.never_remind"), this, "never"));
        }
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        _ctx.getBangClient().clearPopup(this, true);

        if (action.equals("use_now")) {
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            _ctx.getLocationDirector().moveTo(bbd.stationOid);
            return;
        }

        _ctx.getChatDirector().displayFeedback(StationCodes.STATION_MSGS, "m.to_station");
        if (action.equals("never")) {
            BangPrefs.setNoRemind(_ctx.getUserObject(), _ticket.getTownId());
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _contents.setBackground(BComponent.DEFAULT, new ImageBackground(ImageBackground.CENTER_XY,
                    _ctx.loadImage("ui/station/pass_details_" + _ticket.getTownId() + ".jpg")));
    }

    protected BangContext _ctx;
    protected FreeTicket _ticket;
    protected boolean _station;
}
