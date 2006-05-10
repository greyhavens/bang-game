//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangCodes;
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
public class GameOverView extends SteelWindow
    implements ActionListener
{
    /**
     * The constructor used by the actual game.
     */
    public GameOverView (BangContext ctx, BangController ctrl,
                         BangObject bangobj)
    {
        this(ctx, ctrl, bangobj, ctx.getUserObject());
        _bctx = ctx;
    }

    /**
     * The constructor used by the test harness.
     */
    public GameOverView (BasicContext ctx, BangController ctrl,
                         BangObject bangobj, PlayerObject user)
    {
        super(ctx, ctx.xlate(GameCodes.GAME_MSGS, "m.endgame_title"));
        setLayer(1);

        _ctx = ctx;
        _ctrl = ctrl;

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            GameCodes.GAME_MSGS);
        int pidx = bangobj.getPlayerIndex(user.getVisibleName());
        Award award = null;

        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.TOP));
        _contents.add(new Spacer(1, -50)); // kids, don't try this at home

        // display the players' avatars in rank order
        GroupLayout gl = GroupLayout.makeHoriz(GroupLayout.CENTER);
        gl.setGap(15);
        BContainer who = new BContainer(gl);
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            int apidx = bangobj.awards[ii].pidx;
            if (pidx == apidx) {
                award = bangobj.awards[ii];
                _cueidx = award.rank;
            }
            who.add(new FinalistView(ctx, apidx, bangobj.players[apidx],
                                     bangobj.avatars[apidx],
                                     bangobj.awards[ii].rank));
        }
        _contents.add(who);

        // display our earnings and awarded badge (if any)
        if (award != null) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            ((GroupLayout)row.getLayoutManager()).setGap(25);
            ((GroupLayout)row.getLayoutManager()).setOffAxisPolicy(
                GroupLayout.STRETCH);
            _contents.add(row);

            BContainer econt = new BContainer(new BorderLayout(0, 15));
            econt.setStyleClass("endgame_border");
            row.add(econt);

            String rankstr = msgs.get("m.endgame_rank" + award.rank);
            String txt = msgs.get("m.endgame_earnings", rankstr);
            econt.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);

            BContainer rrow = new BContainer(new TableLayout(7, 5, 5));
            // we need to center this verticaly
            BContainer vbox = GroupLayout.makeVBox(GroupLayout.CENTER);
            vbox.add(rrow);
            econt.add(vbox, BorderLayout.CENTER);

            rrow.add(new BLabel(msgs.get("m.endgame_reward", rankstr),
                                "endgame_smallheader"));
            rrow.add(new Spacer(1, 1));

            Purse purse = user.getPurse();
            if (purse.getTownIndex() == 0) { // no purse
                txt = msgs.get("m.endgame_nopurse");
            } else {
                txt = ctx.xlate(BangCodes.GOODS_MSGS, purse.getName());
            }
            rrow.add(new BLabel(txt, "endgame_smallheader"));

            rrow.add(new Spacer(1, 1));
            rrow.add(new BLabel(msgs.get("m.endgame_total"),
                                "endgame_smallheader"));
            rrow.add(new Spacer(30, 1));
            rrow.add(new BLabel(msgs.get("m.endgame_have"),
                                "endgame_header"));

            NumberFormat cfmt = NumberFormat.getInstance();
            BLabel label;
            txt = cfmt.format(Math.round(award.cashEarned /
                                         purse.getPurseBonus()));
            rrow.add(label = new BLabel(txt, "endgame_smallcash"));
            label.setIcon(BangUI.scripIcon);
            label.setIconTextGap(3);

            rrow.add(new BLabel("+", "endgame_smallcash"));

            NumberFormat pfmt = NumberFormat.getPercentInstance();
            txt = pfmt.format(purse.getPurseBonus() - 1);
            rrow.add(label = new BLabel(txt, "endgame_smallcash"));
            if (purse.getTownIndex() == 0) { // no purse
                label.setIcon(new BlankIcon(64, 64));
            } else {
                BufferedImage pimg = ctx.getImageCache().getBufferedImage(
                    purse.getIconPath());
                BImage scaled = new BImage(
                    pimg.getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH));
                label.setIcon(new ImageIcon(scaled));
            }

            rrow.add(new BLabel("=", "endgame_smallcash"));

            txt = cfmt.format(award.cashEarned);
            rrow.add(label = new BLabel(txt, "endgame_smallcash"));
            label.setIcon(BangUI.scripIcon);
            label.setIconTextGap(3);

            rrow.add(new Spacer(1, 1));

            label = new BLabel(cfmt.format(user.scrip), "endgame_cash");
            label.setIconTextGap(6);
            label.setIcon(new ImageIcon(
                              ctx.loadImage("ui/icons/big_scrip.png")));
            rrow.add(label);

            if (award.badge != null) {
                BContainer bcont = new BContainer(new BorderLayout());
                bcont.setStyleClass("endgame_border");
                txt = msgs.get("m.endgame_badge");
                bcont.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);
                bcont.add(new ItemIcon(ctx, award.badge), BorderLayout.CENTER);
                String reward = award.badge.getReward();
                if (reward != null) {
                    txt = _ctx.xlate(BangCodes.BADGE_MSGS, reward);
                    bcont.add(new BLabel(txt, "endgame_reward"),
                              BorderLayout.SOUTH);
                }
                row.add(bcont);
            }
        }

        // add some buttons at the bottom
        _buttons.add(new BButton(msgs.get("m.to_saloon"), this, "to_saloon"));
        _buttons.add(new BButton(msgs.get("m.to_town"), this, "to_town"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("to_town") || action.equals("to_saloon")) {
            _bctx.getBangClient().clearPopup(this, true);
            _ctrl.statsDismissed(action.equals("to_town"));
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // cue up our end of game riff
        if (_ctx instanceof BangContext) {
            String mpath = "sounds/music/post_game" + _cueidx + ".ogg";
            _bctx.getBangClient().queueMusic(mpath, false, 2f);
        }
    }

    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BangController _ctrl;
    protected int _cueidx = 2;
}
