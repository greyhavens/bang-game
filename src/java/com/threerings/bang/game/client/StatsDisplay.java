//
// $Id$

package com.threerings.bang.game.client;

import java.util.HashSet;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BadgeIcon;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;
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

        _ctx = ctx;
        _ctrl = ctrl;

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            GameCodes.GAME_MSGS);

        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                              GroupLayout.STRETCH));

        BLabel label = new BLabel(title);
        label.setStyleClass("dialog_title");
        add(label);

        BContainer bits = new BContainer(
            new TableLayout(bangobj.players.length + 1, 5, 5));
        bits.add(new BLabel(""));
        for (int pp = 0; pp < bangobj.players.length; pp++) {
            // TODO: make bold
            bits.add(new BLabel(bangobj.players[pp].toString()));
        }

        // enumerate all the stat types accumulated during the game
        HashSet<Stat.Type> types = new HashSet<Stat.Type>();
        for (int ii = 0; ii < bangobj.stats.length; ii++) {
            for (Iterator iter = bangobj.stats[ii].iterator();
                 iter.hasNext(); ) {
                types.add(((Stat)iter.next()).getType());
            }
        }

        // TODO: define some sort of "section" for different stats;
        // display them according to section; perhaps sort them within
        // each section

        for (Stat.Type type : types) {
            bits.add(new BLabel(_ctx.xlate(BangCodes.STATS_MSGS, type.key())));
            for (int pp = 0; pp < bangobj.stats.length; pp++) {
                Stat pstat = (Stat)bangobj.stats[pp].get(type.name());
                BLabel slabel = new BLabel(
                    pstat == null ? "" : pstat.valueToString());
                slabel.setStyleClass("right_label");
                bits.add(slabel);
            }
        }
        add(bits);

        // display awarded cash and badges if this is the end of the game
        if (bangobj.awards != null) {
            add(new BLabel(""));
            BContainer awards = new BContainer(
                GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                     GroupLayout.STRETCH));
            label = new BLabel(msgs.get("m.awards"));
            label.setStyleClass("dialog_title");
            awards.add(label);
            for (int ii = 0; ii < bangobj.awards.length; ii++) {
                BContainer pbox = GroupLayout.makeHBox(GroupLayout.LEFT);
                pbox.add(new BLabel(bangobj.players[ii].toString()));
                BLabel cash = new BLabel("" + bangobj.awards[ii].cashEarned);
                cash.setIcon(BangUI.scripIcon);
                pbox.add(cash);
                for (Badge badge : bangobj.awards[ii].badges) {
                    pbox.add(new BadgeIcon().setItem(_ctx, badge));
                }
                awards.add(pbox);
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
