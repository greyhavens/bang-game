//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.TopRankObject;
import com.threerings.bang.saloon.data.TopRankedList;

/**
 * Displays top scorers in the Saloon or elsewhere.
 */
public class TopScoreView extends BContainer
{
    public TopScoreView (BangContext ctx, TopRankObject rankobj)
    {
        super(new BorderLayout());
        setStyleClass("top_score_view");
        _ctx = ctx;
        _rankobj = rankobj;
        _msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)buttons.getLayoutManager()).setGap(10);
        buttons.add(_left = new BButton(new BlankIcon(37,25), _navigator, "left"));
        _left.setStyleClass("arrow_back_button");
        _left.setEnabled(false);
        buttons.add(_title = new BLabel(""));
        _title.setPreferredSize(150, -1);
        _title.setStyleClass("top10_title");
        buttons.add(_right = new BButton(new BlankIcon(37,25), _navigator, "right"));
        _right.setStyleClass("arrow_fwd_button");
        add(buttons, BorderLayout.NORTH);

        _listCont = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.STRETCH));
        add(new BScrollPane(_listCont), BorderLayout.CENTER);

        boolean thisWeek = false, lastWeek = false;
        int pages = 1;
        for (TopRankedList list : rankobj.getTopRanked()) {
            if (list.period == TopRankedList.THIS_WEEK) {
                if (!thisWeek) {
                    pages++;
                }
                thisWeek = true;
            } else if (list.period == TopRankedList.LAST_WEEK) {
                if (!lastWeek) {
                    pages++;
                }
                lastWeek = true;
            }
        }

        _pages = new int[pages];
        int idx = 0;
        if (thisWeek) {
            _pages[idx++] = TopRankedList.THIS_WEEK;
        }
        if (lastWeek) {
            _pages[idx++] = TopRankedList.LAST_WEEK;
        }
        _pages[idx] = TopRankedList.LIFETIME;
        showList(_pages[_page]);
        if (_pages.length == 1) {
            _right.setEnabled(false);
        }
    }

    protected void displayPage (int page)
    {
        if (_page == page || page < 0 || page >= _pages.length) {
            return;
        }

        _page = page;
        _left.setEnabled(_page != 0);
        _right.setEnabled(_page != _pages.length - 1);
        showList(_pages[_page]);
    }

    protected void showList (int period)
    {
        _listCont.removeAll();

        _title.setText(_msgs.get("m.top10_title_" + period));

        for (TopRankedList list : _rankobj.getTopRanked()) {
            if (list.criterion.indexOf(ScenarioInfo.OVERALL_IDENT) > -1 &&
                    list.players.length > 0 && list.period == period) {
                addScenario(_listCont, list);
                break;
            }
        }
        for (TopRankedList list : _rankobj.getTopRanked()) {
            if (list.criterion.indexOf(ScenarioInfo.OVERALL_IDENT) == -1 &&
                    list.players.length > 0 && list.period == period) {
                addScenario(_listCont, list);
            }
        }
    }

    protected String getHeaderText ()
    {
        return _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.top_scores");
    }

    protected void addScenario (BContainer cont, TopRankedList list)
    {
        cont.add(new Spacer(10, 5));

        BContainer row = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        ((GroupLayout)row.getLayoutManager()).setGap(25);
        ((GroupLayout)row.getLayoutManager()).setOffAxisJustification(
            GroupLayout.BOTTOM);
        cont.add(row);

        BContainer col = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER));
        ((GroupLayout)col.getLayoutManager()).setGap(0);
        row.add(col);

        String cat = _ctx.xlate(SaloonCodes.SALOON_MSGS,
                MessageBundle.qualify(GameCodes.GAME_MSGS, "m.scenario_" + list.criterion));
        col.add(new BLabel(cat, "top_score_category"));

        AvatarView aview = new AvatarView(_ctx, 4, false, true);
        col.add(aview, GroupLayout.FIXED);
        if (list.players.length > 0) {
            aview.setHandle(list.players[0], "1. " + list.players[0]);
        }
        if (list.topDogSnapshot != null) {
            aview.setAvatar(list.topDogSnapshot);
        }

        col = new BContainer(GroupLayout.makeVStretch());
        ((GroupLayout)col.getLayoutManager()).setGap(0);
        row.add(col);
        for (int ii = 1; ii < list.players.length; ii++) {
            col.add(new PlayerLabel(ii + 1, list.players[ii]));
        }
    }

    protected class PlayerLabel extends BLabel
    {
        public PlayerLabel (int rank, Handle handle) {
            super(rank + ". " + handle, "top_score_list");
            _handle = handle;
        }

        @Override // from BComponent
        public boolean dispatchEvent (BEvent event)
        {
            // pop up a player menu if they click the mouse
            return PlayerPopupMenu.checkPopup(
                _ctx, getWindow(), event, _handle, false) ||
                super.dispatchEvent(event);
        }

        protected Handle _handle;
    }

    /** Listens for navigation button presses. */
    protected ActionListener _navigator = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            String action = event.getAction();
            if (action.equals("right")) {
                displayPage(_page + 1);
            } else if (action.equals("left")) {
                displayPage(_page - 1);
            }
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected int _page;
    protected int[] _pages;
    protected TopRankObject _rankobj;
    protected BButton _left, _right;
    protected BLabel _title;
    protected BContainer _listCont;
}
