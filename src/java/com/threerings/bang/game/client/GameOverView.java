//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.WhereToView;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BangController;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.jme.util.ImageCache;

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
        BufferedImage bar = ImageCache.createCompatibleImage(width, height, true);
        Graphics2D g = bar.createGraphics();
        AffineTransformOp halfOp = (small ? new AffineTransformOp(
                                        AffineTransform.getScaleInstance(0.5, 0.5),
                                        AffineTransformOp.TYPE_BILINEAR) : null);
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
    public GameOverView (BangContext ctx, BangController ctrl, BangObject bangobj, boolean animate)
    {
        this(ctx, ctrl, (BangConfig)ctrl.getPlaceConfig(), bangobj, ctx.getUserObject(), animate);
        _bctx = ctx;
    }

    /**
     * The constructor used by the test harness.
     */
    public GameOverView (BasicContext ctx, BangController ctrl, BangConfig bconfig,
                         BangObject bangobj, PlayerObject user, boolean animate)
    {
        super(ctx, ctx.xlate(GameCodes.GAME_MSGS, "m.endgame_title"));
        setLayer(1);

        _ctx = ctx;
        _ctrl = ctrl;
        _bobj = bangobj;

        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        int pidx = bangobj.getPlayerIndex(user.getVisibleName());
        Award award = null;

        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.TOP));

        // display the players' avatars in rank order
        if (bangobj.roundId == 0 && bangobj.scenario.getTeams() == ScenarioInfo.Teams.COOP) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            _contents.add(row);
            row.add(new CoopFinalistView(ctx, bangobj, ctrl));
            for (int ii = 0; ii < bangobj.awards.length; ii++) {
                if (pidx == bangobj.awards[ii].pidx) {
                    award = bangobj.awards[ii];
                    // let's cue the music based on the team performance
                    _cueidx = 3 - (bangobj.perRoundRanks[0][0] - BangObject.COOP_RANK - 1)/25;
                }
            }

        } else {
            GroupLayout gl = GroupLayout.makeHoriz(GroupLayout.CENTER);
            gl.setGap(15);
            GroupLayout vl = GroupLayout.makeVert(GroupLayout.CENTER);
            vl.setGap(15);
            BContainer split = new BContainer(vl);
            BContainer who = new BContainer(gl);
            BContainer losers = new BContainer(gl);
            _contents.add(new Spacer(1, -50)); // kids, don't try this at home
            if (bangobj.isTeamGame()) {
                int rank = 0;
                boolean[] added = new boolean[bangobj.awards.length];
                for (int ii = 0; ii < bangobj.awards.length; ii++) {
                    int apidx = bangobj.awards[ii].pidx;
                    int tidx = bangobj.teams[apidx];
                    if (!added[tidx]) {
                        added[tidx] = true;
                        TeamFinalistView view = new TeamFinalistView(ctx, bangobj, ctrl, tidx, rank);
                        if (rank == 0) {
                            who.add(view);
                            who.add(split);
                            if (bangobj.roundId > 0) {
                                split.add(new Spacer(1, 20));
                            }
                            split.add(losers);
                        } else {
                            losers.add(view);
                        }
                        rank++;
                    }
                    if (pidx == apidx) {
                        award = bangobj.awards[ii];
                        _cueidx = award.rank;
                    }
                }

            } else {
                for (int ii = 0; ii < bangobj.awards.length; ii++) {
                    int apidx = bangobj.awards[ii].pidx;
                    if (pidx == apidx) {
                        award = bangobj.awards[ii];
                        _cueidx = award.rank;
                    }
                    FinalistView view =
                        new FinalistView(ctx, bangobj, ctrl, apidx, bangobj.awards[ii].rank);
                    if (ii == 0) {
                        who.add(view);
                        who.add(split);
                        if (bangobj.roundId > 0) {
                            split.add(new Spacer(1, 20));
                        }
                        split.add(losers);
                    } else {
                        losers.add(view);
                    }
                }
            }
            _contents.add(who);

            // display a summary of your round ranks for a multi-round game
            if (bangobj.roundId > 0 && pidx > -1) {
                BContainer ranks = new BContainer(new TableLayout(3, 2, 5));
                for (int ii = 0; ii < bangobj.perRoundRanks.length; ii++) {
                    String msg = msgs.get("m.endgame_round", "" + (ii + 1));
                    ranks.add(new BLabel(msg, "endgame_round"));
                    String scid = bconfig.getScenario(ii);
                    ranks.add(new BLabel(msgs.get("m.scenario_" + scid), "endgame_desc"));
                    ScenarioInfo info = ScenarioInfo.getScenarioInfo(scid);
                    int rank = bangobj.perRoundRanks[ii][pidx];
                    if (info.getTeams() == ScenarioInfo.Teams.COOP) {
                        rank -= BangObject.COOP_RANK;
                        ranks.add(createCoopIcon(_ctx, info, rank, true));
                    } else {
                        msg = msgs.get("m.endgame_place", msgs.get("m.endgame_rank" + rank));
                        ranks.add(new BLabel(msg, "endgame_desc"));
                    }
                }

                ranks.add(new BLabel(msgs.get("m.endgame_overall"), "endgame_round"));
                ranks.add(new Spacer(1, 30));
                String msg = msgs.get("m.endgame_place", msgs.get("m.endgame_rank" + _cueidx));
                ranks.add(new BLabel(msg, "endgame_desc"));
                split.add(ranks);
            }
        }

        // display our earnings and awarded badge (if any)
        if (award != null) {
            _contents.add(new AwardView(_ctx, bangobj, bconfig, user, award, animate));
        }

        // add some buttons at the bottom
        _buttons.add(new BButton(msgs.get("m.view_stats"), this, "stats"));
        if (_ctx instanceof BangContext) {
            String from = ((BangContext)_ctx).getBangClient().getPriorLocationIdent();
            if (bconfig.duration == BangConfig.Duration.PRACTICE) {
                from = "tutorial";
            }
            _buttons.add(new BButton(msgs.get("m.to_" + from), this, "to_" + from));
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
                _bctx.getBangClient().showTownView();

            } else if (action.equals("to_tutorial")) {
                _bctx.getBangClient().displayPopup(
                    new WhereToView(_bctx, true), true, WhereToView.WIDTH_HINT);

            } else {
                _bctx.getLocationDirector().moveTo(_bctx.getBangClient().getPriorLocationOid());
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
            _bctx.getBangClient().queueMusic("frontier_town/post_game" + _cueidx, false, 2f);
        }
    }

    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BangController _ctrl;
    protected BangObject _bobj;
    protected int _cueidx = 2;
}
