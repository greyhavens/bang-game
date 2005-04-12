//
// $Id$

package com.threerings.bang.data.surprise;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.effect.Effect;

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

    /** Returns the name of the icon image for this surprise. */
    public abstract String getIconPath ();

    /** Returns the radius that should be used when displaying this
     * surprise's area of effect. */
    public abstract int getRadius ();

    /**
     * Activates the specified surprise at the supplied coordinates. The
     * returned effect will be prepared and effected immediately.
     *
     * @return the effect of the surprise activation.
     */
    public abstract Effect activate (int x, int y);

    /**
     * This is used to assign the owner and a new unique id to a surprise
     * when it is created (on the server). Derived classes can also
     * override this method and further configure their surprise based on
     * the relative strength or weakness of the receiving player.
     */
    public void init (BangObject bangobj, int owner)
    {
        _key = null;
        surpriseId = 0;
        this.owner = owner;
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
