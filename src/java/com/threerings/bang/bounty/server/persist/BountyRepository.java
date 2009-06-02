//
// $Id$

package com.threerings.bang.bounty.server.persist;

import java.util.Collection;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.io.PersistenceException;

/**
 * Tracks recent bounty completers.
 */
@Singleton
public class BountyRepository extends DepotRepository
{
    @Inject public BountyRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads all recent completers records for all bounties for the specified town.
     */
    public Collection<RecentCompletersRecord> loadCompleters (String townId)
        throws PersistenceException
    {
        return findAll(RecentCompletersRecord.class,
                       new Where(RecentCompletersRecord.TOWN_ID, townId));
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(RecentCompletersRecord.class);
    }
}
