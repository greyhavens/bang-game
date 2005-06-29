//
// $Id$

package com.threerings.bang.lobby.client;

import com.jme.bui.BButton;
import com.jme.bui.BComboBox;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BWindow;
import com.jme.bui.util.Dimension;
import com.jme.bui.border.CompoundBorder;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.border.LineBorder;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;
import com.jme.renderer.ColorRGBA;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.data.TableLobbyObject;

import com.threerings.jme.chat.ChatView;

import com.threerings.bang.client.EscapeMenuView;
import com.threerings.bang.client.TownView;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.lobby.data.LobbyObject;
import com.threerings.bang.util.BangContext;

import java.util.Iterator;

import static com.threerings.bang.Log.log;

/**
 * Displays the interface for a Bang! lobby.
 */
public class LobbyView extends BWindow
    implements PlaceView, TableObserver, SeatednessObserver, ActionListener
{
    public LobbyView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        _ctx = ctx;

        // display a simple menu when the player presses escape
        setModal(true);
        EscapeMenuView oview = new EscapeMenuView(_ctx) {
            public void actionPerformed (ActionEvent event) {
                String action = event.getAction();
                if ("back_to_town".equals(action)) {
                    if (_ctx.getLocationDirector().leavePlace()) {
                        dismiss();
                    }
                } else {
                    super.actionPerformed(event);
                }
            }
            protected void addButtons () {
                add(createButton("m.resume", "dismiss"));
                add(createButton("m.back_to_town", "back_to_town"));
                super.addButtons();
            }
        };
        oview.bind(this);

        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        _chat.setBorder(new EmptyBorder(5, 0, 5, 5));
        _chat.setPreferredSize(new Dimension(100, 150));
        add(_chat, BorderLayout.SOUTH);

        MessageBundle msgs =
            ctx.getMessageManager().getBundle("lobby");

        // create our table director
        _tbldtr = new TableDirector(ctx, LobbyObject.TABLE_SET, this);

        // add ourselves as a seatedness observer
        _tbldtr.addSeatednessObserver(this);

        BContainer top = new BContainer(GroupLayout.makeHStretch());
        top.setBorder(new EmptyBorder(5, 5, 5, 0));

        BContainer plist = createLabeledList(msgs.get("m.pending_games"));
        _penders = (BContainer)plist.getComponent(1);
        _penders.setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                              new EmptyBorder(5, 5, 5, 5)));
        BContainer blist = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        _seats = new BComboBox(SEATS);
        _seats.selectItem(SEATS[0]);
        blist.add(_seats);
        BButton create = new BButton(msgs.get("m.create"), "create");
        create.addListener(this);
        blist.add(create);
        plist.add(blist, BorderLayout.SOUTH);
        top.add(plist);

        BContainer ilist = createLabeledList(msgs.get("m.in_progress"));
        _inplay = (BContainer)ilist.getComponent(1);
        _inplay.setBorder(new LineBorder(ColorRGBA.black));
        top.add(ilist);
        add(top, BorderLayout.CENTER);

//         // set up a layout manager
// 	HGroupLayout gl = new HGroupLayout(HGroupLayout.STRETCH);
// 	gl.setOffAxisPolicy(HGroupLayout.STRETCH);
// 	setLayout(gl);

//         // we have two lists of tables, one of tables being matchmade...
//         VGroupLayout pgl = new VGroupLayout(VGroupLayout.STRETCH);
//         pgl.setOffAxisPolicy(VGroupLayout.STRETCH);
//         pgl.setJustification(VGroupLayout.TOP);
//         JPanel panel = new JPanel(pgl);
//         String cmsg = config.isPartyGame() ?
//             "m.create_game" : "m.pending_tables";
//         panel.add(new JLabel(msgs.get(cmsg)), VGroupLayout.FIXED);

//         VGroupLayout mgl = new VGroupLayout(VGroupLayout.NONE);
//         mgl.setOffAxisPolicy(VGroupLayout.STRETCH);
//         mgl.setJustification(VGroupLayout.TOP);
//         _matchList = new JPanel(mgl);
//         if (!config.isPartyGame()) {
//             _matchList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//             panel.add(new SafeScrollPane(_matchList));
//         }

//         // create and initialize our configurator interface
//         _figger = _config.createConfigurator();
//         if (_figger != null) {
//             _figger.init(_ctx);
//             _figger.setGameConfig(config);
//             panel.add(_figger, VGroupLayout.FIXED);
//         }

//         // add the interface for selecting the number of seats at the table
//         panel.add(_pslide = new SimpleSlider(msgs.get("m.seats"), 0, 10, 0),
//                   VGroupLayout.FIXED);

//         // configure our slider
//         _pslide.setMinimum(config.getMinimumPlayers());
//         _pslide.setMaximum(config.getMaximumPlayers());
//         _pslide.setValue(config.getDesiredPlayers());

//         int range = config.getMaximumPlayers() - config.getMinimumPlayers();
//         _pslide.getSlider().setPaintTicks(true);
//         _pslide.getSlider().setMinorTickSpacing(1);
//         _pslide.getSlider().setMajorTickSpacing(range / 2);
//         _pslide.getSlider().setSnapToTicks(true);

//         // if the min == the max, hide the slider because it's pointless
//         _pslide.setVisible(config.getMinimumPlayers() !=
//                            config.getMaximumPlayers());

