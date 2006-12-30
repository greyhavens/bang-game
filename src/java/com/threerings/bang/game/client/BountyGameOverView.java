//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.ActionEvent;
import com.samskivert.util.Interval;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.bounty.client.OutlawView;
import com.threerings.bang.bounty.data.BountyConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays the results of a Bounty game.
 */
public class BountyGameOverView extends SteelWindow
{
    public BountyGameOverView (BasicContext ctx, BountyConfig config, String gameId,
                               BangConfig gconfig, BangObject bangobj)
    {
        super(ctx, ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS).get(
                  "m.bover_title", config.getGame(gameId).name));
        _contents.setStyleClass("bover_contents");

        _ctx = ctx;
        _config = config;
        _gameId = gameId;
        _gconfig = gconfig;
        _bangobj = bangobj;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        // start off with the game details
        displayDetails(true);
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();

        // start up an interval that will show the results
        new Interval(_ctx.getApp()) {
            public void expired () {
                if (_row < _rows) {
                    setRowVisible(_row++, true);
                    schedule(INTER_ANIM_DELAY);
                    BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
                } else if (_row == _rows) {
                    _overall.setAlpha(1f);
                    _row++;
                    BangUI.play(_failed == 0 ? BangUI.FeedbackSound.CHAT_SEND :
                                BangUI.FeedbackSound.INVALID_ACTION);
                    schedule(POST_ANIM_DELAY);
                } else {
                    displayResults(_failed > 0);
                }
            }
            protected int _rows = _stats.getComponentCount()/COLS, _row = 1;
        }.schedule(PRE_ANIM_DELAY);
    }

    protected void displayDetails (boolean animate)
    {
        _contents.setLayoutManager(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(25));

        AvatarView aview = new AvatarView(_ctx, 4, false, true);
        aview.setHandle((Handle)_bangobj.players[0]);
        aview.setAvatar(_bangobj.playerInfo[0].avatar);
        _contents.add(aview);

        BContainer right = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(25));
        right.add(_stats = new BContainer(new TableLayout(COLS, 30, 20)));
        _contents.add(right);

        _stats.add(new BLabel(_msgs.get("m.bover_head_crit"), "bover_header"));
        _stats.add(new BLabel(_msgs.get("m.bover_head_got"), "bover_smallheader"));
        _stats.add(new BLabel("", "bover_header"));
        _stats.add(new BLabel(_msgs.get("m.bover_head_result"), "bover_header"));

        int row = 0;
        for (Criterion crit : _gconfig.criteria) {
            _stats.add(new BLabel(_msgs.xlate(crit.getDescription()), "bover_crit"));
            _stats.add(new BLabel(crit.getCurrentState(_bangobj), "bover_rcrit"));
            _stats.add(new BLabel(_msgs.get("m.bover_equals"), "bover_result"));
            String result = "complete", style = "bover_result";
            if (crit.isMet(_bangobj) != null) {
                _failed++;
                result = "failed";
                style = "bover_failed_result";
            }
            _stats.add(new BLabel(_msgs.get("m.bover_" + result), style));
            setRowVisible(++row, false);
        }

        String result = (_failed > 0) ? "failed" : "complete";
        result = _msgs.get("m.bover_all_" + result, _config.getGame(_gameId).name);
        right.add(_overall = new BLabel(result, "bover_overall"));
        _overall.setAlpha(0f);
    }

    protected void displayResults (boolean failed)
    {
        _contents.removeAll();
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(25));

        BountyConfig.GameInfo info = _config.getGame(_gameId);
        String result = failed ? "failed" : "complete";
        _contents.add(
            _overall = new BLabel(_msgs.get("m.bover_all_" + result, info.name), "bover_overall"));

        BContainer horiz = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(25));
        OutlawView oview = new OutlawView(_ctx, 1f);
        oview.setOutlaw(_ctx, _config.outlawPrint, false);
        horiz.add(oview);

        BContainer vert = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(10));
        vert.add(new BLabel(_msgs.get("m.bover_next_" + result), "bounty_title"));

        PlayerObject user = null; // TODO
        boolean buttoned = false;
        BContainer games = new BContainer(new TableLayout(2, 2, 20));
        for (BountyConfig.GameInfo game : _config.games) {
            String key = _config.getStatKey(game.ident);
            boolean completed = (user == null) ? (game == info && !failed) : // null when testing
                user.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED, key);
            BContainer pair = GroupLayout.makeHBox(GroupLayout.LEFT);
            pair.add(new BLabel(completed ? BangUI.completed : BangUI.incomplete));
            pair.add(new BLabel(game.name, "bover_game"));
            games.add(pair);
            if (completed || buttoned) {
                games.add(new BLabel(""));
            } else {
                String msg = _msgs.get(failed ? "m.bover_replay" : "m.bover_play");
                final String ident = game.ident;
                games.add(new BButton(msg, new ActionListener() {
                    public void actionPerformed (ActionEvent event) {
                        playBountyGame(ident);
                    }
                }, ""));
                buttoned = true;
            }
        }

        vert.add(games);
        horiz.add(vert);
        _contents.add(horiz);
        _contents.add(new BLabel(failed ? info.failedQuote : info.completedQuote, "bounty_quote"));

        pack();
        center();
    }

    protected void playBountyGame (String ident)
    {
        // TODO
    }

    protected void setRowVisible (int row, boolean visible)
    {
        for (int ii = row*COLS, ll = ii+COLS; ii < ll; ii++) {
            _stats.getComponent(ii).setAlpha(visible ? 1f : 0f);
        }
    }

    protected BasicContext _ctx;
    protected BountyConfig _config;
    protected String _gameId;
    protected BangConfig _gconfig;
    protected BangObject _bangobj;

    protected MessageBundle _msgs;

    protected BContainer _stats;
    protected BLabel _overall;
    protected int _failed;

    protected static final int COLS = 4;
    protected static final long PRE_ANIM_DELAY = 1000L;
    protected static final long INTER_ANIM_DELAY = 500L;
    protected static final long POST_ANIM_DELAY = 3000L;
}
