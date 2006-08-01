//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.card.Card;

import static com.threerings.bang.Log.*;

/**
 * Removes a random card from one player.
 */
public class DropCardEffect extends Effect
{
    /** The index of the player losing a card. */
    public int player;
    
    /** The type of card taken from the player. */
    public String type;
    
    public DropCardEffect ()
    {
    }
    
    public DropCardEffect (int pidx)
    {
        player = pidx;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[0];
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        int ccount = bangobj.countPlayerCards(player);
        if (ccount == 0) {
            log.warning("Couldn't find card to drop [pidx=" + player + "].");
            return;
        }
        int cidx = RandomUtil.getInt(ccount);
        Card dcard = null;
        for (Card card : bangobj.cards) {
            if (card.owner == player && cidx-- == 0) {
                dcard = card;
                break;
            }
        }
        type = dcard.getType();
        bangobj.removeFromCards(dcard.cardId);
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer observer)
    {
        return (type != null);
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx != player) {
            return null;
        }
        return MessageBundle.compose("m.effect_drop_card",
            MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + type));
    }
}
