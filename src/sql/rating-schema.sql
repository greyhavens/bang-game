/**
 * $Id: ratings-schema.sql 17079 2004-09-17 02:26:30Z ray $
 *
 * Schema for the Bang! Howdy rating repository tables.
 */

drop table if exists RATINGS;

/**
 * Contains persistent data associated with per-player ratings.
 */
CREATE TABLE RATINGS
(
    /** The player id of the owner of this rating. */
    PLAYER_ID INTEGER NOT NULL,

    /** The scenario for which this rating is applicable. */
    SCENARIO VARCHAR(2) NOT NULL,

    /** The rating value. */
    RATING SMALLINT NOT NULL,

    /** The number of times this scenario has been played. */
    EXPERIENCE INTEGER NOT NULL,

    /** Defines our table keys. */
    PRIMARY KEY (PLAYER_ID, SCENARIO)
);
