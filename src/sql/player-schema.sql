/**
 * $Id: items-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy player repository tables.
 */

drop table if exists PLAYERS;

/**
 * Contains the persistent data associated with each player.
 */
CREATE TABLE PLAYERS
(
    /** The unique identifier for this player. */
    PLAYER_ID INTEGER NOT NULL AUTO_INCREMENT,

    /** The authentication account name associated with this player. */
    ACCOUNT_NAME VARCHAR(255) NOT NULL,

    /** The amount of scrip this player holds. */
    SCRIP INTEGER NOT NULL,

    /** The time at which this player was created (when they first starting
     * playing  this particular game). */
    CREATED DATETIME NOT NULL,

    /** The number of sessions this player has played. */
    SESSIONS INTEGER UNSIGNED NOT NULL,

    /** The cumulative number of minutes spent playing. */
    SESSION_MINUTES INTEGER NOT NULL,

    /** The time at which the player ended their last session. */
    LAST_SESSION DATETIME NOT NULL,

    /** Defines our table keys. */
    PRIMARY KEY (PLAYER_ID)
);
