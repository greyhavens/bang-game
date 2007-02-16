//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.jme.chat.ChatView;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.chat.client.BangChatDirector;

import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Displays a pending matched game and handles the process of entering the game
 * when all is ready to roll.
 */
public abstract class MatchView extends BContainer
    implements Subscriber<MatchObject>
{
    public MatchView (BangContext ctx, int matchOid, boolean showChat)
    {
        super(GroupLayout.makeVStretch());
        setStyleClass("match_view");

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _msub = new SafeSubscriber<MatchObject>(matchOid, this);
        _msub.subscribe(_ctx.getDObjectManager());

        // this will contain the players and game info
        BContainer main = new BContainer(GroupLayout.makeHStretch());
        main.add(_left = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_left.getLayoutManager()).setGap(0);
        main.add(_info = GroupLayout.makeVBox(GroupLayout.CENTER),
                 GroupLayout.FIXED);
        main.add(_right = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_info.getLayoutManager()).setGap(3);
        ((GroupLayout)_right.getLayoutManager()).setGap(0);
        main.setPreferredSize(new Dimension(395, 224));
        add(main, GroupLayout.FIXED);

        // this will contain our current criterion
        _info.add(_rounds = new BLabel("", "match_label"));
        _info.add(_players = new BLabel("", "match_label"));
        _info.add(_ranked = new BLabel("", "match_label"));
        _info.add(_range = new BLabel("", "match_label"));
        _info.add(_opponents = new BLabel("", "match_label"));
        _info.add(_prevscen = new BLabel("", "match_label"));
        _info.add(_starting = new BLabel("", "starting_label"));

        // add our leave button
        BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_bye = new BButton(_msgs.get("m.leave"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _bye.setEnabled(false);
                leaveMatch(_mobj.getOid());
            }
        }, "leave"));
        add(row, GroupLayout.FIXED);

        // cut it short if we're not showing the chat display
        if (!showChat) {
            return;
        }

        // add a label that will overlay the "Back Parlors" text (it also has
        // custom spacing that positions everything properly)
        ImageIcon icon = new ImageIcon(
            _ctx.loadImage("ui/saloon/matched_game_chat.png"));
        add(new BLabel(icon, "match_chat_header"), GroupLayout.FIXED);

        // this will eventually be the chat view
        _chat = new ChatView(_ctx, _ctx.getChatDirector()) {
            /* ChatView() */ {
                _text.setStyleClass("match_chat_text");
                _input.setStyleClass("match_chat_input");
            }
            protected boolean handlesType (String localType) {
                return "match_chat".equals(localType);
            }
            protected void wasRemoved () {
                super.wasRemoved();
                // save halted message for the game
                ((BangChatDirector)_ctx.getChatDirector()).setHaltedMessage(
                    _input.getText());
            }
        };
        ((BorderLayout)_chat.getLayoutManager()).setGaps(2, 0);
        _chat.setEnabled(false);
        icon = new ImageIcon(_ctx.loadImage("ui/chat/bubble_icon.png"));
        BButton chat = new BButton(icon, "");
        chat.setStyleClass("arrow_button");
        _chat.setChatButton(chat);
        _chat.setStyleClass("match_chat");
        add(_chat);
    }

    // documentation inherited from interface Subscriber
    public void objectAvailable (MatchObject object)
    {
        _mobj = object;
        _mobj.addListener(_elup);
        _mobj.addListener(_atch);

        // create our player slots
        _slots = new PlayerSlot[_mobj.playerOids.length];
        for (int ii = 0; ii < _slots.length; ii++) {
            if (ii % 2 == 0) {
                _left.add(_slots[ii] = new PlayerSlot(_ctx));
            } else {
                _right.add(_slots[ii] = new PlayerSlot(_ctx));
            }
        }

        updateDisplay();
        updateCriterion();
        updateStarting();

        if (_chat == null) {
            return;
        }
        _ctx.getChatDirector().addAuxiliarySource(_mobj, "match_chat");
        _chat.setSpeakService(_mobj.speakService, "match_chat");
        _chat.setEnabled(true);
        _chat.requestFocus();
        _ctx.getChatDirector().displayInfo(
            SaloonCodes.SALOON_MSGS, "m.chat_here", "match_chat");
    }

    // documentation inherited from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        log.warning("Failed to subscribe to match object " +
                    "[oid=" + oid + ", cause=" + cause + "].");
        leaveMatch(-1);
    }

    /**
     * Leaves the specified match.
     */
    protected abstract void leaveMatch (int matchOid);

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _msub.unsubscribe(_ctx.getDObjectManager());
        if (_chat != null && _mobj != null) {
            _ctx.getChatDirector().removeAuxiliarySource(_mobj);
            _chat.clearSpeakService();
        }
    }

    protected void updateDisplay ()
    {
        for (int ii = 0; ii < _mobj.playerOids.length; ii++) {
            _slots[ii].setPlayerOid(_mobj.playerOids[ii]);
        }
    }

    protected void updateCriterion ()
    {
        String value = _mobj.criterion.getPlayerString();
        _players.setText(_msgs.get("m.cr_players", value));
        value = _mobj.criterion.getRoundString();
        _rounds.setText(_msgs.get("m.cr_rounds", value));
        updateRanked();
        value = _mobj.criterion.getAIString();
        _opponents.setText(_msgs.get("m.cr_aiopps", value));
        value = "m." + CriterionView.RANGE[_mobj.criterion.range];
        _range.setText(_msgs.get(value));
        _prevscen.setText(_mobj.criterion.allowPreviousTowns ?
                          _msgs.get("m.cr_allscens") : "");
    }

    protected void updateRanked ()
    {
        _ranked.setText(_msgs.get(_mobj.criterion.getDesiredRankedness() ?
                                  "m.ranked" : "m.unranked"));
    }

    protected void updateStarting ()
    {
        _starting.setText(_mobj.starting ? _msgs.get("m.starting") : "");
    }

    protected AttributeChangeListener _atch = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(MatchObject.STARTING)) {
                updateStarting();
            } else {
                updateCriterion();
            }
        }
    };

    protected ElementUpdateListener _elup = new ElementUpdateListener() {
        public void elementUpdated (ElementUpdatedEvent event) {
            updateDisplay();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected SafeSubscriber<MatchObject> _msub;
    protected MatchObject _mobj;

    protected BLabel _players, _rounds, _ranked, _range, _prevscen;
    protected BLabel _opponents, _starting;
    protected BButton _bye;

    protected BContainer _left, _right, _info;
    protected PlayerSlot[] _slots;

    protected ChatView _chat;
}
