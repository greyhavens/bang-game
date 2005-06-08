/**
 * $Id: items-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy item repository tables.
 */

drop table if exists ITEMS;

/**
 * Contains the persistent data associated with items.
 */
CREATE TABLE ITEMS
(
    /** The unique identifier for this item. */
    ITEM_ID INTEGER NOT NULL AUTO_INCREMENT,

    /** The player id of the owner of this item. */
    OWNER_ID INTEGER NOT NULL,

    /** The item type code for this item (maps to derived class). */
    ITEM_TYPE INTEGER NOT NULL,

    /** A serialized representation of the item's contents. */
    ITEM_DATA BLOB NOT NULL,

    /** Defines our table keys. */
    PRIMARY KEY (ITEM_ID),
    KEY (OWNER_ID)
);
