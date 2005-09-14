//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;
import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;
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

        BangUserObject user = ctx.getUserObject();
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(user.username.toString()));
        BLabel town = new BLabel(_msgs.get("m." + user.townId));
        town.setHorizontalAlignment(BLabel.RIGHT);
        row.add(town);
        add(row, GroupLayout.FIXED);

        row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m.status_scrip", "" + user.scrip)));
        row.add(new BLabel(_msgs.get("m.status_gold", "" + user.gold)));
        add(row, GroupLayout.FIXED);

        BTabbedPane tabs = new BTabbedPane();
        add(tabs);

        // add the inventory tab
        tabs.addTab(_msgs.get("m.status_inventory"), new BContainer());

        // add the big shots tab
        UnitPalette bigshots = new UnitPalette(ctx, null);
        bigshots.setUser(user);
        tabs.addTab(_msgs.get("m.status_big_shots"), bigshots);

        // add the badges tab
        tabs.addTab(_msgs.get("m.status_badges"), new BContainer());

        // add the stats tab
        tabs.addTab(_msgs.get("m.status_stats"), createStatsTab(user));

        row = new BContainer(GroupLayout.makeHStretch());
        row.add(createButton("quit"), GroupLayout.FIXED);
        row.add(new BContainer()); // spacer
        if (user.location != -1) {
            row.add(createButton("to_town"), GroupLayout.FIXED);
            row.add(new BContainer()); // spacer
        }
        row.add(createButton("resume"), GroupLayout.FIXED);
        add(row, GroupLayout.FIXED);
    }

    protected BContainer createStatsTab (BangUserObject user)
    {
        BContainer scont = new BContainer(new TableLayout(2, 5, 5));
        scont.setBorder(new EmptyBorder(5, 5, 5, 5));
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
}
