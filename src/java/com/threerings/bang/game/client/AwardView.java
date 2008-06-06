//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.util.Dimension;
import com.samskivert.util.Interval;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.store.client.GoodsIcon;
import com.threerings.bang.store.data.CardTripletGood;

/**
 * Displays end-of-game awards. Used by the {@link GameOverView} and the {@link
 * BountyGameOverView}.
 */
public class AwardView extends BContainer
{
    public AwardView (BasicContext ctx, BangObject bangobj, BangConfig bconfig,
                      final PlayerObject user, Award award, boolean animate)
    {
        super(GroupLayout.makeHoriz(GroupLayout.CENTER).
              setGap(25).setOffAxisPolicy(GroupLayout.STRETCH));

        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        boolean isShort = (bconfig.type == BangConfig.Type.BOUNTY ||
                           bconfig.type == BangConfig.Type.TUTORIAL ||
                           bconfig.duration == BangConfig.Duration.PRACTICE);
        boolean isCoop = (bangobj.roundId == 0 &&
                          bangobj.scenario.getTeams() == ScenarioInfo.Teams.COOP);

        BContainer econt = new BContainer(new BorderLayout(0, award.acesEarned > 0 ? 5 : 15));
        econt.setStyleClass("endgame_border");
        add(econt);

        String rankstr = msgs.get("m.endgame_rank" + award.rank);
        String txt;
        switch (bconfig.type) {
        case BOUNTY:
            txt = msgs.get("m.bover_earnings");
            break;
        case TUTORIAL:
            txt = msgs.get("m.tover_earnings");
            break;
        default:
            txt = (isCoop ? msgs.get("m.endgame_coop_earnings") :
                   msgs.get("m.endgame_earnings", rankstr));
        }
        econt.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);

        _table = new BContainer(new TableLayout(isShort ? 3 : 7, 5, 5));
        // we need to center this verticaly
        BContainer vbox = GroupLayout.makeVBox(GroupLayout.CENTER);
        ((GroupLayout)vbox.getLayoutManager()).setGap(0);
        vbox.add(_table);
        if (user.quitter > 2) {
            vbox.add(_quitter = new BLabel(msgs.get("m.endgame_quitter"), "endgame_text"));
            if (animate) {
                _quitter.setAlpha(0f);
            }
        }
        if (award.acesEarned > 0) {
            vbox.add(_aces = GroupLayout.makeHBox(GroupLayout.CENTER));
            _aces.add(new BLabel(msgs.get("m.endgame_aces_pre"), "endgame_text"));
            _aces.add(new Spacer(-2, 1));
            _aces.add(new BLabel(BangUI.acesIcon));
            _aces.add(new BLabel(String.valueOf(award.acesEarned), "endgame_smallcash"));
            _aces.add(new BLabel(msgs.get("m.endgame_aces_post"), "endgame_text"));
            if (animate) {
                _aces.setAlpha(0f);
            }
        }
        econt.add(vbox, BorderLayout.CENTER);

        switch (bconfig.type) {
        case BOUNTY:
            txt = msgs.get("m.bover_reward");
            break;
        case TUTORIAL:
            txt = msgs.get("m.tover_reward");
            break;
        default:
            txt = (isCoop ? msgs.get("m.endgame_coop_reward") :
                   msgs.get("m.endgame_reward", rankstr));
        }
        _table.add(new BLabel(txt, "endgame_smallheader"));

        Purse purse;
        if (isShort) {
            _table.add(new Spacer(70, 1));
            purse = Purse.DEFAULT_PURSE;
        } else {
            _table.add(new Spacer(1, 1));
            purse = user.getPurse();
            if (purse.getTownIndex() == 0) { // no purse
                txt = msgs.get("m.endgame_nopurse");
            } else {
                txt = ctx.xlate(BangCodes.GOODS_MSGS, purse.getName());
            }
            _table.add(new BLabel(txt, "endgame_smallheader"));
            _table.add(new Spacer(1, 1));
            _table.add(new BLabel(msgs.get("m.endgame_total"), "endgame_smallheader"));
            _table.add(new Spacer(30, 1));
        }
        _table.add(new BLabel(msgs.get("m.endgame_have"), "endgame_header"));

        BLabel label;
        txt = _cfmt.format(Math.round(award.cashEarned / purse.getPurseBonus()));
        _table.add(label = new BLabel(txt, "endgame_smallcash"));
        label.setIcon(BangUI.scripIcon);
        label.setIconTextGap(3);

        if (!isShort) {
            _table.add(new BLabel("+", "endgame_smallcash"));

            NumberFormat pfmt = NumberFormat.getPercentInstance();
            txt = pfmt.format(purse.getPurseBonus() - 1);
            _table.add(label = new BLabel(txt, "endgame_smallcash"));
            if (purse.getTownIndex() == 0) { // no purse
                label.setIcon(new BlankIcon(64, 64));
            } else {
                BufferedImage pimg = ctx.getImageCache().getBufferedImage(purse.getIconPath());
                BImage scaled = new BImage(
                    pimg.getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH));
                label.setIcon(new ImageIcon(scaled));
            }

