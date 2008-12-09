//
// $Id$

package com.threerings.bang.tourney.server;

import com.threerings.util.Name;

import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.tourney.data.TourneyConfig;
import com.threerings.parlor.tourney.server.TourneyManager;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.tourney.data.TourneyListingEntry;
import com.threerings.bang.tourney.data.BangTourneyConfig;

/**
 * Manages running an individual tournament.
 */
public class BangTourneyManager extends TourneyManager
{
    @Override // from TourneyManager
    public int init (TourneyConfig config, Comparable<?> key)
    {
        int tournOid = super.init(config, key);

        // keep track of our start time in millis
        _startTime = System.currentTimeMillis() + (MINUTE * _config.startsIn);

        // add us to the list of pending tournies
        TourneyListingEntry entry = new TourneyListingEntry(
            ((BangTourneyConfig)config).desc, key, _trobj.getOid(), config.startsIn);
        ((BangTourniesManager)_tmgr).getTourniesObject().addToTournies(entry);

        return tournOid;
    }

    // documentation inherited
    public void notifyAllParticipants (String msg)
    {
    }

    // documentation inherited
    protected BodyObject lookupBody (Name username)
    {
        return BangServer.locator.lookupBody(username);
    }

    // documentation inherited
    protected void joinTourney (BodyObject body)
        throws InvocationException
    {

    }
}
