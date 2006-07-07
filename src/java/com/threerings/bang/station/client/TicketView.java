//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
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
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _status = status;

        // determine the town to which they'll be buying a ticket; the first
        // one to which they don't currently hold a ticket
        int ticketTownIdx = -1;
        for (int ii = 1; ii < BangCodes.TOWN_IDS.length; ii++) {
            String townId = BangCodes.TOWN_IDS[ii];
            if (!ctx.getUserObject().holdsTicket(townId)) {
                ticketTownIdx = ii;
                _ticketTownId = townId;
                break;
            }
        }

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            StationCodes.STATION_MSGS);
        String title, body;
        if (_ticketTownId == null) {
            title = "t.have_all_tickets";
            body = "m.have_all_tickets";
        } else {
            title = getTownMessage("t.buy_ticket");
            body = "m.buy_ticket_" + _ticketTownId;
        }

        add(new BLabel(msgs.xlate(title), "ticket_header"),
            GroupLayout.FIXED);
        add(new BLabel(msgs.get(body), "ticket_info"));

        if (_ticketTownId != null) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            MoneyLabel cost = new MoneyLabel(ctx);
            cost.setMoney(StationCodes.TICKET_SCRIP[ticketTownIdx],
                          StationCodes.TICKET_COINS[ticketTownIdx], false);
            row.add(cost);
            add(row, GroupLayout.FIXED);

            row = GroupLayout.makeHBox(GroupLayout.CENTER);
            row.add(_buy = new BButton(msgs.get("b.buy_ticket"), this, ""));
            _buy.setStyleClass("big_button");
            _buy.setEnabled(_ticketTownId != null);
            add(row, GroupLayout.FIXED);
        }
    }

    public void init (StationObject stobj)
    {
        _stobj = stobj;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // avoid double clicky badness
        _buy.setEnabled(false);

        // fire off a request to buy the ticket
        _stobj.service.buyTicket(
            _ctx.getClient(), new StationService.ConfirmListener() {
            public void requestProcessed () {
                _status.setStatus(StationCodes.STATION_MSGS,
                                  getTownMessage("m.bought_ticket"), true);
            }
            public void requestFailed (String reason) {
                _status.setStatus(StationCodes.STATION_MSGS, reason, true);
                _buy.setEnabled(true);
            }
        });
    }

    protected String getTownMessage (String message)
    {
        return MessageBundle.compose(
            message, MessageBundle.qualify(
                BangCodes.BANG_MSGS, "m." + _ticketTownId));
    }

    protected BangContext _ctx;
    protected StationObject _stobj;
    protected String _ticketTownId;
    protected BButton _buy;
    protected StatusLabel _status;
}
