//
// $Id$

package com.threerings.bang.server.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;

import com.threerings.bang.data.Stat;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import static com.threerings.bang.Log.log;

/**
 * Responsible for the persistent storage of per-player statistics.
 */
public class StatRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database
     * connection. This value being <code>statdb</code>.
     */
    public static final String STAT_DB_IDENT = "statdb";

    /**
     * Constructs a new statistics repository with the specified
     * connection provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public StatRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, STAT_DB_IDENT);
    }

    /**
     * Loads the stats associated with the specified player.
     */
    public ArrayList<Stat> loadStats (final int playerId)
        throws PersistenceException
    {
        final ArrayList<Stat> stats = new ArrayList<Stat>();
        final String query = "select STAT_CODE, STAT_DATA " +
            "from STATS where PLAYER_ID = " + playerId;
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        Stat stat = decodeStat(
                            rs.getInt(1), (byte[])rs.getObject(2));
                        if (stat != null) {
                            stats.add(stat);
                        }
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return stats;
    }

    /**
     * Writes out any of the stats in the supplied array that have been
     * modified since they were first loaded. Exceptions that occur while
     * writing the stats will be caught and logged.
     */
    public void writeModified (int playerId, Stat[] stats)
    {
        for (int ii = 0; ii < stats.length; ii++) {
            try {
                if (stats[ii].isModified()) {
                    updateStat(playerId, stats[ii]);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error flushing modified stat " +
                        "[stat=" + stats[ii] + "].", e);
            }
        }
    }

    /**
     * Instantiates the appropriate stat class and decodes the stat from
     * the data.
     */
    protected Stat decodeStat (int statCode, byte[] data)
    {
        String errmsg = null;
        Exception error = null;

        try {
            Stat.Type type = Stat.getType(statCode);
            if (type == null) {
                log.warning("Unable to decode stat, unknown type " +
                            "[code=" + statCode + "].");
                return null;
            }

            // decode its contents from the serialized data
            Stat stat = type.newStat();
            ByteArrayInputStream bin = new ByteArrayInputStream(data);
            stat.unpersistFrom(new ObjectInputStream(bin));
            return stat;

        } catch (ClassNotFoundException cnfe) {
            error = cnfe;
            errmsg = "Unable to instantiate stat";

        } catch (IOException ioe) {
            error = ioe;
            errmsg = "Unable to decode stat";
        }

        log.log(Level.WARNING, errmsg + " [code=" + statCode + "]", error);
        return null;
    }

    /**
     * Updates the specified stat in the database, inserting it if
     * necessary.
     */
    protected void updateStat (int playerId, final Stat stat)
        throws PersistenceException
    {
        final String uquery = "update STATS set STAT_DATA = ?" +
            " where PLAYER_ID = " + playerId +
            " and STAT_CODE = " + stat.getCode();
        final String iquery = "insert into STATS (PLAYER_ID, STAT_CODE, " +
            "STAT_DATA) values (" + playerId + ", " + stat.getCode() + ", ?)";
        final ByteArrayOutInputStream out = new ByteArrayOutInputStream();
        try {
            stat.persistTo(new ObjectOutputStream(out));
        } catch (IOException ioe) {
            String errmsg = "Error serializing stat " + stat;
            throw new PersistenceException(errmsg, ioe);
        }

        // now update (or insert) the flattened data into the database
        execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(uquery);
                try {
                    stmt.setBinaryStream(1, out.getInputStream(), out.size());
                    if (stmt.executeUpdate() == 0) {
                        JDBCUtil.close(stmt);
                        stmt = conn.prepareStatement(iquery);
                        stmt.setBinaryStream(
                            1, out.getInputStream(), out.size());
                        JDBCUtil.checkedUpdate(stmt, 1);
                    }
                    return null;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }
}
