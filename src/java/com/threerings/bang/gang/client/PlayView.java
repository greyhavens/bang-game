//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.client.CriterionView;
import com.threerings.bang.saloon.client.MatchView;
import com.threerings.bang.saloon.data.Criterion;

import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Allows the user to play a game from the hideout.
 */
public class PlayView extends BContainer
    implements HideoutCodes, GameReadyObserver
{
    public PlayView (BangContext ctx, HideoutObject hideoutobj, StatusLabel status)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);
        _hideoutobj = hideoutobj;
        _status = status;

        setStyleClass("play_view");

        add(_crview = new CriterionView(ctx) {
            protected void addRankedControl (MessageBundle msgs, BContainer table) {
                table.add(BangUI.createLabel(_msgs, "m.game_mode", "match_label"));
                table.add(_ranked = new BComboBox(xlate(_msgs, GAME_MODE)));
            }
            protected void addAIControls (MessageBundle msgs, BContainer table) {
                // nada
            }
            protected int getAllowAIs () {
                return 1; // none
            }
            protected void findMatch (Criterion criterion) {
                PlayView.this.findMatch(criterion);
            }
        });

        _ctx.getParlorDirector().addGameReadyObserver(this);
    }

    /**
     * Releases the view's resources after it is no longer needed.
     */
    public void shutdown ()
    {
        _ctx.getParlorDirector().removeGameReadyObserver(this);
    }

    // documentation inherited from interface GameReadyObserver
    public boolean receivedGameReady (int gameOid)
    {
        _gameStarting = true;
        return false;
    }

    protected void findMatch (Criterion criterion)
    {
        _hideoutobj.service.findMatch(_ctx.getClient(), criterion,
            new HideoutService.ResultListener() {
                public void requestProcessed (Object result) {
                    displayMatchView((Integer)result);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(HIDEOUT_MSGS, cause, true);
                    _crview.reenable();
                }
            });
    }

    protected void displayMatchView (int matchOid)
    {
        // remove our criterion view
        if (_crview.isAdded()) {
            remove(_crview);
        }

        // this should never happen, but just to be ultra-robust
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // display a match view for this pending match
        add(_mview = new MatchView(_ctx, matchOid, false) { {
                ((GroupLayout)getLayoutManager()).setGap(10);
                setStyleClass("hideout_match_view");
                _info.remove(_opponents);
            }
            protected void updateRanked () {
                _ranked.setText(PlayView.this._msgs.get(
                    _mobj.criterion.getDesiredRankedness() ? "m.compete" : "m.practice"));
            }
            protected void leaveMatch (int matchOid) {
                PlayView.this.leaveMatch(matchOid);
            }
        });
    }

    protected void leaveMatch (int matchOid)
    {
        if (_gameStarting) {
            return;
        }
        if (matchOid != -1) {
            _hideoutobj.service.leaveMatch(_ctx.getClient(), matchOid);
        }

        // out with the old match view
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // redisplay the criterion view
        if (!_crview.isAdded()) {
            add(_crview);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected StatusLabel _status;

    protected CriterionView _crview;
    protected MatchView _mview;

    protected boolean _gameStarting;

    protected static final String[] GAME_MODE = { "compete", "practice" };
}
