//
// $Id$

package com.threerings.bang.data.generate;

import java.awt.Rectangle;
import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.piece.Artillery;
import com.threerings.bang.data.piece.Chopper;
import com.threerings.bang.data.piece.Marine;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.Tank;

/**
 * Generates the pieces for our test skirmish scenario.
 */
public class SkirmishScenario extends ScenarioGenerator
{
    @Override // documentation inherited
    public void generate (
        ToyBoxGameConfig config, BangBoard board, ArrayList<Piece> pieces)
    {
        // each player starts in their own corner
        for (int ii = 0; ii < config.players.length; ii++) {
            int sx = (ii % 2 == 0) ? 0 : board.getWidth()-1;
            int sy = (ii == 0 || ii == 3) ? 0 : board.getHeight()-1;
            placePlayer(board, pieces, ii, sx, sy);
        }
    }

    protected void placePlayer (
        BangBoard board, ArrayList<Piece> pieces, int pidx, int sx, int sy)
    {
        // search out from the corner for a valid position
        ArrayList<Piece> placers = new ArrayList<Piece>();
        configureAndAdd(placers, pidx, new Artillery());
        configureAndAdd(placers, pidx, new Chopper());
        configureAndAdd(placers, pidx, new Marine());
        configureAndAdd(placers, pidx, new Marine());
        configureAndAdd(placers, pidx, new Tank());
        configureAndAdd(placers, pidx, new Tank());

        Piece piece = placers.remove(0);
        Rectangle rect = new Rectangle(sx, sy, 1, 1);
        for (int gg = 0; gg < 10; gg++) {
            int xx, yy, lx, ly;

            xx = rect.x;
            for (yy = rect.y, ly = rect.y + rect.height; yy < ly; yy++) {
                if (!tryPlacement(board, pieces, xx, yy, piece)) {
                    continue;
                }
                if (placers.size() == 0) {
                    return;
                } else {
                    piece = placers.remove(0);
                }
            }

            xx = rect.x + rect.width - 1;
            for (yy = rect.y, ly = rect.y + rect.height; yy < ly; yy++) {
                if (!tryPlacement(board, pieces, xx, yy, piece)) {
                    continue;
                }
                if (placers.size() == 0) {
                    return;
                } else {
                    piece = placers.remove(0);
                }
            }

            yy = rect.y;
            for (xx = rect.x + 1, lx = rect.x + rect.width - 2; xx < lx; xx++) {
                if (!tryPlacement(board, pieces, xx, yy, piece)) {
                    continue;
                }
                if (placers.size() == 0) {
                    return;
                } else {
                    piece = placers.remove(0);
                }
            }

            yy = rect.y + rect.height - 1;
            for (xx = rect.x + 1, lx = rect.x + rect.width - 2; xx < lx; xx++) {
                if (!tryPlacement(board, pieces, xx, yy, piece)) {
                    continue;
                }
                if (placers.size() == 0) {
                    return;
                } else {
                    piece = placers.remove(0);
                }
            }

            rect.grow(1, 1);
        }
    }
}
