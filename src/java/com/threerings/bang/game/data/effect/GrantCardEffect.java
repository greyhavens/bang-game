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
    /** Effect reported when a player activates a card bonus. */
    public static final String ACTIVATED_CARD = "frontier_town/card";

    /** The card to grant to the player. */
    public Card card;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        super.init(piece);
        _player = piece.owner;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        GrantCardEffect effect = (GrantCardEffect)super.clone();
        effect.card = (Card)card.clone();
        return effect;
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
        if (bangobj.countPlayerCards(_player) >= GameCodes.MAX_CARDS) {
            log.info("No soup four you! " + _player + ".");
            return;
        }

        super.prepare(bangobj, dammap);

        // pick a weighted random card to be given to the player
        card = Card.newCard(Card.selectRandomCard(bangobj.townId, bangobj, _player));
        card.init(bangobj, _player);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return card != null;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);
        addAndReport(bangobj, card, obs);
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose(
            "m.effect_card", piece.getName(),
            MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + card.getType()));
    }

    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return ACTIVATED_CARD;
    }

    /** The player receiving the card. */
    protected transient int _player;
}
