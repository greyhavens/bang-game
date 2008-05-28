//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Cursor;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Table;

import static com.threerings.bang.Log.*;

/**
 * Handles the loading and management of our persistent board data.
 */
public class BoardRepository extends JORARepository
{
    /** Type definition! */
    public static class BoardList extends ArrayList<BoardRecord>
    {
    }

    /** The database identifier used when establishing a database
     * connection. This value being <code>boarddb</code>. */
    public static final String BOARD_DB_IDENT = "boarddb";

    /**
     * Constructs a new board repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public BoardRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, BOARD_DB_IDENT);
    }

    /**
     * Loads all boards in the whole database without their board data.
     */
    public BoardList loadBoards ()
        throws PersistenceException
    {
        final BoardList blist = new BoardList();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(
                        "select BOARD_ID, NAME, CREATOR, SCENARIOS, " +
                        "PLAYERS, FLAGS from BOARDS");
                    while (rs.next()) {
                        BoardRecord brec = new BoardRecord();
                        brec.boardId = rs.getInt(1);
                        brec.name = rs.getString(2);
                        brec.creator = rs.getString(3);
                        brec.scenarios = rs.getString(4);
                        brec.players = rs.getInt(5);
                        brec.flags = rs.getInt(6);
                        blist.add(brec);
                    }
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return blist;
    }

    /**
     * Loads the board data and data hash for the given board.
     */
    public void loadBoardData (final BoardRecord brec)
        throws PersistenceException
    {
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "select DATA, DATA_HASH from BOARDS where BOARD_ID = ?");
                try {
                    stmt.setInt(1, brec.boardId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        brec.data = (byte[])rs.getObject(1);
                        brec.dataHash = (byte[])rs.getObject(2);
                        
                    } else {
                        log.warning("Couldn't find board data! [brec=" + brec +
                            "].");
                    }
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }
    
    /**
     * Inserts a new board into the repository for the first time. If the
     * board's name conflicts with an existing board an exception is
     * thrown.
     */
    public void createBoard (BoardRecord record)
        throws PersistenceException
    {
        record.boardId = insert(_btable, record);
    }

    /**
     * Updates the supplied board if another board exists with the same
     * name, otherwise inserts the record as a new board.
     */
    public void storeBoard (BoardRecord record)
        throws PersistenceException
    {
        FieldMask mask = _btable.getFieldMask();
        mask.setModified("name");
        mask.setModified("players");
        BoardRecord orecord = loadByExample(_btable, record, mask);
        if (orecord != null) {
            record.boardId = orecord.boardId;
            update(_btable, record);
        } else {
            record.boardId = insert(_btable, record);
        }
    }

    /**
     * Wipes all non-user-created boards that don't fall in the specified
     * set. Used to prune obsolete boards after reloading the boards from the
     * source files.
     *
     * @return the number of pruned boards.
     */
    public int clearStaleBoards (ArrayIntSet except)
        throws PersistenceException
    {
        return update("delete from BOARDS where CREATOR is NULL and BOARD_ID not in " + except);
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "BOARDS", new String[] {
            "BOARD_ID INTEGER NOT NULL AUTO_INCREMENT",
            "NAME VARCHAR(255) NOT NULL",
            "CREATOR VARCHAR(255)",
            "SCENARIOS VARCHAR(255)",
            "PLAYERS INTEGER NOT NULL",
            "FLAGS INTEGER NOT NULL",
            "DATA MEDIUMBLOB NOT NULL",
            "DATA_HASH BLOB NOT NULL",
            "PRIMARY KEY (BOARD_ID)",
            "UNIQUE(NAME,PLAYERS)",
            "KEY(CREATOR)",
        }, "");

        // TEMP: co-opt the unused PLAYS column as FLAGS
        if (JDBCUtil.tableContainsColumn(conn, "BOARDS", "PLAYS")) {
            JDBCUtil.changeColumn(conn, "BOARDS", "PLAYS", "FLAGS INTEGER NOT NULL");
        }
        // END TEMP
    }

    @Override // documentation inherited
    protected void createTables ()
    {
	_btable = new Table<BoardRecord>(
            BoardRecord.class, "BOARDS", "BOARD_ID", true);
    }

    protected Table<BoardRecord> _btable;
}
