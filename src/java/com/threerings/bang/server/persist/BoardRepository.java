//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.FieldMask;
import com.samskivert.jdbc.jora.Session;
import com.samskivert.jdbc.jora.Table;

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
     * Loads all boards in the whole database.
     */
    public BoardList loadBoards ()
        throws PersistenceException
    {
        return (BoardList)execute(new Operation() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                return (BoardList)_btable.select("").toArrayList();
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
        BoardRecord orecord = (BoardRecord)loadByExample(_btable, mask, record);
        if (orecord != null) {
            record.boardId = orecord.boardId;
            update(_btable, record);
        } else {
            record.boardId = insert(_btable, record);
        }
    }

    @Override // documentation inherited
    protected void createTables (Session session)
    {
	_btable = new Table(BoardRecord.class.getName(), "BOARDS",
                            session, "BOARD_ID", true);
    }

    protected Table _btable;
}
