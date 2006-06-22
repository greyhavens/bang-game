//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BonusConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Causes the activating piece to "pick up" the nugget.
 */
public class NuggetEffect extends HoldEffect
{
    /** The bonus identifier for the nugget bonus. */
    public static final String NUGGET_BONUS = "frontier_town/nugget";

    /** The identifier for the type of effect that we produce. */
    public static final String PICKED_UP_NUGGET =
        "bonuses/frontier_town/nugget/pickedup";

    /** The identifier for the type of effect that we produce. */
    public static final String DROPPED_NUGGET =
        "bonuses/frontier_town/nugget/dropped";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_ADDED =
        "bonuses/frontier_town/nugget/added";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_REMOVED =
        "bonuses/frontier_town/nugget/removed";

    /** The id of the claim involved in this nugget transfer or -1 if we're
     * picking the nugget up off of the board. */
    public int claimId = -1;

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
        return (NuggetEffect)dropBonus(bangobj, unit, causerId, NUGGET_BONUS);
    }

    public NuggetEffect ()
    {
        type = NUGGET_BONUS;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] affected = super.getAffectedPieces();
        if (drop == null) {
            return ArrayUtil.append(affected, claimId);
        }
        return affected;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        if (claimId > 0) {
            Counter claim = (Counter)bangobj.pieces.get(claimId);
            if (dropping) {
                // if we're on the server, grant points to the player
                if (bangobj.getManager().isManager(bangobj)) {
                    bangobj.grantPoints(
                        claim.owner, ScenarioCodes.POINTS_PER_NUGGET);
                }
                claim.count++;
                reportEffect(obs, claim, NUGGET_ADDED);
            } else {
                // if we're on the server, deduct points from the player
                if (bangobj.getManager().isManager(bangobj)) {
                    bangobj.grantPoints(
                        claim.owner, -ScenarioCodes.POINTS_PER_NUGGET);
                }
                claim.count--;
                reportEffect(obs, claim, NUGGET_REMOVED);
            }
        }

        return true;
    }

    @Override // documentation inherited
    protected String getDroppedEffect ()
    {
        return DROPPED_NUGGET;
    }
    
    @Override // documentation inherited
    protected String getPickedUpEffect ()
    {
        return PICKED_UP_NUGGET;
    }
}
