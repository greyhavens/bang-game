//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.client.GameReadyObserver;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorObject;

/**
 * Handles the client side of a Back Parlor.
 */
public class ParlorController extends PlaceController
    implements GameReadyObserver
{
    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
        _ctx.getParlorDirector().addGameReadyObserver(this);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _parobj = (ParlorObject)plobj;
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);
        _ctx.getParlorDirector().removeGameReadyObserver(this);
    }

    /**
     * Called by the {@link CriterionView} when the player requests to find a matched game.
     */
    public void findSaloonMatch (Criterion criterion)
    {
        ParlorService.ResultListener rl = new ParlorService.ResultListener() {
            public void requestProcessed (Object result) {
                _view.displaySaloonMatchView((Integer)result);
            }
            public void requestFailed (String reason) {
                _view.findSaloonMatchFailed(reason);
            }
        };
        _parobj.service.findSaloonMatch(criterion, rl);
    }

    /**
     * Called by the {@link MatchView} if the player requests to cancel their pending match.
     */
    public void leaveSaloonMatch (int matchOid)
    {
        if (_gameStarting) {
            return;
        }
        if (matchOid != -1) {
            _parobj.service.leaveSaloonMatch(matchOid);
        }
        _view.clearSaloonMatchView(null);
    }

    // documentation inherited from interface GameReadyObserver
    public boolean receivedGameReady (int gameOid)
    {
        _gameStarting = true;
        return false;
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new ParlorView((BangContext)ctx, this));
    }

    protected BangContext _ctx;
    protected ParlorView _view;
    protected ParlorObject _parobj;
    protected boolean _gameStarting;
}
