//
// $Id$

package com.threerings.bang.game.client;

import com.jme.system.DisplaySystem;
import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.Interval;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.bounty.client.OutlawView;
import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays the results of a Bounty game.
 */
public class BountyGameOverView extends SteelWindow
    implements ActionListener
{
    public BountyGameOverView (BasicContext ctx, BountyConfig bounty, String gameId,
                               BangConfig gconfig, BangObject bangobj, PlayerObject user)
    {
        super(ctx, "");
        _contents.setStyleClass("bover_contents");

        _ctx = ctx;
        _bctx = (ctx instanceof BangContext) ? (BangContext)ctx : null;
        _bounty = bounty;
        _gameId = gameId;
        _gconfig = gconfig;
        _bangobj = bangobj;
        _user = user;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        // locate our award
        int pidx = _bangobj.getPlayerIndex(_user.getVisibleName());
        for (Award award : _bangobj.awards) {
            if (pidx == award.pidx) {
                _award = award;
                break;
            }
        }

        // start off with the game details
        displayDetails(true);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("to_town")) {
            _bctx.getBangClient().clearPopup(this, true);
            _bctx.getLocationDirector().leavePlace();
            _bctx.getBangClient().showTownView();

        } else if (action.equals("to_office")) {
            _bctx.getBangClient().clearPopup(this, true);
            BangBootstrapData bbd = (BangBootstrapData)_bctx.getClient().getBootstrapData();
            _bctx.getLocationDirector().moveTo(bbd.officeOid);

        } else if (action.equals("stats")) {
            displayDetails(false);

        } else if (action.equals("results")) {
            displayResults(false);

        } else if (action.startsWith("play_")) {
            final BButton play = (BButton)event.getSource();
            play.setEnabled(false);
            PlayerService psvc = (PlayerService)
                _bctx.getClient().requireService(PlayerService.class);
            psvc.playBountyGame(_bctx.getClient(), _bounty.ident, action.substring(5),
                                new PlayerService.InvocationListener() {
                public void requestFailed (String cause) {
                    _bctx.getChatDirector().displayFeedback(OfficeCodes.OFFICE_MSGS, cause);
                    play.setEnabled(true);
                }
            });
        }
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();

        // fade out the game music
        if (_bctx != null) {
            _bctx.getBangClient().fadeOutMusic(2f);
        }

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
                     displayResults(true);
                }
            }
            protected int _rows = _stats.getComponentCount()/COLS, _row = 1;
        }.schedule(PRE_ANIM_DELAY);
    }

    protected void displayDetails (boolean animate)
    {
        _header.setText(_msgs.get("m.bover_dtitle", _bounty.getGame(_gameId).name));
        _contents.removeAll();
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

        int row = _failed = 0;
        for (Criterion crit : _gconfig.criteria) {
            _stats.add(new BLabel(_msgs.xlate(crit.getDescription()), "bover_crit"));
            _stats.add(new BLabel(_msgs.xlate(crit.getCurrentValue(_bangobj, _award.rank)),
                                  "bover_rcrit"));
            _stats.add(new BLabel(_msgs.get("m.bover_equals"), "bover_result"));
            String result = "complete", style = "bover_result";
            if (!crit.isMet(_bangobj, _award.rank)) {
                _failed++;
                result = "failed";
                style = "bover_failed_result";
            }
            _stats.add(new BLabel(_msgs.get("m.bover_" + result), style));
            setRowVisible(++row, !animate);
        }

        String result = (_failed > 0) ? "failed" : "complete";
        result = _msgs.get("m.bover_game_" + result, _bounty.getGame(_gameId).name);
        right.add(_overall = new BLabel(result, "bover_overall"));
        _overall.setAlpha(animate ? 0f : 1f);

        _buttons.removeAll();
        if (!animate) {
            _buttons.add(new BButton(_msgs.get("m.bover_results"), this, "results"));
        }

        // relayout and recenter the window if we're already showing
        if (isShowing()) {
            relayout();
        }
    }

    protected void displayResults (boolean playMusic)
    {
        // note whether we've completed the entire bounty
        boolean gfailed = (_failed > 0), completed = !gfailed && _bounty.isCompleted(_user);

        _header.setText(_msgs.get("m.bover_rtitle", _bounty.title));
        _contents.removeAll();
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(35));

        BountyConfig.GameInfo info = _bounty.getGame(_gameId);
        String result = gfailed ? "failed" : "complete";
        String msg = completed ? _msgs.get("m.bover_all_complete", _bounty.title) :
            _msgs.get("m.bover_game_" + result, info.name);
        _contents.add(_overall = new BLabel(msg, "bover_overall"));

        BountyConfig.Quote quote = gfailed ? info.failedQuote : info.completedQuote;
        BContainer horiz = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(25));
        horiz.add(PreGameBountyView.createSpeakerView(
                      _ctx, _bounty, _gameId, _gconfig, quote, completed));

        BContainer vert = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).setGap(10));
        vert.add(new BLabel(_msgs.get(completed ? "m.bover_next_all_complete" :
                                      "m.bover_next_" + result), "bounty_title"));

        boolean buttoned = false;
        BContainer games = new BContainer(new TableLayout(2, 2, 20));
        for (BountyConfig.GameInfo game : _bounty.games) {
            String key = _bounty.getStatKey(game.ident);
            boolean gcompleted = _user.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED, key);
            BContainer pair = GroupLayout.makeHBox(GroupLayout.LEFT);
            pair.add(new BLabel(gcompleted ? BangUI.completed : BangUI.incomplete));
            pair.add(new BLabel(game.name, "bover_game"));
            games.add(pair);
            if (gcompleted || buttoned) {
                games.add(new BLabel(""));
            } else {
                msg = _msgs.get(gfailed ? "m.bover_replay" : "m.bover_play");
                games.add(new BButton(msg, this, "play_" + game.ident));
                buttoned = true;
            }
        }

        vert.add(games);
        horiz.add(vert);
        _contents.add(horiz);

        if (completed) {
            _contents.add(new AwardView(_ctx, _gconfig, _user, _award));
        } else {
            msg = quote.text;
            _contents.add(new BLabel(msg, "bounty_quote"));
        }

        _buttons.removeAll();
        _buttons.add(new BButton(_msgs.get("m.bover_stats"), this, "stats"));
        _buttons.add(new BButton(_msgs.get("m.to_office"), this, "to_office"));

        // relayout and recenter the window
        relayout();

        // cue up our end of game riff (1st place for all completed, 2nd place for game won, 4th
        // place for game lost)
        int cueidx = completed ? 0 : (gfailed ? 3 : 1);
        if (_bctx != null && playMusic) {
            _bctx.getBangClient().queueMusic("frontier_town/post_game" + cueidx, false, 2f);
        }
    }

    /**
     * Re-pack()s and recenters the window, preserving the non-center vertical centering (which
     * accounts for the space used by the player status views).
     */
    protected void relayout ()
    {
        int oy = getY(), oheight = getHeight(); // preserve our funny vertical centering
        pack();
        int width = DisplaySystem.getDisplaySystem().getWidth();
        setLocation((width-getWidth())/2, oy + (oheight-getHeight())/2);
    }

    protected void setRowVisible (int row, boolean visible)
    {
        for (int ii = row*COLS, ll = ii+COLS; ii < ll; ii++) {
            _stats.getComponent(ii).setAlpha(visible ? 1f : 0f);
        }
    }

    protected BasicContext _ctx;
    protected BangContext _bctx;
    protected BountyConfig _bounty;
    protected String _gameId;
    protected BangConfig _gconfig;
    protected BangObject _bangobj;
    protected PlayerObject _user;
    protected Award _award;

    protected MessageBundle _msgs;

    protected BContainer _stats;
    protected BLabel _overall;
    protected int _failed;

    protected static final int COLS = 4;
    protected static final long PRE_ANIM_DELAY = 1000L;
    protected static final long INTER_ANIM_DELAY = 500L;
    protected static final long POST_ANIM_DELAY = 3000L;
}
