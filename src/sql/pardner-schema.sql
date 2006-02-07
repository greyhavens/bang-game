/**
 * $Id$
 *
 * Schema for the Bang! pardner lists.
 */

drop table if exists PARDNERS;

/**
 * The pardners table contains the paired player ids of people who want to be
 * friends.
 */
CREATE TABLE PARDNERS
(
    /**
     * The id of the first player in a pair (the inviter).
     */
    PLAYER_ID1 INTEGER UNSIGNED NOT NULL,

    /**
     * The id of the second player in a pair (the invitee).
     */
    PLAYER_ID2 INTEGER UNSIGNED NOT NULL,

    /**
     * Whether or not the pardnership is active.
     */
    ACTIVE TINYINT UNSIGNED NOT NULL,
    
    /**
     * Define a unique index on 1,2 to prevent duplicates and which
     *  also acts as an index on 1. Then add a second index on 2.
     */
    UNIQUE (PLAYER_ID1, PLAYER_ID2), INDEX (PLAYER_ID2)
);
