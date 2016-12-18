//
// $Id$

package com.threerings.bang.game.client;

import java.util.HashSet;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.stats.data.Stat;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays post-round and post-game statistics on all players.
 */
public class StatsDisplay extends BDecoratedWindow
{
    public StatsDisplay (
        BangContext ctx, BangController ctrl, BangObject bangobj,
        int pidx, String title)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                              GroupLayout.STRETCH));

        _ctx = ctx;
        _ctrl = ctrl;

        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        add(new BLabel(title, "dialog_title"));
        add(new Spacer(10, 20));

        BContainer bits = new BContainer(new TableLayout(bangobj.players.length + 1, 5, 25));
        bits.add(new BLabel(""));
        for (int pp = 0; pp < bangobj.players.length; pp++) {
            bits.add(new BLabel(bangobj.players[pp].toString(), "stats_header"));
        }

        // enumerate all the stat types accumulated during the game
        HashSet<StatType> types = new HashSet<StatType>();
        for (int ii = 0; ii < bangobj.stats.length; ii++) {
            for (Iterator<Stat> iter = bangobj.stats[ii].iterator(); iter.hasNext(); ) {
                types.add((StatType)iter.next().getType());
            }
        }

        for (StatType type : types) {
            bits.add(new BLabel(_ctx.xlate(BangCodes.STATS_MSGS, type.key()) + ":"));
            for (int pp = 0; pp < bangobj.stats.length; pp++) {
                Stat pstat = bangobj.stats[pp].get(type.name());
                bits.add(new BLabel(pstat == null ? "" : pstat.valueToString(), "right_label"));
            }
        }
        add(bits);

        // display awarded cash and badges if this is the end of the game
        if (bangobj.awards != null && pidx >= 0) {
            add(new Spacer(10, 20));
            BContainer awards = new BContainer(
                GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));

            BContainer hbox = GroupLayout.makeHBox(GroupLayout.LEFT);
            hbox.add(new BLabel(msgs.get("m.stats_cash"), "stats_info"));
            BLabel cash = new BLabel("" + bangobj.awards[pidx].cashEarned);
            cash.setIcon(BangUI.scripIcon);
            cash.setIconTextGap(5);
            cash.setStyleClass("stats_cash");
            hbox.add(cash);
            awards.add(hbox);

            if (bangobj.awards[pidx].item instanceof Badge) {
                awards.add(new BLabel(msgs.get("m.stats_badges"), "stats_info"));
                hbox = GroupLayout.makeHBox(GroupLayout.LEFT);
                hbox.add(new ItemIcon(_ctx, bangobj.awards[pidx].item));
                awards.add(hbox);
            }

            add(awards);
        }

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        BButton dismiss = new BButton(msgs.get("m.dismiss"));
        dismiss.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getRootNode().removeWindow(StatsDisplay.this);
                _ctrl.statsDismissed();
            }
        });
        buttons.add(dismiss);
        add(buttons);
    }

    protected BangContext _ctx;
    protected BangController _ctrl;
}