//         cmsg = config.isPartyGame() ? "m.create_game" : "m.create_table";
//         _create = new JButton(msgs.get(cmsg));
//         _create.addActionListener(this);
//         JPanel bbox = HGroupLayout.makeButtonBox(HGroupLayout.RIGHT);
//         bbox.add(_create);
//         panel.add(bbox, VGroupLayout.FIXED);

//         if (config.isPartyGame()) {
//             panel.add(new JLabel(msgs.get("m.party_hint")), VGroupLayout.FIXED);
//         }

//         add(panel);

//         // ...and one of games in progress
//         panel = new JPanel(pgl);
//         panel.add(new JLabel(msgs.get("m.in_progress")), VGroupLayout.FIXED);

//         _playList = new JPanel(mgl);
//     	_playList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//         panel.add(new SafeScrollPane(_playList));

//         add(panel);
    }

    protected BContainer createLabeledList (String label)
    {
        BContainer outer = new BContainer(new BorderLayout(5, 5));
        outer.add(new BLabel(label), BorderLayout.NORTH);
        GroupLayout vlay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        outer.add(new BContainer(vlay), BorderLayout.CENTER);
        return outer;
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        setBounds(0, 0, _ctx.getDisplay().getWidth(),
                  _ctx.getDisplay().getHeight());

        // switch to a gray background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // restore the black background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        // pass will enter place onto interested parties
        _chat.willEnterPlace(plobj);
        _tbldtr.willEnterPlace(plobj);

        // iterate over the tables already active in this lobby and put
        // them in their respective lists
        TableLobbyObject tlobj = (TableLobbyObject)plobj;
        for (Iterator iter = tlobj.getTables().iterator(); iter.hasNext(); ) {
            tableAdded((Table)iter.next());
        }
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // clear out our table lists
        _penders.removeAll();
        _inplay.removeAll();

        _tbldtr.didLeavePlace(plobj);
        _chat.didLeavePlace(plobj);
    }

    // documentation inherited from interface TableObserver
    public void tableAdded (Table table)
    {
        log.info("Table added [table=" + table + "].");

        // create a table item for this table and insert it into the
        // appropriate list
        BContainer host = table.inPlay() ? _inplay : _penders;
        host.add(new TableItem(_ctx, _tbldtr, table));
    }

    // documentation inherited from interface TableObserver
    public void tableUpdated (Table table)
    {
        log.info("Table updated [table=" + table + "].");

        // locate the table item associated with this table
        TableItem item = getTableItem(table.getTableId());
        if (item == null) {
            log.warning("Received table updated notification for " +
                        "unknown table [table=" + table + "].");
            return;
        }

        // let the item perform any updates it finds necessary
        item.tableUpdated(table);

        // and we may need to move the item from the match to the in-play
        // list if it just transitioned
        if (table.gameOid != -1 && item.getParent() == _penders) {
            _penders.remove(item);
            _inplay.add(item);
        }
    }

    // documentation inherited from interface TableObserver
    public void tableRemoved (int tableId)
    {
        log.info("Table removed [tableId=" + tableId + "].");

        // locate the table item associated with this table
        TableItem item = getTableItem(tableId);
        if (item == null) {
            log.warning("Received table removed notification for " +
                        "unknown table [tableId=" + tableId + "].");
            return;
        }

        // remove this item from the user interface
        BContainer parent = (BContainer)item.getParent();
        parent.remove(item);

        // let the little fellow know that we gave him the boot
        item.tableRemoved();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
//         // the create table button was clicked. use the game config as
//         // configured by the configurator to create a table
//         ToyBoxGameConfig config = _config;
//         if (_figger != null) {
//             config = (ToyBoxGameConfig)_figger.getGameConfig();
//         }

//         // fill in our number of seats configuration
//         config.setDesiredPlayers(_pslide.getValue());

        TableConfig tconfig = new TableConfig();
        tconfig.desiredPlayerCount = (Integer)_seats.getSelectedItem();
        BangConfig config = new BangConfig();
        config.seats = tconfig.desiredPlayerCount;
        _tbldtr.createTable(tconfig, config);
    }

    // documentation inherited from interface SeatednessObserver
    public void seatednessDidChange (boolean isSeated)
    {
//         // update the create table button
//         _create.setEnabled(!isSeated);
    }

    /**
     * Fetches the table item component associated with the specified
     * table id.
     */
    protected TableItem getTableItem (int tableId)
    {
        // first check the pending tables list
        int ccount = _penders.getComponentCount();
        for (int i = 0; i < ccount; i++) {
            TableItem child = (TableItem)_penders.getComponent(i);
            if (child.table.getTableId() == tableId) {
                return child;
            }
        }

        // then the inplay list
        ccount = _inplay.getComponentCount();
        for (int i = 0; i < ccount; i++) {
            TableItem child = (TableItem)_inplay.getComponent(i);
            if (child.table.getTableId() == tableId) {
                return child;
            }
        }

        // sorry charlie
        return null;
    }

    protected BangContext _ctx;
    protected ChatView _chat;
    protected TableDirector _tbldtr;

    protected BComboBox _seats;
    protected BContainer _penders;
    protected BContainer _inplay;

    protected static final Integer[] SEATS = new Integer[] {
        new Integer(2), new Integer(3), new Integer(4) };
}
