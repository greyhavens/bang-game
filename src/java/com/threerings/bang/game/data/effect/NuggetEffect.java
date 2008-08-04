//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.NuggetScenarioInfo;

/**
 * Causes the activating piece to "pick up" the nugget.
 */
public class NuggetEffect extends HoldEffect
{
    /** The bonus identifier for the nugget bonus. */
    public static final String NUGGET_BONUS = "frontier_town/nugget";

    /** The identifier for the type of effect that we produce. */
    public static final String PICKED_UP_NUGGET =
        "frontier_town/nugget/pickedup";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_ADDED = "frontier_town/nugget/added";

    /** The identifier for the type of effect that we produce. */
    public static final String NUGGET_REMOVED = "frontier_town/nugget/removed";

    /** The id of the claim involved in this nugget transfer or -1 if we're
     * picking the nugget up off of the board. */
    public int claimId = -1;

    /**
     * Determines whether the given bonus type represents either a real nugget
     * or a nugget of fool's gold.
     */
    public static boolean isNuggetBonus (Piece piece)
    {
        return (piece instanceof Bonus) && isNuggetBonus(((Bonus)piece).getConfig().type);
    }

    /**
     * Determines whether the given bonus type represents either a real nugget
     * or a nugget of fool's gold.
     */
    public static boolean isNuggetBonus (String type)
    {
        return NUGGET_BONUS.equals(type) ||
            FoolsNuggetEffect.FOOLS_NUGGET_BONUS.equals(type);
    }
    
    public NuggetEffect ()
    {
        type = NUGGET_BONUS;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] affected = super.getAffectedPieces();
        if (claimId > 0) {
            return ArrayUtil.append(affected, claimId);
        }
        return affected;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);
        if (claimId > 0) {
            applyToClaim(bangobj, obs);
        }
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1 || !dropping ||
            claimId > 0) {
            return null;
        }
        return MessageBundle.compose("m.effect_drop_nugget", piece.getName());
    }
    
    @Override // documentation inherited
    public String getPickedUpEffect ()
    {
        return PICKED_UP_NUGGET;
    }
        
    /**
     * Applies the effect on the claim.
     */
    protected void applyToClaim (BangObject bangobj, Observer obs)
    {
        Counter claim = (Counter)bangobj.pieces.get(claimId);
        if (dropping) {
            // if we're on the server, grant points to the player
            if (bangobj.getManager().isManager(bangobj)) {
                bangobj.grantPoints(
                    claim.owner, NuggetScenarioInfo.POINTS_PER_NUGGET);
            }
            claim.count++;
            reportEffect(obs, claim, NUGGET_ADDED);
        } else {
            // if we're on the server, deduct points from the player
            if (bangobj.getManager().isManager(bangobj)) {
                bangobj.grantPoints(
                    claim.owner, -NuggetScenarioInfo.POINTS_PER_NUGGET);
            }
            claim.count--;
            reportEffect(obs, claim, NUGGET_REMOVED);
        }
    }   
}
