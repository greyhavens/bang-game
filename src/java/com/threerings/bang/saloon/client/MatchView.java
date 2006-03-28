//
// $Id$

package com.threerings.bang.saloon.client;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.MatchObject;
import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Displays a pending matched game and handles the process of entering the game
 * when all is ready to roll.
 */
public class MatchView extends BContainer
    implements Subscriber
{
    public MatchView (BangContext ctx, SaloonController ctrl, int matchOid)
    {
        super(new BorderLayout(5, 10));
        setStyleClass("match_view");

        _ctx = ctx;
        _ctrl = ctrl;
        _msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _msub = new SafeSubscriber(matchOid, this);
        _msub.subscribe(_ctx.getDObjectManager());

        // this will contain the players and game info
        BContainer main = new BContainer(GroupLayout.makeHStretch());
        main.add(_left = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_left.getLayoutManager()).setGap(0);
        main.add(_info = GroupLayout.makeVBox(GroupLayout.CENTER),
                 GroupLayout.FIXED);
        main.add(_right = GroupLayout.makeVBox(GroupLayout.CENTER));
        ((GroupLayout)_right.getLayoutManager()).setGap(0);
        add(main, BorderLayout.CENTER);

        // this will contain our current criterion
        _info.add(_rounds = new BLabel("", "match_label"));
        _info.add(_players = new BLabel("", "match_label"));
        _info.add(_ranked = new BLabel("", "match_label"));
        _info.add(_range = new BLabel("", "match_label"));
        _info.add(_starting = new BLabel("", "starting_label"));

        // add our leave button
        BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_bye = new BButton(_msgs.get("m.leave"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _bye.setEnabled(false);
                _ctrl.leaveMatch(_mobj.getOid());
            }
        }, "leave"));
        add(row, BorderLayout.SOUTH);

        // load up some images
        _silhouette = ctx.loadImage("ui/saloon/silhouette.png");
        _playerScroll = ctx.loadImage("ui/frames/tiny_scroll.png");
        _emptyScroll = ctx.loadImage("ui/frames/tall_tiny_scroll.png");
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        _msub.unsubscribe(_ctx.getDObjectManager());
    }

    // documentation inherited from interface Subscriber
    public void objectAvailable (DObject object)
    {
        _mobj = (MatchObject)object;
        _mobj.addListener(_elup);
        _mobj.addListener(_atch);

        // create our player slots
        _slots = new PlayerSlot[_mobj.playerOids.length];
        for (int ii = 0; ii < _slots.length; ii++) {
            if (ii % 2 == 0) {
                _left.add(_slots[ii] = new PlayerSlot());
            } else {
                _right.add(_slots[ii] = new PlayerSlot());
            }
        }

        updateDisplay();
        updateCriterion();
    }

    // documentation inherited from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        log.warning("Failed to subscribe to match object " +
                    "[oid=" + oid + ", cause=" + cause + "].");
        _ctrl.leaveMatch(-1);
    }

    protected void updateDisplay ()
    {
        for (int ii = 0; ii < _mobj.playerOids.length; ii++) {
            _slots[ii].setPlayerOid(_mobj.playerOids[ii]);
        }
        updateStarting();
    }

    protected void updateCriterion ()
    {
        String value = _mobj.criterion.getPlayerString();
        _players.setText(_msgs.get("m.cr_players", value));
        value = _mobj.criterion.getRoundString();
        _rounds.setText(_msgs.get("m.cr_rounds", value));
        _ranked.setText(_msgs.get(_mobj.criterion.getDesiredRankedness() ?
                                  "m.ranked" : "m.unranked"));
        value = "m." + CriterionView.RANGE[_mobj.criterion.range];
        _range.setText(_msgs.get(value));
        updateStarting();
    }

    protected void updateStarting ()
    {
        int filled = 0;
        for (int ii = 0; ii < _mobj.playerOids.length; ii++) {
            if (_mobj.playerOids[ii] > 0) {
                filled++;
            }
        }
        _starting.setText(_mobj.criterion.getDesiredPlayers() == filled ?
                          _msgs.get("m.starting") : "");
    }

    protected AttributeChangeListener _atch = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            updateCriterion();
        }
    };

    protected ElementUpdateListener _elup = new ElementUpdateListener() {
        public void elementUpdated (ElementUpdatedEvent event) {
            updateDisplay();
        }
    };

    protected class PlayerSlot extends BLabel
    {
        public PlayerSlot ()
        {
            super("");
            setStyleClass("match_slot");
        }

        public void setPlayerOid (int playerOid)
        {
            if (playerOid == _playerOid) {
                return;
            }
            _playerOid = playerOid;

            if (playerOid <= 0) {
                setText(_msgs.get("m.waiting_for_player"));
                return;
            }

            BangOccupantInfo boi = (BangOccupantInfo)
                _ctx.getOccupantDirector().getOccupantInfo(playerOid);
            if (boi == null) {
                log.warning("Missing occupant info for player " +
                            "[oid=" + playerOid + "].");
                setText("???");
            } else {
                setText(boi.username.toString());
                _avatar = AvatarView.getImage(_ctx, boi.avatar,
                    AvatarLogic.WIDTH/8, AvatarLogic.HEIGHT/8);
            }
        }

        public ColorRGBA getColor ()
        {
            return _playerOid > 0 ? super.getColor() : GREY_ALPHA;
        }

        protected Dimension computePreferredSize (int whint, int hhint)
        {
            return new Dimension(120, 75);
        }

        protected void renderBackground (Renderer renderer)
        {
            super.renderBackground(renderer);

            BImage icon, scroll;
            int offy = 0;
            if (_playerOid > 0) {
                icon = _avatar;
                scroll = _playerScroll;
            } else {
                icon = _silhouette;
                scroll = _emptyScroll;
                offy = 5;
            }
            int ix = (getWidth() - icon.getWidth())/2;
            int iy = getHeight() - icon.getHeight() - offy;
            icon.render(renderer, ix, iy, 1f);
            scroll.render(renderer, 0, 0, 1f);
        }

        protected int _playerOid = -1;
        protected BImage _avatar;
    }

    protected BangContext _ctx;
    protected SaloonController _ctrl;
    protected MessageBundle _msgs;
    protected SafeSubscriber _msub;
    protected MatchObject _mobj;

    protected BLabel _players, _rounds, _ranked, _range;
    protected BLabel _starting;
    protected BButton _bye;

    protected BImage _silhouette, _playerScroll, _emptyScroll;
    protected BContainer _left, _right, _info;
    protected PlayerSlot[] _slots;

    protected static final ColorRGBA GREY_ALPHA =
        new ColorRGBA(0f, 0f, 0f, 0.25f);
}
