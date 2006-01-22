/**
 * $Id: items-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy look repository tables.
 */

drop table if exists LOOKS;

/**
 * Contains persistent data associated with each avatar "look".
 */
CREATE TABLE LOOKS
(
    /** The id of the player that owns this look. */
    PLAYER_ID INTEGER NOT NULL,

    /** The name given to this look by the player. */
    NAME VARCHAR(24) NOT NULL,

    /** The array of integers representing the avatar's aspects. */
    ASPECTS BLOB NOT NULL,

    /** The array of integers representing the avatar's articles. */
    ARTICLES BLOB NOT NULL,

    /** Defines our table keys. */
    KEY (PLAYER_ID)
);
