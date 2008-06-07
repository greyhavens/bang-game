//
// $Id$

package com.threerings.bang.tourney.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.RunQueue;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.parlor.tourney.server.TourneyManager;
import com.threerings.parlor.tourney.server.TourniesManager;
import com.threerings.parlor.tourney.server.TourniesDispatcher;
import com.threerings.parlor.tourney.data.TourneyConfig;

import com.threerings.bang.server.BangServer;
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

    @Inject public BangTourniesManager (ShutdownManager shutmgr)
    {
        super(shutmgr);
    }

    @Override // from TourniesManager
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        // create the distributed object that holds the active tournies
        _tobj = BangServer.omgr.registerObject(new TourniesObject());
        BangServer.invmgr.registerDispatcher(new TourniesDispatcher(this), GLOBAL_GROUP);

        super.init(conprov);
    }

    // documentation inherited
    public void createTourney (ClientObject caller, TourneyConfig config,
            final InvocationService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

        // only admins cna create tournaments
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

    // documentation inherited
    protected TourneyManager makeTourneyManager(
            TourneyConfig config, Comparable key, InvocationService.ResultListener listener)
    {
        return new BangTourneyManager(config, this, key, listener);
    }

    // documentation inherited
    protected String getDBIdent ()
    {
        return TOURNEY_DB_IDENT;
    }

    // documentation inherited
    protected RunQueue getRunQueue ()
    {
        return BangServer.omgr;
    }

    // documentation inherited
    protected long getIntervalDelay ()
    {
        return TWENTY_SECONDS;
    }

    protected static final long TWENTY_SECONDS = 1000L * 20;

    protected TourniesObject _tobj;
}
