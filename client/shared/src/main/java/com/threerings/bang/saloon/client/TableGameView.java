//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Rectangle;

import com.threerings.bang.client.bui.StatusLabel;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.TableGameObject;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;

import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public class TableGameView extends BContainer
    implements AttributeChangeListener
{
    public TableGameView (BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());
        _ctx = ctx;
        _gconfig = new ParlorGameConfigView(_ctx, _status, this);
    }

    /**
     * Called by the controller to instruct us to display the pending match
     * view when we have requested to play a game.
     */
    public void displayMatchView ()
    {
        // remove our configuration view
        if (_gconfig.isAdded()) {
            remove(_gconfig);
        }

        // this should never happen, but just to be ultra-robust
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // display a match view for this pending match
        add(_mview = new ParlorMatchView(_ctx, _tobj), CONFIG_RECT);
    }

    /**
     * Called by the match view if the player cancels their pending match.
     * Redisplays the criterion view.
     */
    public void clearMatchView ()
    {
        // out with the old match view
        if (_mview != null) {
            remove(_mview);
            _mview = null;
        }

        // redisplay the criterion view
        if (!_gconfig.isAdded()) {
            add(_gconfig, CONFIG_RECT);
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(TableGameObject.PLAYER_OIDS)) {
            if (_tobj.playerOids == null) {
                clearMatchView();
            } else {
                displayMatchView();
            }
        }
    }

    /**
     * Called to subscribe to the TableGameObject.
     */
    public void willEnterPlace (int tableOid)
    {
        (_tablesub = new SafeSubscriber<TableGameObject>(
                tableOid, new Subscriber<TableGameObject>() {
            public void objectAvailable (TableGameObject tobj) {
                _tobj = tobj;
                _tobj.addListener(TableGameView.this);
                _gconfig.willEnterPlace(_tobj);

                // show the match view if there's a game already in progress
                if (_tobj.playerOids != null) {
                    displayMatchView();
                } else {
                    clearMatchView();
                }
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to subscribe to table game object", "oid", oid, "cause", cause);
            }
        })).subscribe(_ctx.getDObjectManager());

    }

    /**
     * Called before removing the view to unsubscribe from the TableGameObject.
     */
    public void didLeavePlace ()
    {
        _gconfig.didLeavePlace();

        if (_tobj != null) {
            _tobj.removeListener(this);
            _tobj = null;
        }
        if (_tablesub != null) {
            _tablesub.unsubscribe(_ctx.getDObjectManager());
            _tablesub = null;
        }
    }

    /**
     * This will be overriden to allow control over who can configure/create table games.
     */
    public boolean canCreate ()
    {
        return true;
    }

    /**
     * Called to force an visual update of the table game configuration.
     */
    public void updateDisplay ()
    {
        if (_gconfig.isAdded()) {
            _gconfig.updateDisplay();
        }
    }

    protected TableGameObject _tobj;
    protected SafeSubscriber<TableGameObject> _tablesub;
    protected BangContext _ctx;
    protected StatusLabel _status;

    protected ParlorGameConfigView _gconfig;
    protected ParlorMatchView _mview;

    protected static final Rectangle CONFIG_RECT = new Rectangle(0, 0, 440, 252);
}
