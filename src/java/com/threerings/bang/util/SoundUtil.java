//
// $Id$

package com.threerings.bang.util;

import java.util.HashSet;

import com.threerings.bang.util.BasicContext;

/**
 * Handles some useful sound related bits.
 */
public class SoundUtil
{
    /**
     * Loads up information from our deployment.
     */
    public static void init (BasicContext ctx)
    {
        String[] sounds = BangUtil.resourceToStrings("rsrc/sounds.txt");
        for (int ii = 0; ii < sounds.length; ii++) {
            _sounds.add(sounds[ii]);
        }
    }

    /**
     * Returns true if the specified sound file is bundled with the game,
     * false if not.
     */
    public static boolean haveSound (String path)
    {
        return _sounds.contains(path);
    }

    protected static HashSet<String> _sounds = new HashSet<String>();
}
