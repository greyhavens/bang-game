//
// $Id$

package com.threerings.bang.data;

import java.util.HashMap;

/**
 * Represents a Big Shot unit owned by a player.
 */
public class BigShot extends Item
{
    /** A Frontier Town Big Shot. */
    public static final String MOUNTED_RIFLEMAN = "mounted_rifleman";

    /**
     * Returns an array of all Big Shot types available for the specified
     * town.
     */
    public static String[] getBigShotTypes (String townId)
    {
        return _shotTypes.get(townId);
    }

    /**
     * A blank constructor used during unserialization.
     */
    public BigShot ()
    {
    }

    /**
     * Creates a new Big Shot item of the specified type.
     */
    public BigShot (int ownerId, String type)
    {
        _type = type;
    }

    /**
     * Returns the type code for this Big Shot. This is the same as the
     * associated unit type.
     */
    public String getType ()
    {
        return _type;
    }

    protected static void registerBigShot (String townId, String bigShotId)
    {
        String[] shots = _shotTypes.get(townId);
        if (shots == null) {
            shots = new String[0];
        }
        String[] nshots = new String[shots.length+1];
        System.arraycopy(shots, 0, nshots, 0, shots.length);
        nshots[shots.length] = bigShotId;
        _shotTypes.put(townId, nshots);
    }

    protected String _type;

    protected static HashMap<String,String[]> _shotTypes =
        new HashMap<String,String[]>();

    static {
        // register the Frontier Town Big Shots
        registerBigShot(BangCodes.FRONTIER_TOWN, MOUNTED_RIFLEMAN);
    }
}
