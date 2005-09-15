//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.game.data.card.Card;

/**
 * Represents a particular card held in the user's inventory. All copies
 * of this card are consolidated into one card item.
 */
public class CardItem extends Item
{
    /** A blank constructor used during unserialization. */
    public CardItem ()
    {
    }

    /** Creates a new card item of the specified type. */
    public CardItem (int ownerId, String type)
    {
        super(ownerId);
        _type = type;
    }

    /**
     * Returns the type code for this card.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns the number of copies of this card represented by this item.
     */
    public int getQuantity ()
    {
        return _quantity;
    }

    /**
     * Deducts one from the number of cards represented by this item.
     *
     * @return true if no cards remain and this item should be destroyed,
     * false if there is at least one more card.
     */
    public boolean playCard ()
    {
        // don't allow things to stray into the negative
        if (_quantity <= 1) {
            _quantity = 0;
            return true;
        } else {
            _quantity--;
            return false;
        }
    }

    /**
     * Returns a {@link Card} instance that can be used in a game. Note,
     * this does not deduct the card from this item, that will be done
     * later by a call to {@link #playCard} after the game manager has
     * confirmed that the card was indeed used in the course of the game.
     */
    public Card getCard ()
    {
        return Card.newCard(_type);
    }

    protected String _type;
    protected int _quantity;
}
