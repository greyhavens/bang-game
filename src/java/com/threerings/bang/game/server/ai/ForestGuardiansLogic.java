//
// $Id$

package com.threerings.bang.game.server.ai;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;

/**
 * A simple AI for the forest guardians scenario.
 */
public class ForestGuardiansLogic extends AILogic
{
    @Override // documentation inherited
    public String getBigShotType ()
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.BIGSHOT);
        return getWeightedUnitTypes(configs, UNIT_EVALUATOR, 1)[0];
    }
    
    @Override // documentation inherited
    public String[] getUnitTypes (int count)
    {
        UnitConfig[] configs = UnitConfig.getTownUnits(_bangobj.townId,
            UnitConfig.Rank.NORMAL);
        return getWeightedUnitTypes(configs, UNIT_EVALUATOR, count);
    }

    // documentation inherited
    protected void moveUnit (
        Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // search for closest growing tree, closest logging robot
        TreeBed ctree = null;
        Piece cbot = null;
        for (Piece piece : pieces) {
            if (piece instanceof TreeBed) {
                TreeBed tree = (TreeBed)piece;
                if (tree.isAlive() && tree.damage != 0 &&
                    (ctree == null || unit.getDistance(tree) <
                        unit.getDistance(ctree))) {
                    ctree = tree;
                }
            } else if (piece instanceof Unit && piece.owner == -1 &&
                piece.isAlive() && (cbot == null || unit.getDistance(piece) <
                    unit.getDistance(cbot))) {
                cbot = piece;
            }
        }
        
        // if there's a tree that needs growing, go to it
        if (ctree != null && moveUnit(pieces, unit, moves, ctree.x, ctree.y,
                TARGET_EVALUATOR)) {
            return;
        
        // if there's a logging robot in range, shoot it
        } else if (cbot != null && attacks.contains(cbot.x, cbot.y)) {
            executeOrder(unit, Short.MAX_VALUE, 0, cbot);
        
        // if there's a logging robot at all, move towards it
        } else if (cbot != null) {
            moveUnit(pieces, unit, moves, cbot.x, cbot.y, TARGET_EVALUATOR);
        }
    }
    
    /** Prefers air units (because they're immune to the logging robots'
     * melee attack), higher fire distance, greater damage. */
    protected static final UnitConfigEvaluator UNIT_EVALUATOR =
        new UnitConfigEvaluator() {
        public int getWeight (UnitConfig config) {
            return (config.mode == UnitConfig.Mode.AIR ? 100 : 0) +
                config.maxFireDistance*10 + config.damage;
        }
    };
    
    /** Only targets logging robots.  Prefers ones with more damage. */
    protected static final TargetEvaluator TARGET_EVALUATOR =
        new TargetEvaluator() {
        public int getWeight (BangObject bangobj, Unit unit, Piece target, 
                int dist, PointSet preferredMoves) {
            return (target.owner == -1) ? (1 + target.damage) : -1;
        }
    };
}
