//
// $Id$

package com.threerings.bang.server.persist;

/**
 * Contains a friend/foe mapping.
 */
public class FolkRecord
{
    /** Indicates that a player thinks another is friendly. */
    public static final byte FRIEND = 0;

    /** Indicates that a player thinks another is dastardly. */
    public static final byte FOE = 1;

    /** The id of the player holding the opinion. */
    public int playerId;

    /** The id of the player about whom an opinion is held. */
    public int targetId;

    /** Either {@link #FRIEND} or {@link #FOE}. */
    public byte opinion;
}
