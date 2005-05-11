//
// $Id$

package com.threerings.bang.lobby.client;

import com.jme.bui.BWindow;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.renderer.ColorRGBA;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableLobbyObject;

import com.threerings.jme.chat.ChatView;

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
        super(ctx.getLookAndFeel(), new BorderLayout());
        _ctx = ctx;

        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        add(_chat, BorderLayout.SOUTH);

        MessageBundle msgs =
            ctx.getMessageManager().getBundle("lobby");

        // create our table director
        _tbldtr = new TableDirector(ctx, LobbyObject.TABLE_SET, this);

        // add ourselves as a seatedness observer
        _tbldtr.addSeatednessObserver(this);

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

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        setBounds(0, 0, _ctx.getDisplay().getWidth(),
                  _ctx.getDisplay().getWidth());
        _ctx.getInputDispatcher().addWindow(this);
        _ctx.getInterface().attachChild(getNode());

        // switch to a gray background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);

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
        _tbldtr.didLeavePlace(plobj);
        _chat.didLeavePlace(plobj);

        _ctx.getInputDispatcher().removeWindow(this);
        _ctx.getGeometry().detachChild(getNode());

        // restore the black background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);

//         // clear out our table lists
//         _matchList.removeAll();
//         _playList.removeAll();
    }

    // documentation inherited from interface TableObserver
    public void tableAdded (Table table)
    {
        log.info("Table added [table=" + table + "].");

//         // create a table item for this table and insert it into the
//         // appropriate list
//         JPanel panel = table.inPlay() ? _playList : _matchList;
//         panel.add(new TableItem(_ctx, _tbldtr, table));
//         SwingUtil.refresh(panel);
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

//         // and we may need to move the item from the match to the in-play
//         // list if it just transitioned
//         if (table.gameOid != -1 && item.getParent() == _matchList) {
//             _matchList.remove(item);
//             SwingUtil.refresh(_matchList);
//             _playList.add(item);
//             SwingUtil.refresh(_playList);
//         }
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

//         // remove this item from the user interface
//         JPanel panel = (JPanel)item.getParent();
//         panel.remove(item);
//         SwingUtil.refresh(panel);

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

//         _tbldtr.createTable(config);
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
//         // first check the match list
//         int ccount = _matchList.getComponentCount();
//         for (int i = 0; i < ccount; i++) {
//             TableItem child = (TableItem)_matchList.getComponent(i);
//             if (child.table.getTableId() == tableId) {
//                 return child;
//             }
//         }

//         // then the inplay list
//         ccount = _playList.getComponentCount();
//         for (int i = 0; i < ccount; i++) {
//             TableItem child = (TableItem)_playList.getComponent(i);
//             if (child.table.getTableId() == tableId) {
//                 return child;
//             }
//         }

        // sorry charlie
        return null;
    }

    protected BangContext _ctx;
    protected ChatView _chat;
    protected TableDirector _tbldtr;
}
