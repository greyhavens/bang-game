//
// $Id$

package com.samskivert.bang.data.generate;

import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.piece.Artillery;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.Tank;

/**
 * Generates a quick test scenario.
 */
public class TestScenario extends ScenarioGenerator
{
    @Override // documentation inherited
    public void generate (
        ToyBoxGameConfig config, BangBoard board, ArrayList<Piece> pieces)
    {
        Piece piece = new Artillery();
        piece.position(0, 0);
        configureAndAdd(pieces, 0, piece);
        piece = new Tank();
        piece.position(0, 3);
        configureAndAdd(pieces, 0, piece);

        piece = new Artillery();
        piece.position(5, 0);
        configureAndAdd(pieces, 1, piece);

        piece = new Tank();
        piece.position(5, 3);
        configureAndAdd(pieces, 1, piece);
    }
}
