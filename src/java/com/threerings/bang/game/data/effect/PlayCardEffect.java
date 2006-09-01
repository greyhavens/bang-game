//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.card.Card;

import static com.threerings.bang.Log.*;

/**
 * Represents the act of playing a card.
 */
public class PlayCardEffect extends Effect
{
    /** A copy of the card played. */
    public Card card;
    
    /** The target that the card was played on. */
    public Object target;
    
    public PlayCardEffect ()
    {
    }
    
    public PlayCardEffect (Card card, Object target)
    {
        this.card = card;
        this.target = target;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return (card.getPlacementMode() == Card.PlacementMode.VS_PIECE) ?
            new int[] { (Integer)target } : NO_PIECES;
    }
    
    @Override // documentation inherited
    public Rectangle getBounds (BangObject bangobj)
    {
        if (card.getPlacementMode() == Card.PlacementMode.VS_AREA) {
            int[] coords = (int[])target;
            int radius = card.getRadius();
            return new Rectangle(coords[0] - radius, coords[1] - radius,
                radius * 2 + 1, radius * 2 + 1);
        } else {
            return null;
        }
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // no-op
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // make sure the card exists
        if (!bangobj.cards.contains(card)) {
            log.warning("Missing card for card played effect [card=" +
                card + ", target=" + target + "].");
            return false;
        }
        playAndReport(bangobj, card, target, obs);
        return true;
    }
}
