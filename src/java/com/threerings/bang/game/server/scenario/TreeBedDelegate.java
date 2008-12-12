//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.util.QuickSort;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ClearPieceEffect;
import com.threerings.bang.game.data.effect.PuntEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Manages tree beds for Forest Guardians.
 */
public class TreeBedDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        // collect and remove all the tree beds
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (p instanceof TreeBed) {
                _trees.add((TreeBed)p);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
    {
        // give the trees piece ids before we add any
        for (TreeBed tree : _trees) {
            tree.assignPieceId(bangobj);
        }
    }

    /**
     * Returns the set of trees in use for this wave.
     */
    public List<TreeBed> getWaveTrees ()
    {
        return _ctrees;
    }

    /**
     * Clears all the trees so they are not counted twice at the end of the game.
     */
    public void clearTrees ()
    {
        _ctrees.clear();
    }

    /**
     * Adds and removes trees such that the board contains a random subset of
     * them. Called prior to starting the game and at the end of each wave in
     * preparation for the next.
     */
    public void resetTrees (final BangObject bangobj, int difficulty)
    {
        // shuffle the trees and move any currently visible to the end
        Collections.shuffle(_trees);
        QuickSort.sort(_trees, new Comparator<TreeBed>() {
            public int compare (TreeBed t1, TreeBed t2) {
                boolean c1 = bangobj.pieces.contains(t1),
                    c2 = bangobj.pieces.contains(t2);
                return (c1 == c2) ? 0 : (c1 ? +1 : -1);
            }
        });

        // determine the desired number of trees and add/remove accordingly
        float ratio = BASE_TREE_RATIO + TREE_RATIO_INCREMENT * difficulty;
        int units = (_bangmgr.getTeamSize() + 1) * _bangmgr.getPlayerCount();
        int ntrees = Math.min(MAX_TREES, Math.round(units * ratio));
        _ctrees.clear();
        for (TreeBed tree : _trees) {
            if (ntrees-- > 0) {
                if (!bangobj.pieces.contains(tree)) {
                    addTree(bangobj, tree);
                }
                // reset and retrieve the cloned instance
                _bangmgr.deployEffect(-1, new TreeBedEffect(tree));
                _ctrees.add((TreeBed)bangobj.pieces.get(tree.pieceId));

            } else {
                if (bangobj.pieces.contains(tree)) {
                    _bangmgr.deployEffect(-1, new ClearPieceEffect(tree));
                }
            }
        }
    }

    /**
     * Adds a tree (back) to the board, moving any unit or bonus occupying its
     * space.
     */
    protected void addTree (BangObject bangobj, TreeBed tree)
    {
        if (!bangobj.board.isOccupiable(tree.x, tree.y) ||
            bangobj.board.containsBonus(tree.x, tree.y)) {
            for (Piece piece : bangobj.pieces) {
                if (!piece.intersects(tree)) {
                    continue;
                }
                if (piece instanceof Unit) {
                    Point spot = bangobj.board.getOccupiableSpot(
                        tree.x, tree.y, 3);
                    if (spot != null) {
                        _bangmgr.deployEffect(-1,
                            ((Unit)piece).generateMoveEffect(
                                bangobj, spot.x, spot.y, null));
                    } else {
                        log.warning("Unable to find spot to move unit", "unit", piece);
                    }
                } else if (piece instanceof Bonus) {
                    PuntEffect effect = PuntEffect.puntBonus(
                        bangobj, (Bonus)piece, -1);
                    if (effect != null) {
                        _bangmgr.deployEffect(-1, effect);
                    }
                }
            }
        }
        tree.init();
        _bangmgr.addPiece(tree);
    }

    /** The tree beds on the board to begin with. */
    protected List<TreeBed> _trees = Lists.newArrayList();

    /** The trees that are currently on the board. */
    protected List<TreeBed> _ctrees = Lists.newArrayList();

    /** The maximum number of trees on the board. */
    protected static final int MAX_TREES = 6;

    /** The base number of tree beds to create per unit. */
    protected static final float BASE_TREE_RATIO = 1 / 3f;

    /** The increment in number of trees per unit for each wave. */
    protected static final float TREE_RATIO_INCREMENT = 1 / 24f;
}
