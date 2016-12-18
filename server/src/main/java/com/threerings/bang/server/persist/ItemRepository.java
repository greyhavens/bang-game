//
// $Id$

package com.threerings.bang.server.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interator;
import com.samskivert.util.StringUtil;
import com.threerings.io.ObjectInputStream;

import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.data.GoldPass;
import com.threerings.bang.data.Item;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ItemFactory;

import static com.threerings.bang.Log.log;

/**
 * Responsible for the persistent storage of items as well as for transferring items from one
 * player to another.
 */
@Singleton
public class ItemRepository extends SimpleRepository
{
    /**
     * The database identifier used when establishing a database connection. This value being
     * <code>itemdb</code>.
     */
    public static final String ITEM_DB_IDENT = "itemdb";

    /**
     * Constructs a new item repository with the specified connection provider.
     *
     * @param conprov the connection provider via which we will obtain our database connection.
     */
    @Inject public ItemRepository (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, ITEM_DB_IDENT);
    }

    /**
     * Loads the items owned by the specified player.
     */
    public List<Item> loadItems (final int playerId)
        throws PersistenceException
    {
        return loadItems(playerId, false);
    }

    /**
     * Loads the items owned by the specified player or gang.
     */
    public List<Item> loadItems (final int ownerId, final boolean gangOwned)
        throws PersistenceException
    {
        final List<Item> items = new ArrayList<Item>();
        final String query = "select ITEM_ID, ITEM_TYPE, ITEM_DATA, GANG_ID, EXPIRES " +
            "from ITEMS where GANG_OWNED = " + gangOwned + " and OWNER_ID = " + ownerId;
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        items.add(decodeItem(rs.getInt(1), rs.getInt(2), gangOwned, ownerId,
                                (byte[])rs.getObject(3), rs.getInt(4), rs.getDate(5)));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return items;
    }

    /**
     * Inserts the specified item into the database. The item's owner id must be valid at the time
     * of insertion, but its item id will be assigned during the insertion process.
     *
     * @return true if the item was successfully inserted, false if the user already owns an
     * equivalent item and the item does not allow duplicates
     */
    public boolean insertItem (final Item item)
        throws PersistenceException
    {
        // determine the prototype's assigned item type and serialize it
        final int itemType = getItemType(item);
        final byte[] itemData = persistItem(item).toByteArray();

        // now insert the flattened data into the database
        return executeUpdate(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                String query = "select count(*) from ITEMS where GANG_OWNED = ? " +
                    "and OWNER_ID = ? and ITEM_TYPE = ? and ITEM_DATA = ?";
                String insert = "insert into ITEMS " +
                    "(GANG_OWNED, OWNER_ID, ITEM_TYPE, ITEM_DATA, GANG_ID, EXPIRES) " +
                    "values (?, ?, ?, ?, ?, ?)";
                try {
                    if (!item.allowsDuplicates()) {
                        stmt = conn.prepareStatement(query);
                        stmt.setBoolean(1, item.isGangOwned());
                        stmt.setInt(2, item.getOwnerId());
                        stmt.setInt(3, itemType);
                        stmt.setBytes(4, itemData);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            return false;
                        }
                    }
                    stmt = conn.prepareStatement(insert);
                    stmt.setBoolean(1, item.isGangOwned());
                    stmt.setInt(2, item.getOwnerId());
                    stmt.setInt(3, itemType);
                    stmt.setBytes(4, itemData);
                    stmt.setInt(5, item.getGangId());
                    stmt.setDate(6, item.getExpiryDate());

                    // do the insertion
                    JDBCUtil.checkedUpdate(stmt, 1);

                    // grab and fill in the item id
                    item.setItemId(liaison.lastInsertedId(conn, stmt, "ITEMS", "ITEM_ID"));

                    // record the insertion
                    BangServer.itemLog("item_created id:" + item.getItemId() +
                                       (item.isGangOwned() ? " gang_owned" : "") +
                                       " oid:" + item.getOwnerId() +
                                       " type:" + item.getClass().getName());
                    return true;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Inserts copies of the given prototype item into the inventories of the identified users and
     * stores the created items in the provided list.
     */
    public void insertItems (final Item prototype, final ArrayIntSet userIds, final List<Item> items)
        throws PersistenceException
    {
        // determine the prototype's assigned item type and serialize it
        final int itemType = getItemType(prototype);
        final byte[] itemData = persistItem(prototype).toByteArray();
        final int gangId = prototype.getGangId();
        final Date expires = prototype.getExpiryDate();

        // now insert the flattened data into the database
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                String query = "insert into ITEMS " +
                    "(GANG_OWNED, OWNER_ID, ITEM_TYPE, ITEM_DATA, GANG_ID, EXPIRES) " +
                    "values (FALSE, ?, ?, ?, ?, ?)";
                try {
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(2, itemType);
                    stmt.setBytes(3, itemData);
                    stmt.setInt(4, gangId);
                    stmt.setDate(5, expires);

                    // do the insertions
                    for (Interator it = userIds.interator(); it.hasNext(); ) {
                        Item item = (Item)prototype.clone();
                        item.setOwnerId(it.nextInt());
                        stmt.setInt(1, item.getOwnerId());
                        JDBCUtil.checkedUpdate(stmt, 1);
                        item.setItemId(liaison.lastInsertedId(conn, stmt, "ITEMS", "ITEM_ID"));
                        items.add(item);

                        // record the insertion
                        BangServer.itemLog("item_created id:" + item.getItemId() +
                                           " oid:" + item.getOwnerId() +
                                           " type:" + item.getClass().getName());
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Returns true if the specified player owns any gold pass item, false if not. This can be used
     * on one-time payment systems to determine if the player has access to one-time functionality
     * even when they are offline.
     */
    public boolean holdsGoldPass (final int playerId)
        throws PersistenceException
    {
        return execute(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "select count(*) from ITEMS where OWNER_ID = ? and ITEM_TYPE = ?");
                try {
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, ItemFactory.getType(GoldPass.class));
                    return stmt.executeQuery().next();

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Transfers the specified item to the specified player. The database will be updated as well
     * as the item's <code>ownerId</code> field.
     */
    public void transferItem (Item item, int newOwnerId)
        throws PersistenceException
    {
        if (update("update ITEMS set OWNER_ID = " + newOwnerId +
                   " where ITEM_ID = " + item.getItemId()) == 0) {
            log.warning("Requested to transfer non-persisted item", "item", item, "to", newOwnerId);

        } else {
            // update the item's ownerId field
            int oldOwnerId = item.getOwnerId();
            item.setOwnerId(newOwnerId);
            // record the transfer
            BangServer.itemLog("item_transfer id:" + item.getItemId() +
                               " src:" + oldOwnerId + " dst:" + newOwnerId);
        }
    }

    /**
     * Updates the specified item in the database.
     */
    public void updateItem (final Item item)
        throws PersistenceException
    {
        // first serialize the item
        final ByteArrayOutInputStream out = persistItem(item);

        // now insert the flattened data into the database
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "update ITEMS set ITEM_DATA = ?, EXPIRES = ? where ITEM_ID = ?");
                try {
                    stmt.setBinaryStream(1, out.getInputStream(), out.size());
                    stmt.setDate(2, item.getExpiryDate());
                    stmt.setInt(3, item.getItemId());
                    JDBCUtil.checkedUpdate(stmt, 1);
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Deletes the specified item from the database.
     */
    public void deleteItem (final Item item, final String why)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query =
                    "delete from ITEMS where ITEM_ID = " + item.getItemId();
                Statement stmt = conn.createStatement();
                try {
                    if (stmt.executeUpdate(query) == 0) {
                        log.warning("Requested to delete non-persisted item", "item", item,
                                    "why", why);
                    } else {
                        BangServer.itemLog("item_deleted id:" + item.getItemId() + " why:" + why);
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Deletes the specified items from the database.
     */
    public void deleteItems (final ArrayIntSet itemIds, final String why)
        throws PersistenceException
    {
        if (itemIds == null || itemIds.size() < 1) {
            throw new IllegalArgumentException(
                "Refusing to delete empty itemIds set '" + itemIds + "'");
        }

        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "delete from ITEMS where ITEM_ID in " +
                    StringUtil.toString(itemIds.iterator(), "(", ")");
                Statement stmt = conn.createStatement();
                try {
                    int deleted = stmt.executeUpdate(query);
                    if (deleted != itemIds.size()) {
                        log.warning("Multiple item delete funny business", "itemIds", itemIds,
                                    "deleted", deleted, "query", query);
                    }

                    // record the deletions
                    BangServer.itemLog("items_deleted ids:" + itemIds + " why:" + why);
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Deletes all items owned by the specified player.
     */
    public void deleteItems (final int playerId, final String why)
        throws PersistenceException
    {
        final ArrayIntSet itemIds = new ArrayIntSet();

        // first enumerate all items owned by this player
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "select ITEM_ID from ITEMS where OWNER_ID = " + playerId +
                    " and GANG_OWNED = false";
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        itemIds.add(rs.getInt(1));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });

        // then delete them in the standard way so that we get an audit log with the deleted ids
        if (!itemIds.isEmpty()) {
            deleteItems(itemIds, why);
        }
    }

    /**
     * Given a set of player ids and a prototype item, determines which of the players own an
     * identical item.
     *
     * @param alt an optional alternate item to match (which must be of the same item type)
     */
    public ArrayIntSet getItemOwners (final ArrayIntSet playerIds, final Item item, final Item alt)
        throws PersistenceException
    {
        // make sure the set isn't empty
        if (playerIds.isEmpty()) {
            return new ArrayIntSet();
        }

        // serialize the item prototype
        final ByteArrayOutInputStream out = persistItem(item),
            aout = (alt == null) ? null : persistItem(alt);
        final int itemType = getItemType(item);
        final ArrayIntSet owners = new ArrayIntSet();
        execute(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "select OWNER_ID from ITEMS where GANG_OWNED = FALSE and OWNER_ID in " +
                    StringUtil.toString(playerIds.iterator(), "(", ")") +
                    " and ITEM_TYPE = ? and (ITEM_DATA = ?" +
                    (aout == null ? ")" : " or ITEM_DATA = ?)"));
                try {
                    stmt.setInt(1, itemType);
                    stmt.setBinaryStream(2, out.getInputStream(), out.size());
                    if (aout != null) {
                        stmt.setBinaryStream(3, aout.getInputStream(), out.size());
                    }
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        owners.add(rs.getInt(1));
                    }
                    return null;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return owners;
    }

    /**
     * Instantiates the appropriate item class and decodes the item from the data.
     */
    protected Item decodeItem (int itemId, int itemType, boolean gangOwned, int ownerId,
            byte[] data, int gangId, Date expires)
        throws PersistenceException, SQLException
    {
        String errmsg = null;
        Exception error = null;

        try {
            Class<?> itemClass = ItemFactory.getClass(itemType);
            if (itemClass == null) {
                errmsg = "Unable to decode item [id=" + itemId + ", type=" + itemType + "]: " +
                    "No class registered for item type";
                throw new PersistenceException(errmsg);
            }

            // create the item
            Item item = (Item)itemClass.newInstance();

            // decode its contents from the serialized data
            ByteArrayInputStream bin = new ByteArrayInputStream(data);
            item.unpersistFrom(new ObjectInputStream(bin));

            // then assign the db stored data
            item.setItemId(itemId);
            item.setGangOwned(gangOwned);
            item.setOwnerId(ownerId);
            item.setGangId(gangId);
            item.setExpires(expires);
            return item;

        } catch (IOException ioe) {
            error = ioe;
            errmsg = "Unable to decode item";

        } catch (ClassNotFoundException cnfe) {
            error = cnfe;
            errmsg = "Unable to instantiate item";

        } catch (InstantiationException ie) {
            error = ie;
            errmsg = "Unable to instantiate item";

        } catch (IllegalAccessException iae) {
            error = iae;
            errmsg = "Unable to instantiate item";
        }

        errmsg += " [id=" + itemId + ", type=" + itemType + "]";
        throw new PersistenceException(errmsg, error);
    }

    /**
     * Persists the specified item to a new byte array stream.
     */
    protected ByteArrayOutInputStream persistItem (Item item)
        throws PersistenceException
    {
        ByteArrayOutInputStream out = new ByteArrayOutInputStream();
        try {
            item.persistTo(new ObjectOutputStream(out));
            return out;
        } catch (IOException ioe) {
            String errmsg = "Error serializing item " + item;
            throw new PersistenceException(errmsg, ioe);
        }
    }

    /**
     * Returns the integer item type of the specified item.
     */
    protected int getItemType (Item item)
        throws PersistenceException
    {
        // determine it's assigned item type
        Class<? extends Item> itemClass = item.getClass();
        int itemType = ItemFactory.getType(itemClass);
        if (itemType == -1) {
            throw new PersistenceException(
                "Can't insert item of unknown type [item=" + item + ", class=" + itemClass + "]");
        }
        return itemType;
    }

    @Override // documentation inherited
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "ITEMS", new String[] {
            "ITEM_ID INTEGER NOT NULL AUTO_INCREMENT",
            "GANG_OWNED BOOLEAN NOT NULL",
            "OWNER_ID INTEGER NOT NULL",
            "EXPIRES DATE DEFAULT NULL",
            "GANG_ID INTEGER NOT NULL",
            "ITEM_TYPE INTEGER NOT NULL",
            "ITEM_DATA BLOB NOT NULL",
            "PRIMARY KEY (ITEM_ID)",
            "KEY (OWNER_ID)",
        }, "");
    }
}
