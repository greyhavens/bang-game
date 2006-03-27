//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Delivers a card to the specified player.
 */
public class GrantCardEffect extends BonusEffect
{
    public int player;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        player = piece.owner;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[0];
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // make sure our player has room for another card
        int have = 0;
        for (Iterator iter = bangobj.cards.iterator(); iter.hasNext(); ) {
            Card card = (Card)iter.next();
            if (card.owner == player) {
                have++;
            }
        }
        if (have >= GameCodes.MAX_CARDS) {
            log.info("No soup four you! " + player + ".");
            return;
        }

        // if they are in a tutorial, we always give a missile card, otherwise
        // select a random card
        Card card = Card.newCard(
            bangobj.scenarioId.equals(ScenarioCodes.TUTORIAL) ?
            "missile" : Card.selectRandomCard(bangobj.townId, true));
        card.init(bangobj, player);
        bangobj.addToCards(card);
    }
}
