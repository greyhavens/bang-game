//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Represents the act of turning a bonus on the board into a card.
 */
public class LassoBonusEffect extends BonusEffect
{
    /** The lassoing player. */
    public int player;
    
    /** The location of the bonus to lasso. */
    public transient int x, y;
    
    public LassoBonusEffect ()
    {
    }
    
    public LassoBonusEffect (int player, int x, int y)
    {
        this.player = player;
        this.x = x;
        this.y = y;
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
            log.warning("No soup four you! " + player + ".");
            return;
        }

        // find the bonus
        Bonus bonus = null;
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Bonus && piece.intersects(x, y)) {
                bonus = (Bonus)piece;
                break;
            }
        }
        if (bonus == null) {
            log.warning("Couldn't find bonus for lasso effect [pidx=" +
                player + ", x=" + x + ", y=" + y + "].");
            return;
        }
        String ctype = bonus.getConfig().cardType;
        if (ctype == null) {
            log.warning("Tried to lasso bonus without corresponding card " +
                "[pidx=" + player + ", bonus=" + bonus + "].");
            return;
        }
        bonusId = bonus.pieceId;
        
        // grant the corresponding card
        Card card = Card.newCard(ctype.equals("__random__") ?
            Card.selectRandomCard(bangobj.townId, true) : ctype);
        card.init(bangobj, player);
        bangobj.addToCards(card);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return bonusId > 0;
    }
}
