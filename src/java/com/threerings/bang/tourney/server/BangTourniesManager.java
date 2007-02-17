//
// $Id$

package com.threerings.bang.tourney.server;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.RunQueue;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.tourney.data.TourniesObject;

import com.threerings.parlor.tourney.server.TourneyManager;
import com.threerings.parlor.tourney.server.TourniesManager;
import com.threerings.parlor.tourney.server.TourniesDispatcher;
import com.threerings.parlor.tourney.data.TourneyConfig;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

/**
 * Manages all tournaments running on a bang server.
 */
public class BangTourniesManager extends TourniesManager
    implements BangCodes
{
    public static final String TOURNEY_DB_IDENT = "tourneydb";

    public BangTourniesManager (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }

    @Override // documentation inherited
    public void init ()
    {
        // create the distributed object that holds the active tournies
        _tobj = BangServer.omgr.registerObject(new TourniesObject());
        BangServer.invmgr.registerDispatcher(new TourniesDispatcher(this), GLOBAL_GROUP);
        BangServer.registerShutdowner(this);

        super.init();
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