            _table.add(new BLabel("=", "endgame_smallcash"));

            txt = _cfmt.format(award.cashEarned);
            _table.add(label = new BLabel(txt, "endgame_smallcash"));
            label.setIcon(BangUI.scripIcon);
            label.setIconTextGap(3);
        }

        _table.add(new Spacer(1, 1));
        final int scrip = user.scrip-award.cashEarned;
        _scrip = new BLabel(_cfmt.format(scrip), "endgame_cash") {
            protected Dimension computePreferredSize (int whint, int hhint) {
                Dimension d = super.computePreferredSize(whint, hhint);
                _maxwidth = d.width = Math.max(_maxwidth, d.width);
                return d;
            }
            protected int _maxwidth;
        };
        _scrip.setIconTextGap(6);
        _scrip.setIcon(new ImageIcon(ctx.loadImage("ui/icons/big_scrip.png")));
        _table.add(_scrip);

        if (award.item != null) {
            _item = new BContainer(new BorderLayout());
            _item.setPreferredSize(250, -1);
            _item.setStyleClass("endgame_item_border");
            PaletteIcon icon = null;
            if (award.item instanceof Badge) {
                txt = msgs.get("m.endgame_badge");
                icon = new ItemIcon(ctx, award.item);
            } else if (award.item instanceof CardItem) {
                txt = msgs.get("m.endgame_card");
                CardItem card = (CardItem)award.item;
                CardTripletGood ctg =
                    new CardTripletGood(card.getType(), null, 0, 0, null);
                ctg.setQuantity(card.getQuantity());
                icon = new GoodsIcon(ctx, null, ctg);
                icon.setEnabled(false);
            } else {
                txt = msgs.get("m.endgame_item");
                icon = new ItemIcon(ctx, award.item);
            }
            _item.add(new BLabel(txt, "endgame_title"), BorderLayout.NORTH);
            _item.add(icon, BorderLayout.CENTER);
            if (award.item instanceof Badge) {
                String reward = ((Badge)award.item).getReward();
                if (reward != null) {
                    txt = ctx.xlate(BangCodes.BADGE_MSGS, reward);
                    _item.add(new BLabel(txt, "endgame_reward"), BorderLayout.SOUTH);
                }
            }
            if (animate) {
                _item.setAlpha(0f);
            }
            add(_item);

        } else if (!bconfig.rated && !isShort) {
            BContainer bcont = new BContainer(new BorderLayout());
            bcont.setPreferredSize(200, -1);
            bcont.setStyleClass("endgame_border");
            bcont.add(new BLabel(msgs.get("m.endgame_unranked"), "endgame_text"),
                      BorderLayout.CENTER);
            add(bcont);
        }

        if (!animate) {
            return;
        }

        // start up an interval that will show their earnings
        setVisible(0);
        final int scripinc = (award.cashEarned > 200) ? 20 : 5;
        final int cols = (_table.getComponentCount()/2)-1;
        new Interval(ctx.getApp()) {
            public void expired () {
                if (_step < cols) {
                    setVisible(++_step);
                    setVisible(++_step);
                    BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                    schedule(INTER_ANIM_DELAY);

                } else if (_cscrip == -1) {
                    _cscrip = scrip;
                    setVisible(++_step);
                    BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                    schedule(INTER_ANIM_DELAY);

                } else if ((_cscrip + scripinc) < user.scrip) {
                    BangUI.play(BangUI.FeedbackSound.KEY_TYPED);
                    _cscrip += scripinc;
                    _scrip.setText(_cfmt.format(_cscrip));
                    schedule(SCRIP_ANIM_DELAY);

                } else if (!_done) {
                    _scrip.setText(_cfmt.format(user.scrip));
                    BangUI.play(BangUI.FeedbackSound.ITEM_PURCHASE);
                    _done = true;
                    if (_quitter != null) {
                        _quitter.setAlpha(1f);
                    }
                    if (_aces != null) {
                        _aces.setAlpha(1f);
                    }
                    if (_item != null) {
                        schedule(POST_ANIM_DELAY);
                    }

                } else if (_item != null) {
                    _item.setAlpha(1f);
                    BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
                }
            }

            protected int _step, _cscrip = -1;
            protected boolean _done;
        }.schedule(PRE_ANIM_DELAY);
    }

    protected void setVisible (int columns)
    {
        for (int ii = 0, ll = _table.getComponentCount(); ii < ll; ii++) {
            int column = ii%(ll/2);
            _table.getComponent(ii).setAlpha(column < columns ? 1f : 0f);
        }
    }

    protected BContainer _table;
    protected BLabel _scrip, _quitter;
    protected BContainer _item, _aces;
    protected NumberFormat _cfmt = NumberFormat.getInstance();

    protected static final long PRE_ANIM_DELAY = 1000L;
    protected static final long INTER_ANIM_DELAY = 500L;
    protected static final long SCRIP_ANIM_DELAY = 50L;
    protected static final long POST_ANIM_DELAY = 2000L;
}
