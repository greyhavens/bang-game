//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Causes the activating piece to "pick up" the nugget.
 */
public class NuggetEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String PICKED_UP_NUGGET = "bonuses/nugget/pickedup";

    /** The identifier for the type of effect that we produce. */
    public static final String DROPPED_NUGGET = "bonuses/nugget/dropped";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_ADDED = "bonuses/nugget/added";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_REMOVED = "bonuses/nugget/removed";

    /** The unit receiving or depositing the nugget. */
    public int pieceId;

    /** If true, the piece in question is dropping a nugget; either into a
     * claim if claimId is greater than zero or onto the ground if it is -1. */
    public boolean dropping;

    /** The unit that caused us to drop our nugget (if any). */
    public int causerId = -1;

    /** The id of the claim involved in this nugget transfer or -1 if we're
     * picking the nugget up off of the board. */
    public int claimId = -1;

    /** The nugget to be dropped or null if no nugget is to be dropped. */
    public Bonus drop;

    /**
     * Creates a nugget effect configured to cause the specified unit to drop
     * their nugget. Returns null if a location for the nugget to fall could
     * not be found.
     *
     * @param causerId the piece id of the piece that caused this piece to drop
     * this nugget, used for animation sequencing.
     */
    public static NuggetEffect dropNugget (
        BangObject bangobj, Unit unit, int causerId)
    {
        Point spot = bangobj.board.getOccupiableSpot(unit.x, unit.y, 3);
        if (spot == null) {
            log.warning("Unable to find spot to drop nugget " +
                "[unit=" + unit + "].");
            return null;
        }

        NuggetEffect effect = new NuggetEffect();
        effect.init(unit);
        effect.dropping = true;
        effect.causerId = causerId;
        effect.drop = Bonus.createBonus(BonusConfig.getConfig("nugget"));
        effect.drop.assignPieceId(bangobj);
        effect.drop.position(spot.x, spot.y);
        return effect;
    }

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { causerId };
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId, claimId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            log.warning("Missing unit for nugget effect [id=" + pieceId + "].");
            return;
        }

        if (!dropping) {
            // mark the target piece as benuggeted now as they may have landed
            // on a nugget which was right next to a claim and the claim needs
            // to know not to give them another nugget before this effect will
            // be applied; we'll need to benugget them again in apply to ensure
            // that it happens on the client
            unit.benuggeted = true;
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit != null) {
            if (dropping) {
                unit.benuggeted = false;
                reportEffect(obs, unit, DROPPED_NUGGET);
            } else {
                unit.benuggeted = true;
                reportEffect(obs, unit, PICKED_UP_NUGGET);
            }
        }

        if (drop != null) {
            bangobj.addPieceDirect(drop);
            reportAddition(obs, drop);

        } else if (claimId > 0) {
            Claim claim = (Claim)bangobj.pieces.get(claimId);
            if (dropping) {
                // if we're on the server, grant points to the player
                if (bangobj.getManager().isManager(bangobj)) {
                    bangobj.grantPoints(
                        claim.owner, ScenarioCodes.POINTS_PER_NUGGET);
                }
                claim.nuggets++;
                reportEffect(obs, claim, NUGGET_ADDED);
            } else {
                // if we're on the server, deduct points from the player
                if (bangobj.getManager().isManager(bangobj)) {
                    bangobj.grantPoints(
                        claim.owner, -ScenarioCodes.POINTS_PER_NUGGET);
                }
                claim.nuggets--;
                reportEffect(obs, claim, NUGGET_REMOVED);
            }
        }
    }
}
