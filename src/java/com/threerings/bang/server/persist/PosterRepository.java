//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.jora.Table;
import com.samskivert.io.PersistenceException;

/**
 * Manages persistent per-player poster information.
 */
@Singleton
public class PosterRepository extends JORARepository
{
    /** The database identifier used when establishing a database
     * connection. This value being <code>posterdb</code>. */
    public static final String POSTER_DB_IDENT = "posterdb";

    /**
     * Constructs a new poster repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    @Inject public PosterRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, PosterRepository.POSTER_DB_IDENT);
    }

    /**
     * Loads up the poster record associated with the specified player.
     * Returns null if no matching record could be found.
     */
    public PosterRecord loadPoster (int playerId)
        throws PersistenceException
    {
        return load(_ptable, "where PLAYER_ID = " + playerId);
    }

    /**
     * Stores a poster record to the repository.
     */
    public void storePoster (PosterRecord record)
        throws PersistenceException
    {
        store(_ptable, record);
    }

    /**
     * Deletes the specified poster from the repository.
     */
    public void deletePoster (PosterRecord record)
        throws PersistenceException
    {
        delete(_ptable, record);
    }

    /**
     * Deletes the poster for the specified playerId from the repository
     */
    public void deletePoster (int playerId)
        throws PersistenceException
    {
        update("delete from POSTERS where PLAYER_ID = " + playerId);
    }

    @Override // from SimpleRepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "POSTERS", new String[] {
            "PLAYER_ID INTEGER UNSIGNED NOT NULL",
            "STATEMENT VARCHAR(255)",
            "BADGE1 INTEGER NOT NULL",
            "BADGE2 INTEGER NOT NULL",
            "BADGE3 INTEGER NOT NULL",
            "BADGE4 INTEGER NOT NULL",
            "PRIMARY KEY (PLAYER_ID)",
        }, "");
    }

    @Override // from JORARepository
    protected void createTables ()
    {
        _ptable = new Table<PosterRecord>(
            PosterRecord.class, "POSTERS", "PLAYER_ID", true);
    }

    protected Table<PosterRecord> _ptable;
}
