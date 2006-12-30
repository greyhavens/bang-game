//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.samskivert.util.Interval;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.client.AvatarView;
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

        // start off with the game details
        displayDetails(true);
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

        MessageBundle msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _stats.add(new BLabel(msgs.get("m.bover_head_crit"), "bover_header"));
        _stats.add(new BLabel(msgs.get("m.bover_head_got"), "bover_smallheader"));
        _stats.add(new BLabel("", "bover_header"));
        _stats.add(new BLabel(msgs.get("m.bover_head_result"), "bover_header"));

        int row = 0;
        for (Criterion crit : _gconfig.criteria) {
            _stats.add(new BLabel(msgs.xlate(crit.getDescription()), "bover_crit"));
            _stats.add(new BLabel(crit.getCurrentState(_bangobj), "bover_rcrit"));
            _stats.add(new BLabel(msgs.get("m.bover_equals"), "bover_result"));
            String result = "complete", style = "bover_result";
            if (crit.isMet(_bangobj) != null) {
                _failed++;
                result = "failed";
                style = "bover_failed_result";
            }
            _stats.add(new BLabel(msgs.get("m.bover_" + result), style));
            setRowVisible(++row, false);
        }

        String result = (_failed > 0) ? "failed" : "complete";
        result = msgs.get("m.bover_all_" + result, _config.getGame(_gameId).name);
        right.add(_overall = new BLabel(result, "bover_overall"));
        _overall.setAlpha(0f);
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
                    System.err.println("Next phase!");
                }
            }
            protected int _rows = _stats.getComponentCount()/COLS, _row = 1;
        }.schedule(PRE_ANIM_DELAY);
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

    protected BContainer _stats;
    protected BLabel _overall;
    protected int _failed;

    protected static final int COLS = 4;
    protected static final long PRE_ANIM_DELAY = 1000L;
    protected static final long INTER_ANIM_DELAY = 500L;
    protected static final long POST_ANIM_DELAY = 2000L;
}
