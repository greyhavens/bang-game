//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Provides the player with a one-shot crazy thing that they can do during
 * the course of a game. Players can hold some fixed number of cards and
 * use them on any turn during the game (modulo special restrictions).
 */
public abstract class Card extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Every card has a unique id which is how we reference them. */
    public int cardId;

    /** The player index of the player that is holding this card. */
    public int owner;

    /** Returns the name of the icon image for this card. */
    public abstract String getIconPath ();

    /** Returns the radius that should be used when displaying this
     * card's area of effect. */
    public abstract int getRadius ();

    /**
     * Activates the specified card at the supplied coordinates. The
     * returned effect will be prepared and effected immediately.
     *
     * @return the effect of the card activation.
     */
    public abstract Effect activate (int x, int y);

    /**
     * This is used to assign the owner and a new unique id to a card when
     * it is created (on the server). Derived classes can also override
     * this method and further configure their card based on the relative
     * strength or weakness of the receiving player.
     */
    public void init (BangObject bangobj, int owner)
    {
        _key = null;
        cardId = 0;
        this.owner = owner;
        getKey();
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            if (cardId == 0) {
                cardId = ++_nextCardId;
            }
            _key = new Integer(cardId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return cardId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return cardId == ((Card)other).cardId;
    }

    /** Used as our DSet.Entry key. */
    protected transient Integer _key;

    /** Used to assign unique ids to card instances. */
    protected static int _nextCardId;
}
