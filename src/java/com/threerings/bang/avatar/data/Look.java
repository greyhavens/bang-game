//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Defines a particular "look" for a player's avatar.
 */
public class Look extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The maximum length of a look's name. */
    public static final int MAX_NAME_LENGTH = 48;

    /** The name of this look (provided by the player). */
    public String name;

    /** The immutable avatar aspects associated with this look (character
     * component ids). */
    public int[] aspects;

    /** The most recently configured set of articles for this look (character
     * component ids and colorizations). */
    public int[] articles;

    /**
     * Combines the aspect and article information into a full avatar
     * fingerprint.
     */
    public int[] getAvatar ()
    {
        int[] avatar = new int[aspects.length+articles.length];
        System.arraycopy(aspects, 0, avatar, 0, aspects.length);
        System.arraycopy(articles, 0, avatar, aspects.length, articles.length);
        return avatar;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return name;
    }
}
