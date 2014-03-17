//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
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
    public PlayView (BangContext ctx, HideoutObject hideoutobj, BContainer bcont,
            BToggleButton button, StatusLabel status)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);
        _hideoutobj = hideoutobj;
        _bcont = bcont;
        _button = button;
        _status = status;

        setStyleClass("play_view");

        add(_crview = new CriterionView(ctx, "hideout") { {
                BContainer cont = GroupLayout.makeVBox(GroupLayout.TOP);
                add(cont, BorderLayout.NORTH);
                cont.add(new BLabel(_msgs.get("m.game_tip"), "play_tip"));
                cont.add(new Spacer(1, -20));
            }
            protected void addModeControl (MessageBundle msgs, BContainer table) {
                table.add(BangUI.createLabel(msgs, "m.game_mode", "match_label"));
                if (!_ctx.getUserObject().townId.equals(BangCodes.FRONTIER_TOWN)) {
                    table.add(_mode = new BComboBox(xlate(msgs, GAME_MODE)));
                } else {
                    table.add(BangUI.createLabel(msgs, "m." + GAME_MODE[0], "match_info"));
                }
            }
            protected int getModeSelection ()
            {
                if (_mode == null) {
                    return Criterion.COMP;
                }
                switch (_mode.getSelectedIndex()) {
                    case 1:
                        return Criterion.COOP;
                    default:
                        return Criterion.COMP;
                }
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
        _hideoutobj.service.findMatch(criterion, new HideoutService.ResultListener() {
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

        // disable the gang buttons while matchmaking
        _bcont.setEnabled(false);
        _button.setEnabled(false);

        // display a match view for this pending match
        add(_mview = new MatchView(_ctx, matchOid) { {
                ((GroupLayout)getLayoutManager()).setGap(10);
                setStyleClass("hideout_match_view");
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
            _hideoutobj.service.leaveMatch(matchOid);
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

        // reenable the gang buttons
        _bcont.setEnabled(true);
        _button.setEnabled(true);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected BContainer _bcont;
    protected BToggleButton _button;
    protected StatusLabel _status;

    protected CriterionView _crview;
    protected MatchView _mview;

    protected boolean _gameStarting;

    protected static final String[] GAME_MODE = { "comp", "coop" };
}
