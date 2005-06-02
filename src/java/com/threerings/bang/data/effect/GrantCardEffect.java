//
// $Id$

package com.threerings.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.card.Card;

import static com.threerings.bang.Log.log;

/**
 * Delivers a card to the specified player.
 */
public class GrantCardEffect extends Effect
{
    public int player;

    public GrantCardEffect (int player, Card card)
    {
        this.player = player;
        _card = card;
    }

    public GrantCardEffect ()
    {
    }

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
        if (have >= 3) {
            log.info("No soup four you! " + player + ".");
            return;
        }

        _card.init(bangobj, player);
        bangobj.addToCards(_card);
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // NOOP
    }

    protected transient Card _card;
}
