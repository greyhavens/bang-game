//
// $Id$

package com.threerings.bang.server.persist;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;

/**
 * Responsible for the persistent storage of per-player statistics.
 */
public class StatRepository extends com.threerings.stats.server.persist.StatRepository
{
    /**
     * Constructs a new statistics repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our database connection.
     */
    public StatRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov);
    }
}
