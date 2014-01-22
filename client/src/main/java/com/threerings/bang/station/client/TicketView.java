//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.FreeTicket;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationObject;

/**
 * Displays an interface for buying a train ticket.
 */
public class TicketView extends BContainer
    implements ActionListener
{
    public TicketView (BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _status = status;

        // determine the town to which they'll be buying a ticket; the first one to which they
        // don't currently hold a ticket
        int ticketTownIdx = -1;
        for (int ii = 1; ii < BangCodes.TOWN_IDS.length; ii++) {
            String townId = BangCodes.TOWN_IDS[ii];
            if (!ctx.getUserObject().holdsTicket(townId)) {
                ticketTownIdx = ii;
                _ticketTownId = townId;
                _freeTicket = (FreeTicket)ctx.getUserObject().getEquivalentItem(
                        new FreeTicket(-1, ii));
                break;
            }
        }

        String tpath = "coming_soon";
        String body = "m.have_all_tickets";
        boolean freeUI = _freeTicket != null && !_ticketTownId.equals(ctx.getUserObject().townId);

        if (freeUI) {
            tpath = "free_day_pass";
            body = "m.free_ticket_" + _freeTicket.getTownId();
        } else if (_ticketTownId != null) {
            tpath = "buy_ticket";
            body = "m.buy_ticket_" + _ticketTownId;
        }

        tpath = "ui/station/" + tpath + ".png";
        add(new BLabel(new ImageIcon(ctx.loadImage(tpath))), new Point(0, 403));

        MessageBundle msgs = ctx.getMessageManager().getBundle(StationCodes.STATION_MSGS);
        add(new BLabel(msgs.get(body), "ticket_info"), new Rectangle(0, 230, 160, 173));

        boolean townEnabled = false;
        if (freeUI) {
            String msg = "l.its_free";
            if (_freeTicket.isActivated()) {
                msg = MessageBundle.compose("l.expires", MessageBundle.taint(
                            _freeTicket.getExpireString()));
            }
            add(new BLabel(msgs.xlate(msg), "free_label"), new Rectangle(0, 80, 160, 23));
            String ipath = "goods/tickets/free_" + _ticketTownId + ".png";
            add(new BLabel(new ImageIcon(ctx.loadImage(ipath))), new Point(16, 112));
            townEnabled = true;

        } else {
            if (ticketTownIdx > 0) {
                townEnabled = (StationCodes.TICKET_COINS[ticketTownIdx] > 0) ||
                    ctx.getUserObject().tokens.isAdmin(); // admins get to go anywhere, yay!
            }
            if (_ticketTownId != null) {
                if (townEnabled) {
                    BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
                    row.add(new BLabel(msgs.get("l.price"), "price_label"));
                    MoneyLabel cost = new MoneyLabel(ctx, true);
                    int coinCost = new TrainTicket(-1, ticketTownIdx).
                        getCoinCost(ctx.getUserObject());
                    cost.setMoney(StationCodes.TICKET_SCRIP[ticketTownIdx], coinCost, false);
                    row.add(cost);
                    add(row, new Rectangle(0, 80, 160, 23));
                }

                String ipath = "goods/tickets/" + _ticketTownId + ".png";
                add(new BLabel(new ImageIcon(ctx.loadImage(ipath))), new Point(16, 112));
            }
        }

        BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_buy = new BButton(msgs.get(freeUI ? "b.details" : "b.buy_ticket"), this,
                    (freeUI ? "details" : "buy")));
        _buy.setStyleClass("big_button");
        _buy.setEnabled(_ticketTownId != null && townEnabled);
        add(row, new Rectangle(0, 21, 160, 46));
    }

    public void init (StationObject stobj)
    {
        _stobj = stobj;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("buy".equals(event.getAction())) {
            // avoid double clicky badness
            _buy.setEnabled(false);

            // fire off a request to buy the ticket
            _stobj.service.buyTicket(new StationService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(
                        StationCodes.STATION_MSGS, getTownMessage("m.bought_ticket"), true);
                }
                public void requestFailed (String reason) {
                    _status.setStatus(StationCodes.STATION_MSGS, reason, true);
                    _buy.setEnabled(true);
                }
            });

        } else if ("details".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(new PassDetailsView(_ctx, _freeTicket, true), true);
        }
    }

    protected String getTownMessage (String message)
    {
        return MessageBundle.compose(
            message, MessageBundle.qualify(BangCodes.BANG_MSGS, "m." + _ticketTownId));
    }

    protected BangContext _ctx;
    protected StationObject _stobj;
    protected String _ticketTownId;
    protected BButton _buy;
    protected StatusLabel _status;
    protected FreeTicket _freeTicket;
}
