//
// $Id$

package com.threerings.bang.bounty.server.persist;

import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.clause.Where;

/**
 * Tracks recent bounty completers.
 */
public class BountyRepository extends DepotRepository
{
    public BountyRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Loads all recent completers records for all bounties for the specified town.
     */
    public Collection<RecentCompletersRecord> loadCompleters (String townId)
        throws PersistenceException
    {
        return findAll(RecentCompletersRecord.class,
                       new Where(RecentCompletersRecord.TOWN_ID_C, townId));
    }

    /**
     * Writes an updated recent completers record to the database. It need not have previously
     * existed.
     */
    public void storeCompleters (RecentCompletersRecord record)
        throws PersistenceException
    {
        store(record);
    }
}
