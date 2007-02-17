//
// $Id$

package com.threerings.bang.tourney.server;

import com.threerings.util.Name;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.tourney.data.TourneyListingEntry;
import com.threerings.bang.tourney.data.BangTourneyConfig;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.tourney.data.TourneyConfig;
import com.threerings.parlor.tourney.server.TourneyManager;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;

/**
 * Manages running an individual tournament.
 */
public class BangTourneyManager extends TourneyManager
{
    public BangTourneyManager (TourneyConfig config, BangTourniesManager tmgr,
            Comparable key, InvocationService.ResultListener listener)
    {
        super(config, tmgr, key, listener);

        // keep track of our start time in millis
        _startTime = System.currentTimeMillis() + (MINUTE * _config.startsIn);

        // add us to the list of pending tournies
        TourneyListingEntry entry = new TourneyListingEntry(
                ((BangTourneyConfig)config).desc, key, _trobj.getOid(), config.startsIn);
        tmgr.getTourniesObject().addToTournies(entry);
    }

    // documentation inherited
    public void notifyAllParticipants (String msg)
    {
    }

    // documentation inherited
    protected PresentsDObjectMgr getOMgr ()
    {
        return BangServer.omgr;
    }

    // documentation inherited
    protected InvocationManager getInvMgr ()
    {
        return BangServer.invmgr;
    }

    // documentation inherited
    protected BodyObject lookupBody (Name username)
    {
        return BangServer.lookupBody(username);
    }

    // documentation inherited
    protected void joinTourney (BodyObject body)
        throws InvocationException
    {

    }
}
