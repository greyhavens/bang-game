//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.util.MessageBundle;
import com.threerings.presents.util.SafeSubscriber;

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
        super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                   GroupLayout.STRETCH));
        _ctx = ctx;
        _ctrl = ctrl;
        _msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _msub = new SafeSubscriber(matchOid, this);
        _msub.subscribe(_ctx.getDObjectManager());
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
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

        // create our player rows
        _rows = new PlayerRow[_mobj.playerOids.length];
        for (int ii = 0; ii < _rows.length; ii++) {
            add(_rows[ii] = new PlayerRow());
        }

        add(new BButton(_msgs.get("m.cancel"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctrl.leaveMatch(_mobj.getOid());
            }
        }, "cancel"));
        updateDisplay();
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
            _rows[ii].setPlayerOid(_mobj.playerOids[ii]);
        }
    }

    protected ElementUpdateListener _elup = new ElementUpdateListener() {
        public void elementUpdated (ElementUpdatedEvent event) {
            updateDisplay();
        }
    };

    protected class PlayerRow extends BContainer
    {
        public PlayerRow ()
        {
            super(new BorderLayout(5, 5));
            add(_icon = new BLabel(""), BorderLayout.WEST);;
            add(_name = new BLabel(_msgs.get("m.waiting_for_player")),
                BorderLayout.NORTH);
            add(_info = new BLabel("..."), BorderLayout.CENTER);
        }

        public void setPlayerOid (int playerOid)
        {
            if (playerOid <= 0) {
                _name.setText(_msgs.get("m.waiting_for_player"));
                _icon.setIcon(null);
                return;
            }

            BangOccupantInfo boi = (BangOccupantInfo)
                _ctx.getOccupantDirector().getOccupantInfo(playerOid);
            if (boi == null) {
                log.warning("Missing occupant info for player " +
                            "[oid=" + playerOid + "].");
                _name.setText(_msgs.xlate(SaloonCodes.INTERNAL_ERROR));
            } else {
                _name.setText(boi.username.toString());
                // TODO: set avatar icon
            }
        }

        protected BLabel _icon, _name, _info;
    }

    protected BangContext _ctx;
    protected SaloonController _ctrl;
    protected MessageBundle _msgs;
    protected SafeSubscriber _msub;
    protected MatchObject _mobj;
    protected PlayerRow[] _rows;
}
