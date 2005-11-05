//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Session;
import com.samskivert.jdbc.jora.Table;

import com.threerings.bang.data.Handle;

import static com.threerings.bang.Log.log;

/**
 * Manages persistent information stored on a per-player basis.
 */
public class PlayerRepository extends JORARepository
{
    /** The database identifier used when establishing a database
     * connection. This value being <code>playerdb</code>. */
    public static final String PLAYER_DB_IDENT = "playerdb";

    /**
     * Constructs a new player repository with the specified connection
     * provider.
     *
     * @param conprov the connection provider via which we will obtain our
     * database connection.
     */
    public PlayerRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, PLAYER_DB_IDENT);
    }

    /**
     * Loads up the player record associated with the specified account.
     * Returns null if no matching record could be found.
     */
    public Player loadPlayer (String accountName)
        throws PersistenceException
    {
        return (Player)loadByExample(_ptable, new Player(accountName));
    }

    /**
     * Insert a new player record into the repository and assigns them a
     * unique player id in the process. The {@link Player#created} field
     * will be filled in by this method if it is not already.
     */
    public void insertPlayer (final Player player)
        throws PersistenceException
    {
        if (player.created == null) {
            player.created = new Date(System.currentTimeMillis());
            player.lastSession = player.created;
        }
        insert(_ptable, player);
    }

    /**
     * Configures a player's handle, and gender.
     */
    public void configurePlayer (int playerId, Handle handle, boolean isMale)
        throws PersistenceException
    {
        String gensql = isMale ?
            "| " + Player.IS_MALE_FLAG : "& " + ~Player.IS_MALE_FLAG;
        checkedUpdate("update PLAYERS set FLAGS = FLAGS " + gensql +
                      ", HANDLE = " + JDBCUtil.escape(handle.toString()) +
                      " where PLAYER_ID = " + playerId, 1);
    }

    /**
     * Deletes the specified player from the repository.
     */
    public void deletePlayer (final Player player)
        throws PersistenceException
    {
        delete(_ptable, player);
    }

    /**
     * Deducts the specified amount of scrip from the specified player's
     * account.
     */
    public void spendScrip (int playerId, int amount)
        throws PersistenceException
    {
        updateScrip("PLAYER_ID = " + playerId, amount, "spend");
    }

    /**
     * Adds the specified amount of scrip to the specified player's
     * account.
     */
    public void grantScrip (int playerId, int amount)
        throws PersistenceException
    {
        updateScrip("PLAYER_ID = " + playerId, amount, "grant");
    }

    /**
     * <em>Do not use this method!</em> It exists only because we must work
     * with the coin system which tracks players by name rather than id.
     */
    public void grantScrip (String accountName, int amount)
        throws PersistenceException
    {
        updateScrip("ACCOUNT_NAME = " + JDBCUtil.escape(accountName),
                    amount, "grant");
    }

    /**
     * Mimics the disabling of deleted players by renaming them to an
     * invalid value that we do in our user management system. This is
     * triggered by us receiving a player action indicating that the
     * player was deleted.
     */
    public void disablePlayer (String accountName, String disabledName)
        throws PersistenceException
    {
        if (update("update PLAYERS set ACCOUNT_NAME = " +
                   JDBCUtil.escape(disabledName) + " where ACCOUNT_NAME = " +
                   JDBCUtil.escape(accountName)) == 1) {
            log.info("Disabled deleted player [oname=" + accountName +
                     ", dname=" + disabledName + "].");
        }
    }

    /**
     * Note that a user's session has ended: increment their sessions, add in
     * the number of minutes spent online, set their last session time to now
     * and remember their most recently selected avatar look.
     */
    public void noteSessionEnded (int playerId, String look, int minutes)
        throws PersistenceException
    {
        checkedUpdate("update PLAYERS set SESSIONS = SESSIONS + 1, " +
                      "SESSION_MINUTES = SESSION_MINUTES + " + minutes + ", " +
                      "LOOK = " + JDBCUtil.escape(look) + ", " +
                      "LAST_SESSION = NOW() where PLAYER_ID=" + playerId, 1);
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.addColumn(
            conn, "PLAYERS", "HANDLE", "VARCHAR(64) NOT NULL", "ACCOUNT_NAME");
        JDBCUtil.addColumn(
            conn, "PLAYERS", "FLAGS", "INTEGER NOT NULL", "LAST_SESSION");
    }

    @Override // documentation inherited
    protected void createTables (Session session)
    {
	_ptable = new Table(Player.class.getName(), "PLAYERS",
                            session, "PLAYER_ID", true);
    }

    /** Helper function for {@link #spendScrip} and {@link #grantScrip}. */
    protected void updateScrip (String where, int amount, String type)
        throws PersistenceException
    {
        if (amount <= 0) {
            throw new PersistenceException(
                "Illegal scrip " + type + " [where=" + where +
                ", amount=" + amount + "]");
        }

        String action = type.equals("grant") ? "+" : "-";
        String query = "update PLAYERS set SCRIP = SCRIP " + action + " " +
            amount + " where " + where;
        int mods = update(query);
        if (mods == 0) {
            throw new PersistenceException(
                "Scrip " + type + " modified zero rows [where=" + where +
                ", amount=" + amount + "]");
        } else if (mods > 1) {
            log.warning("Scrip " + type + " modified multiple rows " +
                        "[where=" + where + ", amount=" + amount +
                        ", mods=" + mods + "].");
        }
    }

    protected Table _ptable;
}
