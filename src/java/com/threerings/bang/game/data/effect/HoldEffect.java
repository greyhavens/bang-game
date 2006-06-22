//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import java.util.logging.Level;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles bonuses which can be held (therefor picked up and dropped).
 */
public class HoldEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String PICKED_UP_BONUS = "pickedup";

    /** The identifier for the type of effect that we produce. */
    public static final String DROPPED_BONUS = "dropped";

    /** The unit receiving or depositing the bonus. */
    public int pieceId;

    /** If true, the piece in question is dropping a bonus; */ 
    public boolean dropping;

    /** The unit that caused us to drop our bonus (if any). */
    public int causerId = -1;

    /** The bonus to be dropped or null if no bonus is to be dropped. */
    public Bonus drop;

    /** The type of bonus being effected. */
    public String type;

    /**
     * Creates a hold effect configured to cause the specified unit to drop
     * their bonus. Returns null if a location for the bonus to fall could
     * not be found.
     *
     * @param causerId the piece id of the piece that caused this piece to drop
     * this bonus, used for animation sequencing.
     */
    public static HoldEffect dropBonus (
        BangObject bangobj, Unit unit, int causerId, String type)
    {
        Bonus bonus = Bonus.createBonus(BonusConfig.getConfig(type));
        if (bonus == null) {
            return null;
        }
        Point spot = bangobj.board.getOccupiableSpot(unit.x, unit.y, 3);
        if (spot == null) {
            log.warning("Unable to find spot to drop bonus " +
                "[unit=" + unit + ", type=" + type + "].");
            return null;
        }
        
        try {
            HoldEffect effect = (HoldEffect)Class.forName(
                    bonus.getConfig().effectClass).newInstance();
            effect.init(unit);
            effect.dropping = true;
            effect.causerId = causerId;
            effect.drop = bonus;
            effect.drop.assignPieceId(bangobj);
            effect.drop.position(spot.x, spot.y);
            effect.type = type;
            return effect;
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to instantiate effect class " +
                    "[class=" + bonus.getConfig().effectClass + "].", e);
            return null;
        }
    }

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        if (causerId != -1) {
            return new int[] { causerId };
        }
        return super.getWaitPieces();
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (drop != null) {
            return new int[] { pieceId, drop.pieceId, bonusId };
        }
        return new int[] { pieceId, bonusId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            log.warning("Missing unit for hold effect [id=" + pieceId + "].");
            return;
        }

        if (!dropping) {
            // mark the target piece as holding now as they may have landed
            // next to an object which will also try to give them a holdable
            // bonus; we'll need to update their holding again in apply to 
            // ensure that it happens on the client
            unit.holding = type;
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit != null) {
            if (dropping) {
                unit.holding = null;
                reportEffect(obs, unit, DROPPED_BONUS);
            } else {
                unit.holding = type;
                reportEffect(obs, unit, PICKED_UP_BONUS);
            }
        }

        if (drop != null) {
            bangobj.addPieceDirect(drop);
            reportAddition(obs, drop);
        }
        return true;
    }
}
