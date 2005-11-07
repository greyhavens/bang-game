/**
 * $Id: boards-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy board repository tables.
 */

drop table if exists BOARDS;

/**
 * Contains persistent information for all of the game boards.
 */
CREATE TABLE BOARDS
(
    /** The unique identifier for this board. */
    BOARD_ID INTEGER NOT NULL AUTO_INCREMENT,

    /** The human readable name of this board. */
    NAME VARCHAR(255) NOT NULL,

    /** The username of the player that created this board, or null. */
    CREATOR VARCHAR(255),

    /** The scenarios for which this board is playable. */
    SCENARIOS VARCHAR(255),

    /** The number of players for which this board is appropriate. */
    PLAYERS INTEGER NOT NULL,

    /** The number of "plays" recorded to this board. */
    PLAYS INTEGER NOT NULL,

    /** A serialized representation of the board data. */
    DATA BLOB NOT NULL,

    /** Defines our table keys. */
    PRIMARY KEY (BOARD_ID), UNIQUE(NAME,PLAYERS), KEY(CREATOR)
);
