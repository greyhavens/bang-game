//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays end-of-game awards. Used by the {@link GameOverView} and the {@link
 * BountyGameOverView}.
 */
public class AwardView extends BContainer
{
    public AwardView (BasicContext ctx, BangConfig bconfig, PlayerObject user, Award award)
    {
        super(GroupLayout.makeHoriz(GroupLayout.CENTER).
              setGap(25).setOffAxisPolicy(GroupLayout.STRETCH));

        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        boolean isBounty = (bconfig.type == BangConfig.Type.BOUNTY);

        BContainer econt = new BContainer(new BorderLayout(0, 15));
        econt.setStyleClass("endgame_border");
        add(econt);

        String rankstr = msgs.get("m.endgame_rank" + award.rank);
        String txt= isBounty ?
            msgs.get("m.bover_earnings") : msgs.get("m.endgame_earnings", rankstr);
        econt.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);

        BContainer rrow = new BContainer(new TableLayout(isBounty ? 3 : 7, 5, 5));
        // we need to center this verticaly
        BContainer vbox = GroupLayout.makeVBox(GroupLayout.CENTER);
        vbox.add(rrow);
        econt.add(vbox, BorderLayout.CENTER);

        if (isBounty) {
            rrow.add(new BLabel(msgs.get("m.bover_reward"), "endgame_smallheader"));
        } else {
            rrow.add(new BLabel(msgs.get("m.endgame_reward", rankstr), "endgame_smallheader"));
        }

        Purse purse;
        if (isBounty) {
            rrow.add(new Spacer(70, 1));
            purse = Purse.DEFAULT_PURSE;
        } else {
            rrow.add(new Spacer(1, 1));
            purse = user.getPurse();
            if (purse.getTownIndex() == 0) { // no purse
                txt = msgs.get("m.endgame_nopurse");
            } else {
                txt = ctx.xlate(BangCodes.GOODS_MSGS, purse.getName());
            }
            rrow.add(new BLabel(txt, "endgame_smallheader"));
            rrow.add(new Spacer(1, 1));
            rrow.add(new BLabel(msgs.get("m.endgame_total"), "endgame_smallheader"));
            rrow.add(new Spacer(30, 1));
        }
        rrow.add(new BLabel(msgs.get("m.endgame_have"), "endgame_header"));

        NumberFormat cfmt = NumberFormat.getInstance();
        BLabel label;
        txt = cfmt.format(Math.round(award.cashEarned / purse.getPurseBonus()));
        rrow.add(label = new BLabel(txt, "endgame_smallcash"));
        label.setIcon(BangUI.scripIcon);
        label.setIconTextGap(3);

        if (!isBounty) {
            rrow.add(new BLabel("+", "endgame_smallcash"));

            NumberFormat pfmt = NumberFormat.getPercentInstance();
            txt = pfmt.format(purse.getPurseBonus() - 1);
            rrow.add(label = new BLabel(txt, "endgame_smallcash"));
            if (purse.getTownIndex() == 0) { // no purse
                label.setIcon(new BlankIcon(64, 64));
            } else {
                BufferedImage pimg = ctx.getImageCache().getBufferedImage(purse.getIconPath());
                BImage scaled = new BImage(
                    pimg.getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH));
                label.setIcon(new ImageIcon(scaled));
            }

            rrow.add(new BLabel("=", "endgame_smallcash"));

            txt = cfmt.format(award.cashEarned);
            rrow.add(label = new BLabel(txt, "endgame_smallcash"));
            label.setIcon(BangUI.scripIcon);
            label.setIconTextGap(3);
        }

        rrow.add(new Spacer(1, 1));
        label = new BLabel(cfmt.format(user.scrip), "endgame_cash");
        label.setIconTextGap(6);
        label.setIcon(new ImageIcon(ctx.loadImage("ui/icons/big_scrip.png")));
        rrow.add(label);

        if (award.item instanceof Badge) {
            BContainer bcont = new BContainer(new BorderLayout());
            bcont.setStyleClass("endgame_border");
            txt = msgs.get("m.endgame_badge");
            bcont.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);
            bcont.add(new ItemIcon(ctx, award.item), BorderLayout.CENTER);
            String reward = ((Badge)award.item).getReward();
            if (reward != null) {
                txt = ctx.xlate(BangCodes.BADGE_MSGS, reward);
                bcont.add(new BLabel(txt, "endgame_reward"), BorderLayout.SOUTH);
            }
            add(bcont);

        } else if (!bconfig.rated && !isBounty) {
            BContainer bcont = new BContainer(new BorderLayout());
            bcont.setPreferredSize(200, -1);
            bcont.setStyleClass("endgame_border");
            bcont.add(new BLabel(msgs.get("m.endgame_unranked"), "endgame_text"),
                      BorderLayout.CENTER);
            add(bcont);
        }
    }
}
