//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Causes the unit affected by it to drop its nugget.
 */
public class DropNuggetEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DROPPED = "bonuses/nugget/dropped";

    /** The piece id of the dropping piece. */
    public int dropperId;

    /** The nugget to be dropped. */
    public Bonus drop;

    /**
     * Unserialization constructor or one used when the effect is used in
     * the normal manner.
     */
    public DropNuggetEffect ()
    {
    }

    /**
     * Constructor used when a piece drops its nugget as a result of
     * collateral damage.
     */
    public DropNuggetEffect (Unit target)
    {
        init(target);
    }

    @Override // documentation inherited
    public void init (Piece piece)
    {
        dropperId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { dropperId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece dropper = (Piece)bangobj.pieces.get(dropperId);
        if (dropper == null) {
            log.warning("Missing dropper for effect!? [id=" + dropperId + "].");
            return;
        }

        Point spot = bangobj.board.getOccupiableSpot(dropper.x, dropper.y, 3);
        if (spot == null) {
            log.warning("Unable to find spot to drop nugget " +
                        "[dropper=" + dropper + "].");
            return;
        }

        drop = Bonus.createBonus(BonusConfig.getConfig("nugget"));
        drop.assignPieceId(bangobj);
        drop.position(spot.x, spot.y);
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        if (drop == null) {
            return;
        }
        bangobj.addPieceDirect(drop);
        reportAddition(obs, drop);

        Unit dropper = (Unit)bangobj.pieces.get(dropperId);
        if (dropper == null) {
            log.warning("Missing dropper " + this + ".");
            return;
        }
        dropper.benuggeted = false;
        reportEffect(obs, dropper, DROPPED);
    }
}
