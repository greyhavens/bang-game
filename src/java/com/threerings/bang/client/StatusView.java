//
// $Id$

package com.threerings.bang.client;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
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
        super(ctx.getLookAndFeel(), null);
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _modal = true;
        setLayoutManager(GroupLayout.makeVStretch());
        addListener(new EscapeListener() {
            public void escapePressed() {
                dismiss();
            }
        });

        PlayerObject user = ctx.getUserObject();
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.setLookAndFeel(BangUI.dtitleLNF);
        row.add(new BLabel(user.handle.toString()));
        BLabel town = new BLabel(_msgs.get("m." + user.townId));
        town.setHorizontalAlignment(BLabel.RIGHT);
        row.add(town);
        add(row, GroupLayout.FIXED);

        add(new WalletLabel(ctx), GroupLayout.FIXED);

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
        UnitPalette bigshots = new UnitPalette(ctx, null, 3);
        bigshots.setUser(user);
        _tabs.addTab(_msgs.get("m.status_big_shots"),
                     new BScrollPane(bigshots));

        // add the badges tab
        _tabs.addTab(_msgs.get("m.status_badges"),
                     new BScrollPane(createBadgeTab(user)));

        // add the stats tab
        _tabs.addTab(_msgs.get("m.status_stats"),
                     new BScrollPane(createStatsTab(user)));

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

    protected BContainer createBadgeTab (PlayerObject user)
    {
        BContainer bcont = new BContainer(new TableLayout(2, 5, 5));
        bcont.setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                           new EmptyBorder(5, 5, 5, 5)));
        bcont.add(new BLabel("Not yet implemented"));
        return bcont;
    }

    protected BContainer createStatsTab (PlayerObject user)
    {
        BContainer scont = new BContainer(new TableLayout(2, 5, 5));
        scont.setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                           new EmptyBorder(5, 5, 5, 5)));
        Stat[] stats = (Stat[])user.stats.toArray(new Stat[user.stats.size()]);
        // TODO: sort on translated key
        for (int ii = 0; ii < stats.length; ii++) {
            String key = stats[ii].getType().key();
            scont.add(new BLabel(_ctx.xlate(BangCodes.STATS_MSGS, key)));
            scont.add(new BLabel(stats[ii].valueToString()));
        }
        return scont;
    }

    /**
     * Binds this menu to a component. The component should either be the
     * default event target, or a modal window.
     */
    public void bind (BComponent host)
    {
        host.addListener(new EscapeListener() {
            public void escapePressed () {
                _ctx.getRootNode().addWindow(StatusView.this);
                setSize(500, 500);
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
                dismiss();
                _ctx.getBangClient().showTownView();
            }

        } else if (cmd.equals("resume")) {
            dismiss();
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
