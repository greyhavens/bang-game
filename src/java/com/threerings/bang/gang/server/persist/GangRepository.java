//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.*;

/**
 * Persistifies gang information.
 */
public class GangRepository extends JORARepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>gangdb</code>.
     */
    public static final String GANG_DB_IDENT = "gangdb";
    
    /** Contains information loaded from the database about a gang. */
    public static class GangRecord
    {
        /** The gang's unique identifier. */
        public int gangId;
    
        /** The name of the gang. */
        public String name;

        /** The date upon which the gang was founded. */
        public Date founded;

        /** The amount of scrip in the gang's coffers. */
        public int scrip;
        
        /** The number of coins in the gang's coffers. */
        public int coins;
        
        /** The encoded brand. */
        public byte[] brand;
        
        /** The encoded outfit. */
        public byte[] outfit;
        
        /** Used when creating new gangs. */
        public GangRecord (String name)
        {
            this.name = name;
        }

        /** Used when forming queries. */
        public GangRecord (int gangId)
        {
            this.gangId = gangId;
        }
        
        /** Used when loading records from the database. */
        public GangRecord ()
        {
        }
        
        /** Returns a string representation of this instance. */
        public String toString ()
        {
            return "[gangId=" + gangId + ", name=" + name + ", founded=" +
                founded + ", scrip=" + scrip + ", coins=" + coins + "]";
        }
    }

    /**
     * Constructs a new gang repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public GangRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, GANG_DB_IDENT);
        _byIdMask = _gtable.getFieldMask();
        _byIdMask.setModified("gangId");
    }
    
    /**
     * Loads up the gang record associated with the specified id.
     * Returns null if no matching record could be found.
     */
    public GangRecord loadGang (int gangId)
        throws PersistenceException
    {
        return loadByExample(_gtable, new GangRecord(gangId), _byIdMask);
    }
    
    /**
     * Insert a new gang record into the repository and assigns a unique
     * gang id in the process. The {@link GangRecord#founded} field will be
     * filled in by this method if it is not already.
     */
    public void insertGang (GangRecord gang)
        throws PersistenceException
    {
        if (gang.founded == null) {
            gang.founded = new Date(System.currentTimeMillis());
        }
        gang.gangId = insert(_gtable, gang);
    }
    
    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "GANGS", new String[] {
            "GANG_ID INTEGER NOT NULL AUTO_INCREMENT",
            "NAME VARCHAR(64) NOT NULL",
            "FOUNDED DATETIME NOT NULL",
            "SCRIP INTEGER NOT NULL",
            "COINS INTEGER NOT NULL",
            "BRAND BLOB NOT NULL",
            "OUTFIT BLOB NOT NULL",
            "PRIMARY KEY (GANG_ID)",
            "UNIQUE (NAME)",
        }, "");
    }
    
    @Override // documentation inherited
    protected void createTables ()
    {
	    _gtable = new Table<GangRecord>(GangRecord.class,
	        "GANGS", "GANG_ID", true);
    }
    
    protected Table<GangRecord> _gtable;
    protected FieldMask _byIdMask;
}
