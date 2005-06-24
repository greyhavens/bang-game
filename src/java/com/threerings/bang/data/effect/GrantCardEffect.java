//
// $Id$

package com.threerings.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.card.AreaRepair;
import com.threerings.bang.data.card.Card;
import com.threerings.bang.data.card.DustDevil;
import com.threerings.bang.data.card.Missile;
import com.threerings.util.RandomUtil;

import static com.threerings.bang.Log.log;

/**
 * Delivers a card to the specified player.
 */
public class GrantCardEffect extends Effect
{
    public int player;

    public GrantCardEffect (int player)
    {
        this.player = player;
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

        // TODO: select the card more sophisticatedly
        Card card;
        switch (RandomUtil.getInt(3)) {
        default: case 0: card = new Missile(); break;
        case 1: card = new AreaRepair(); break;
        case 2: card = new DustDevil(); break;
        }

        card.init(bangobj, player);
        bangobj.addToCards(card);
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // NOOP
    }
}
