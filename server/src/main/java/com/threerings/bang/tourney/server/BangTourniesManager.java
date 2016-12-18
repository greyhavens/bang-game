//
// $Id$

package com.threerings.bang.tourney.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Lifecycle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.parlor.tourney.data.TourneyConfig;
import com.threerings.parlor.tourney.data.TourniesMarshaller;
import com.threerings.parlor.tourney.server.TourneyManager;
import com.threerings.parlor.tourney.server.TourniesManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.tourney.data.TourniesObject;

/**
 * Manages all tournaments running on a bang server.
 */
@Singleton
public class BangTourniesManager extends TourniesManager
    implements BangCodes
{
    public static final String TOURNEY_DB_IDENT = "tourneydb";

    @Inject public BangTourniesManager (InvocationManager invmgr, Lifecycle cycle)
    {
        super(cycle);
        invmgr.registerProvider(this, TourniesMarshaller.class, GLOBAL_GROUP);
    }

    // documentation inherited
    public void createTourney (ClientObject caller, TourneyConfig config,
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        // only admins can create tournaments
        PlayerObject player = (PlayerObject)caller;
        if (!player.tokens.isAdmin()) {
            throw new InvocationException("m.not_admin");
        }
        super.createTourney(caller, config, listener);
    }

    /**
     * Returns our tournies object.
     */
    public TourniesObject getTourniesObject ()
    {
        return _tobj;
    }

    @Override // from TourniesManager
    public void init ()
    {
        super.init();

        // create the distributed object that holds the active tournies
        _tobj = _omgr.registerObject(new TourniesObject());
    }

    @Override // from TourniesManager
    protected Class<? extends TourneyManager> getTourneyManagerClass ()
    {
        return BangTourneyManager.class;
    }

    @Override // from TourniesManager
    protected long getIntervalDelay ()
    {
        return TWENTY_SECONDS;
    }

    protected TourniesObject _tobj;

    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invmgr;

    protected static final long TWENTY_SECONDS = 1000L * 20;
}
