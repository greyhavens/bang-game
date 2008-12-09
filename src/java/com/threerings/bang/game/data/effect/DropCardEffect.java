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
    /** The id of the card taken. */
    public int cardId = -1;
    
    public DropCardEffect ()
    {
    }
    
    public DropCardEffect (int pidx)
    {
        _player = pidx;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[0];
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        int ccount = bangobj.countPlayerCards(_player);
        if (ccount == 0) {
            log.warning("Couldn't find card to drop", "pidx", _player);
            return;
        }
        int cidx = RandomUtil.getInt(ccount);
        for (Card card : bangobj.cards) {
            if (card.owner == _player && cidx-- == 0) {
                cardId = card.cardId;
                break;
            }
        }
    }
    
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return cardId > 0;
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        _card = bangobj.cards.get(cardId);
        if (_card == null) {
            log.warning("Missing card to drop", "cardId", cardId);
            return false;
        }
        removeAndReport(bangobj, _card, obs);
        return true;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx != _card.owner || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_drop_card",
            MessageBundle.qualify(BangCodes.CARDS_MSGS,
                "m." + _card.getType()));
    }
    
    /** The index of the player from whom to take the card. */
    protected transient int _player;
    
    /** The removed card. */
    protected transient Card _card;
}
