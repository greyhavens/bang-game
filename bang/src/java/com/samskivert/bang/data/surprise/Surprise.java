//
// $Id$

package com.samskivert.bang.data.surprise;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Provides the player with a one-shot crazy thing that they can do during
 * the course of a game. Players can hold some fixed number of surprises
 * and use them on any turn during the game (modulo special restrictions).
 */
public abstract class Surprise extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Every surprise has a unique id which is how we reference them. */
    public int surpriseId;

    /** The player index of the player that is holding this surprise. */
    public int owner;

    /**
     * This is used to assign a new unique id to a surprise when one is
     * created (on the server).
     */
    public void assignSurpriseId ()
    {
        _key = null;
        surpriseId = 0;
        getKey();
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            if (surpriseId == 0) {
                surpriseId = ++_nextSurpriseId;
            }
            _key = new Integer(surpriseId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return surpriseId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return surpriseId == ((Surprise)other).surpriseId;
    }

    /** Used as our DSet.Entry key. */
    protected transient Integer _key;

    /** Used to assign unique ids to surprise instances. */
    protected static int _nextSurpriseId;
}
