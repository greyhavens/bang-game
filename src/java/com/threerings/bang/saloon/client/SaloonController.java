//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Manages the client side of the Saloon.
 */
public class SaloonController extends PlaceController
{
    /**
     * Called by the {@link CriterionView} when the player requests to find a
     * game.
     */
    public void findMatch (Criterion criterion)
    {
        SaloonService.ResultListener rl = new SaloonService.ResultListener() {
            public void requestProcessed (Object result) {
                _view.displayMatchView((Integer)result);
            }
            public void requestFailed (String reason) {
                _view.setStatus(reason);
            }
        };
        _salobj.service.findMatch(_ctx.getClient(), criterion, rl);
    }

    /**
     * Called by the {@link MatchView} if the player requests to cancel their
     * pending match.
     */
    public void leaveMatch (int matchOid)
    {
        if (matchOid == -1) {
            // there was an error
            _view.clearMatchView(SaloonCodes.INTERNAL_ERROR);
        } else {
            // the user clicked cancel
            _salobj.service.leaveMatch(_ctx.getClient(), matchOid);
            _view.clearMatchView(null);
        }
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _salobj = (SaloonObject)plobj;
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_view = new SaloonView((BangContext)ctx, this));
    }

    protected SaloonView _view;
    protected SaloonObject _salobj;
}
