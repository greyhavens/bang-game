//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.text.NumberFormat;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.icon.BIcon;
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
import com.threerings.bang.client.PickTutorialView;
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
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.BangConfig;

/**
 * Displays the results at the end of the game.
 */
public class GameOverView extends SteelWindow
    implements ActionListener
{
    /**
     * Creates the BLabel showing the coop performance of a team.
     */
    public static BLabel createCoopIcon (
            BasicContext ctx, ScenarioInfo info, int rank, boolean small)
    {
        BufferedImage on = ctx.getImageCache().getBufferedImage(
                "ui/postgame/" + info.getIdent() + "_on.png");
        BufferedImage off = ctx.getImageCache().getBufferedImage(
                "ui/postgame/" + info.getIdent() + "_off.png");
        int width = (int)(on.getWidth() * (small ? 0.5 : 1)),
            height = (int)(on.getHeight() * (small ? 0.5 : 1));
        BufferedImage bar = ctx.getImageCache().createCompatibleImage(
                width, height, true);
        Graphics2D g = bar.createGraphics();
        AffineTransformOp halfOp = (small ?
                new AffineTransformOp(
                    AffineTransform.getScaleInstance(0.5, 0.5),
                    AffineTransformOp.TYPE_BILINEAR) :
                null);
        g.drawImage(off, halfOp, 0, 0);
        int onWidth = rank * on.getWidth() / 100;
        if (onWidth > 0 && onWidth <= on.getWidth()) {
            g.drawImage(on.getSubimage(0, 0, rank * on.getWidth() / 100,
                                       on.getHeight()), halfOp, 0, 0);
        }
        return new BLabel(new ImageIcon(new BImage(bar)));
    }

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
        _bobj = bangobj;

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            GameCodes.GAME_MSGS);
        int pidx = bangobj.getPlayerIndex(user.getVisibleName());
        Award award = null;

        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.TOP));

        // display the players' avatars in rank order
        if (bangobj.roundId == 1 &&
                bangobj.scenario.getTeams() == ScenarioInfo.Teams.COOP) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            _contents.add(row);
            row.add(new CoopFinalistView(ctx, bangobj, ctrl));
            for (int ii = 0; ii < bangobj.awards.length; ii++) {
                if (pidx == bangobj.awards[ii].pidx) {
                    award = bangobj.awards[ii];
                    _cueidx = award.rank;
                }
            }

        } else {
            _contents.add(new Spacer(1, -50)); // kids, don't try this at home
            GroupLayout gl = GroupLayout.makeHoriz(GroupLayout.CENTER);
            gl.setGap(15);
            BContainer who = new BContainer(gl);
            GroupLayout vl = GroupLayout.makeVert(GroupLayout.CENTER);
            vl.setGap(15);
            BContainer split = new BContainer(vl);
            BContainer losers = new BContainer(gl);
            for (int ii = 0; ii < bangobj.awards.length; ii++) {
                int apidx = bangobj.awards[ii].pidx;
                if (pidx == apidx) {
                    award = bangobj.awards[ii];
                    _cueidx = award.rank;
                }
                FinalistView view = new FinalistView(
                        ctx, bangobj, ctrl, apidx, bangobj.awards[ii].rank);
                if (ii == 0) {
                    who.add(view);
                    who.add(split);
                    if (bangobj.roundId > 1) {
                        split.add(new Spacer(1, 20));
                    }
                    split.add(losers);
                } else {
                    losers.add(view);
                }
            }

            // Display a summary of your round ranks for a multi-round game
            if (bangobj.roundId > 1 && pidx > -1) {
                BangConfig bconfig;
                if (ctrl == null) {
                    bconfig = new BangConfig();
                    bconfig.scenarios = new String[] { "tb", "wa", "fg" };
                } else {
                    bconfig = (BangConfig)ctrl.getPlaceConfig();
                }

                BContainer ranks = new BContainer(new TableLayout(3, 2, 5));
                for (int ii = 0; ii < bangobj.perRoundRanks.length; ii++) {
                    ranks.add(new BLabel(msgs.get(
                        "m.endgame_round", "" + (ii + 1)), "endgame_round"));
                    String scid = bconfig.scenarios[ii];
                    ranks.add(new BLabel(msgs.get("m.scenario_" + scid),
                               "endgame_desc"));
                    ScenarioInfo info = ScenarioInfo.getScenarioInfo(scid);
                    int rank = bangobj.perRoundRanks[ii][pidx];
                    if (info.getTeams() == ScenarioInfo.Teams.COOP) {
                        rank -= BangObject.COOP_RANK;
                        ranks.add(createCoopIcon(_ctx, info, rank, true));
                    } else {
                        ranks.add(new BLabel(msgs.get("m.endgame_place",
                            msgs.get("m.endgame_rank" + rank)),
                                    "endgame_desc"));
                    }
                }

                ranks.add(new BLabel(msgs.get("m.endgame_overall"),
                            "endgame_round"));
                ranks.add(new Spacer(1, 30));
                ranks.add(new BLabel(msgs.get("m.endgame_place",
                    msgs.get("m.endgame_rank" + _cueidx)), "endgame_desc"));
                split.add(ranks);
            }
            _contents.add(who);
        }

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
        _buttons.add(new BButton(msgs.get("m.view_stats"), this, "stats"));
        // watchers don't get to go back to parlors because they may not have
        // come from there and may not have been invited
        String from = _bobj.priorLocation.ident;
        if (pidx >= -1 || !"parlor".equals(from)) {
            _buttons.add(new BButton(msgs.get("m.to_" + from), this,
                                     "to_" + from));
        }
        _buttons.add(new BButton(msgs.get("m.to_town"), this, "to_town"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.startsWith("to_")) {
            _bctx.getBangClient().clearPopup(this, true);
            if (action.equals("to_town")) {
                _bctx.getLocationDirector().leavePlace();
                _bctx.getBangClient().showTownView();

            } else if (action.equals("to_tutorial")) {
                // display the pick tutorial view in "finished tutorial" mode
                _bctx.getBangClient().displayPopup(
                    new PickTutorialView(
                        _bctx, PickTutorialView.Mode.COMPLETED), true);

            } else {
                _bctx.getLocationDirector().moveTo(
                    _bobj.priorLocation.placeOid);
            }

        } else if (action.equals("stats")) {
            _bctx.getBangClient().clearPopup(this, true);
            _bctx.getBangClient().displayPopup(_ctrl.getStatsView(), true);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // cue up our end of game riff
        if (_ctx instanceof BangContext) {
            _bctx.getBangClient().queueMusic(
                "frontier_town/post_game" + _cueidx, false, 2f);
        }
    }

    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BangController _ctrl;
    protected BangObject _bobj;
    protected int _cueidx = 2;
}
