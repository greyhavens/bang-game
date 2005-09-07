/**
 * $Id: stats-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy stat repository tables.
 */

drop table if exists STATS;

/**
 * Contains persistent data associated with per-player statistics.
 */
CREATE TABLE STATS
(
    /** The player id of the owner of this stat. */
    PLAYER_ID INTEGER NOT NULL,

    /** The stat code for this stat (maps to derived class). */
    STAT_CODE INTEGER NOT NULL,

    /** A serialized representation of the stat's contents. */
    STAT_DATA BLOB NOT NULL,

    /** Defines our table keys. */
    KEY (PLAYER_ID),
    KEY (STAT_CODE)
);
