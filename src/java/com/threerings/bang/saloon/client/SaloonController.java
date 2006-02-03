//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.presents.client.InvocationService;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.util.Name;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

import static com.threerings.bang.Log.log;

/**
 * Manages the client side of the Saloon.
 */
public class SaloonController extends PlaceController
    implements ActionListener
{
    /** Used to start a one player test game. */
    public static final String TEST_GAME = "test_game";

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

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getAction().equals(TEST_GAME)) {
            BangConfig config = new BangConfig();
            config.players = new Name[] {
                _ctx.getUserObject().getVisibleName(),
                new Name("Larry"), new Name("Moe") };
            config.ais = new GameAI[] {
                null, new GameAI(1, 50), new GameAI(0, 50) };
            config.scenarios = new String[] { "cj" };
            config.teamSize = 3;
            InvocationService.ConfirmListener cl =
                new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                }
                public void requestFailed (String reason) {
                    log.warning("Failed to create game: " + reason);
                }
            };
            _ctx.getParlorDirector().startSolitaire(config, cl);
        }
    }

    @Override // documentation inherited
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (BangContext)ctx;
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

    protected BangContext _ctx;
    protected SaloonView _view;
    protected SaloonObject _salobj;
}
