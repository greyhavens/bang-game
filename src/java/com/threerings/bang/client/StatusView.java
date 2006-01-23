//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.PickLookView;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;

/**
 * Displays a summary of a player's status, including: cash on hand,
 * inventory items, big shots.
 */
public class StatusView extends BDecoratedWindow
    implements ActionListener
{
    public StatusView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _modal = true;
        setLayoutManager(GroupLayout.makeVStretch());
        addListener(new EscapeListener() {
            public void escapePressed() {
                _ctx.getBangClient().clearPopup();
            }
        });

        PlayerObject user = ctx.getUserObject();
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.setStyleClass("dialog_title");
        row.add(new BLabel(user.handle.toString(), "left_label"));
        row.add(new BLabel(_msgs.get("m." + user.townId), "right_label"));
        add(row, GroupLayout.FIXED);

        add(new WalletLabel(ctx, false), GroupLayout.FIXED);

        // TODO: record in a static variable which tab was last selected and
        // use it when opening the view
        _tabs = new BTabbedPane();
        add(_tabs);

        // add the avatar tab
        _tabs.addTab(_msgs.get("m.status_avatar"), new PickLookView(ctx));

        // add the inventory tab
        _tabs.addTab(_msgs.get("m.status_inventory"),
                     new BScrollPane(new InventoryPalette(ctx)));

        // add the big shots tab
        UnitPalette bigshots = new UnitPalette(ctx, null, 4);
        bigshots.setUser(user);
        _tabs.addTab(_msgs.get("m.status_big_shots"),
                     new BScrollPane(bigshots));

        // add the badges tab
        _tabs.addTab(_msgs.get("m.status_badges"),
                     new BScrollPane(new BadgePalette(ctx)));

        // add the stats tab
        PlayerStatsView pstats = new PlayerStatsView(ctx);
        _tabs.addTab(_msgs.get("m.status_stats"),
                     new BScrollPane(pstats));

        row = new BContainer(GroupLayout.makeHStretch());
        row.add(createButton("quit"), GroupLayout.FIXED);
        row.add(new BContainer()); // spacer
        if (user.location != -1) {
            row.add(createButton("to_town"), GroupLayout.FIXED);
            row.add(new BContainer()); // spacer
        }
        row.add(createButton("resume"), GroupLayout.FIXED);
        add(row, GroupLayout.FIXED);

        // finally select the most recently selected tab
        _tabs.selectTab(_selectedTab);
    }

    @Override // documentation inherited
    public void dismiss ()
    {
        super.dismiss();
        _selectedTab = _tabs.getSelectedTabIndex();
    }

    /**
     * Binds this menu to a component. The component should either be the
     * default event target, or a modal window.
     */
    public void bind (BComponent host)
    {
        host.addListener(new EscapeListener() {
            public void escapePressed () {
                _ctx.getBangClient().displayPopup(StatusView.this);
                setSize(610, 500);
                center();
            }
        });
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("quit")) {
            _ctx.getApp().stop();

        } else if (cmd.equals("to_town")) {
            if (_ctx.getLocationDirector().leavePlace()) {
                _ctx.getBangClient().clearPopup();
                _ctx.getBangClient().showTownView();
            }

        } else if (cmd.equals("resume")) {
            _ctx.getBangClient().clearPopup();
        }
    }

    protected BButton createButton (String action)
    {
        BButton button = new BButton(_msgs.get("m.status_" + action), action);
        button.addListener(this);
        return button;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTabbedPane _tabs;

    /** We track which tab was last selected across instances. */
    protected static int _selectedTab;
}
