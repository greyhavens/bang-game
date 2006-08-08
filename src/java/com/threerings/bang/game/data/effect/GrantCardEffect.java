//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
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
        _pieceId = piece.pieceId;
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
        if (bangobj.countPlayerCards(player) >= GameCodes.MAX_CARDS) {
            log.info("No soup four you! " + player + ".");
            return;
        }

        Card card = Card.newCard(
            Card.selectRandomCard(bangobj.townId, bangobj.scenario));
        _type = card.getType();
        card.init(bangobj, player);
        bangobj.addToCards(card);
    }
    
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return _type != null;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(_pieceId);
        if (piece == null || piece.owner != pidx) {
            return null;
        }
        return MessageBundle.compose("m.effect_card", piece.getName(),
            MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + _type));
    }
    
    /** The id of the piece that activated the bonus. */
    protected int _pieceId;
    
    /** The type of card generated. */
    protected String _type;
}
