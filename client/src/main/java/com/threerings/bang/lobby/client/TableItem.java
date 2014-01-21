//
// $Id$

package com.threerings.bang.lobby.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.Table;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays a single table.
 */
public class TableItem extends BContainer
    implements ActionListener, SeatednessObserver
{
    /** A reference to the table we are displaying. */
    public Table table;

    /**
     * Creates a new table item to display and interact with the supplied
     * table.
     */
    public TableItem (BangContext ctx, TableDirector tdtr, Table table)
    {
        super(new TableLayout(3, 5, 5));
        ((TableLayout)getLayoutManager()).setHorizontalAlignment(
            TableLayout.STRETCH);
        setStyleClass("padded_box");

        _ctx = ctx;
        _self = ctx.getUserObject().getVisibleName();
        _tconfig = (BangConfig)table.config;
        _tdtr = tdtr;
        _tdtr.addSeatednessObserver(this);

        _msgs = ctx.getMessageManager().getBundle("lobby");

        // the top row just has the table label
        add(new BLabel(""));
        add(new BLabel(_msgs.get("m.table", String.valueOf(table.tableId))));
        add(new BLabel(""));

        // we have one button for every "seat" at the table
        int bcount = _tconfig.plist.size();

        // create blank buttons for now and then we'll update everything
        // with the current state when we're done
        _seats = new BButton[bcount];
        for (int ii = 0; ii < bcount; ii++) {
            // create the button
            _seats[ii] = new BButton("");
            _seats[ii].addListener(this);
        }

        // depending on how many seats we have we lay the buttons out
        // specially
        for (int ii = 0; ii < bcount/2; ii++) {
            add(_seats[2*ii]);
            add(new BLabel(""));
            add(_seats[2*ii+1]);
        }
        if (bcount%2 == 1) {
            add(new BLabel(""));
            add(_seats[bcount-1]);
            add(new BLabel(""));
        }

//         // if we just added the first button, add the "go" button
//         // right after it
//         if (i == 0) {
//             String msg = _tconfig.isPartyGame() ? "m.join" : "m.watch";
//             _goButton = new JButton(_ctx.xlate(LobbyCodes.LOBBY_MSGS, msg));
//             _goButton.setActionCommand("go");
//             _goButton.addActionListener(this);
//             add(_goButton, gbc);
//         }

        // and update ourselves based on the contents of the players
        // list
        tableUpdated(table);
    }

    /**
     * Called when our table has been updated and we need to update the UI
     * to reflect the new information.
     */
    public void tableUpdated (Table table)
    {
        // grab this new table reference
        this.table = table;

        // first look to see if we're already sitting at a table
        boolean isSeated = _tdtr.isSeated();

        // now enable and label the buttons accordingly
        int slength = _seats.length;
        for (int i = 0; i < slength; i++) {
            if (table.players[i] == null) {
                _seats[i].setText(_msgs.get("m.join"));
                _seats[i].setEnabled(!isSeated);
                _seats[i].setAction("join");

            } else if (table.players[i].equals(_self) && !table.inPlay()) {
                _seats[i].setText(_msgs.get("m.leave"));
                _seats[i].setEnabled(true);
                _seats[i].setAction("leave");

            } else {
                _seats[i].setText(table.players[i].toString());
                _seats[i].setEnabled(false);
            }
        }

        // show or hide our "go" button appropriately
//         _goButton.setVisible(table.gameOid != -1);
    }

    /**
     * Called by the table list view prior to removing us. Here we clean
     * up.
     */
    public void tableRemoved ()
    {
        // no more observy
        _tdtr.removeSeatednessObserver(this);
    }

    // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("join")) {
            // figure out what position this button is in
            int position = -1;
            for (int i = 0; i < _seats.length; i++) {
                if (_seats[i] == event.getSource()) {
                    position = i;
                    break;
                }
            }

            // sanity check
            if (position == -1) {
                log.warning("Unable to figure out what position a <join> click came from",
                            "event", event);
            } else {
                // otherwise, request to join the table at this position
                _tdtr.joinTable(table.tableId, position);
            }

        } else if (cmd.equals("leave")) {
            // if we're not joining, we're leaving
            _tdtr.leaveTable(table.tableId);

        } else if (cmd.equals("go")) {
            // they want to see the game... so go there
            _ctx.getLocationDirector().moveTo(table.gameOid);

        } else {
            log.warning("Received unknown action", "event", event);
        }
    }

    // documentation inherited
    public void seatednessDidChange (boolean isSeated)
    {
        // just update ourselves
        tableUpdated(table);

//         // enable or disable the go button based on our seatedness
//         if (_goButton.isVisible()) {
//             _goButton.setEnabled(!isSeated);
//         }
    }

    protected BangContext _ctx;
    protected Name _self;
    protected TableDirector _tdtr;
    protected BangConfig _tconfig;

    protected MessageBundle _msgs;
    protected BButton[] _seats;
    protected BButton _goButton;
}
