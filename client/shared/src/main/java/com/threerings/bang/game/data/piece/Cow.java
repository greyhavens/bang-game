//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.client.sprite.CowSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.SpookEffect;
import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.util.PieceUtil;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the behavior of the cow piece which is used in cattle rustling and
 * other scenarios.
 */
public class Cow extends Piece
{
    /** Indicates whether or not this cow has been corralled. */
    public boolean corralled;

    /**
     * Called when a unit moves next to this cow or the cow was part of
     * a mass spooking; causes the cow to move away from the spooker.
     *
     * @param herd if true, the cow was spooked as part of a herd, and was
     * not branded
     */
    public SpookEffect spook (BangObject bangobj, Piece spooker, boolean herd)
    {
        // if we were spooked by a big shot, become owned by that player
        int owner = -1;
        if (spooker instanceof Unit && !herd && spooker.isAlive()) {
            Unit unit = (Unit)spooker;
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT &&
                    bangobj.board.canCross(spooker.x, spooker.y, x, y)) {
                if (this.owner != -1) {
                    bangobj.grantPoints(
                        this.owner, -CattleRustlingInfo.POINTS_PER_COW);
                }
                owner = spooker.owner;
                bangobj.grantPoints(owner, CattleRustlingInfo.POINTS_PER_COW);
            }
        }

        // run in the opposite direction of our spooker
        return move(bangobj, (PieceUtil.getDirection(this, spooker) + 2) % 4,
            owner, spooker.pieceId);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new CowSprite();
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 4;
    }

    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (bangobj.getTeam(mover.owner) != bangobj.getTeam(owner) && mover instanceof Unit &&
            ((Unit)mover).getConfig().rank == UnitConfig.Rank.BIGSHOT) ?
                +1 : -1;
    }

    @Override // documentation inherited
    public boolean getFenceBlocksGoal ()
    {
        return true;
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        // if we're corralled, stop moving
        if (corralled) {
            return null;
        }

        // if we're walled in on all three sides, we want to move
        int walls = 0, direction = -1;
        for (int dd = 0; dd < DIRECTIONS.length; dd++) {
            if (bangobj.board.isGroundOccupiable(x + DX[dd], y + DY[dd])) {
                // in the case that we're walled in on three sides, this will
                // only get assigned once, to the direction in which we are not
                // walled in
                direction = dd;
            } else {
                walls++;
            }
        }
        if (walls < 3 || direction == -1) {
            return null;
        }

        ArrayList<Effect> effects = new ArrayList<Effect>();
        effects.add(move(bangobj, direction, -1, -1));
        return effects;
    }

    protected SpookEffect move (BangObject bangobj, int direction,
                                int owner, int spookerId)
    {
        SpookEffect spook = new SpookEffect(owner, spookerId, direction);
        spook.init(this);
        return spook;
    }

    /** Used for temporary calculations. */
    protected static PointSet _moves = new PointSet();
}
