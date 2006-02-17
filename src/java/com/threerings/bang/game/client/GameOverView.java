//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BadgeIcon;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BangController;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays the results at the end of the game.
 */
public class GameOverView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * The constructor used by the actual game.
     */
    public GameOverView (BangContext ctx, BangController ctrl,
                         BangObject bangobj)
    {
        this(ctx, ctrl, bangobj, ctx.getUserObject());
    }

    /**
     * The constructor used by the test harness.
     */
    public GameOverView (BasicContext ctx, BangController ctrl,
                         BangObject bangobj, PlayerObject user)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVert(GroupLayout.TOP));
        ((GroupLayout)getLayoutManager()).setGap(20);

        _ctx = ctx;
        _ctrl = ctrl;

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            GameCodes.GAME_MSGS);
        int pidx = bangobj.getPlayerIndex(user.getVisibleName());
        Award award = null;

        add(new BLabel(msgs.get("m.endgame_title"), "scroll_title"));

        // display the players' avatars in rank order
        GroupLayout gl = GroupLayout.makeHoriz(GroupLayout.CENTER);
        gl.setGap(15);
        BContainer who = new BContainer(gl);
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            int apidx = bangobj.awards[ii].pidx;
            if (pidx == apidx) {
                award = bangobj.awards[ii];
            }

            int[] avatar = null;
            // look up this player's avatar image if they're still around
            BangOccupantInfo boi = (BangOccupantInfo)
                bangobj.getOccupantInfo(bangobj.players[apidx]);
            if (boi != null) {
                avatar = boi.avatar;
            }
            who.add(new FinalistView(ctx, apidx, bangobj.players[apidx], avatar,
                                     bangobj.awards[ii].rank));
        }
        add(who);

        // display our earnings and awarded badge (if any)
        if (award != null) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            ((GroupLayout)row.getLayoutManager()).setGap(25);
            add(row);

            BContainer econt = new BContainer(new BorderLayout());
            econt.setStyleClass("palette_backgound");
            String txt = msgs.get("m.endgame_earnings");
            econt.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);
            txt = "$" + award.cashEarned;
            econt.add(new BLabel(txt, "endgame_cash"), BorderLayout.WEST);
            Purse purse = user.getPurse();
            econt.add(purse.createIcon().setItem(ctx, purse),
                      BorderLayout.CENTER);
            txt = "$" + user.scrip;
            econt.add(new BLabel(txt, "endgame_cash"), BorderLayout.EAST);
            row.add(econt);

            if (award.badge != null) {
                BContainer bcont = new BContainer(new BorderLayout());
                bcont.setStyleClass("palette_backgound");
                txt = msgs.get("m.endgame_badge");
                bcont.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);
                bcont.add(new BadgeIcon().setItem(ctx, award.badge),
                          BorderLayout.CENTER);
                row.add(bcont);
            }
        }

        // add some buttons at the bottom
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(msgs.get("m.to_saloon"), this, "to_saloon"));
        buttons.add(new BButton(msgs.get("m.to_town"), this, "to_town"));
        add(buttons);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("to_town") || action.equals("to_saloon")) {
            _ctx.getRootNode().removeWindow(GameOverView.this);
            _ctrl.statsDismissed(action.equals("to_town"));
        }
    }

    protected BasicContext _ctx;
    protected BangController _ctrl;
}
