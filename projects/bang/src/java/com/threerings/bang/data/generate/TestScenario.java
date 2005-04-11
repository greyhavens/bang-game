//
// $Id$

package com.threerings.bang.data.generate;

import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.piece.Artillery;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Marine;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.Tank;

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
        piece = new Marine();
        piece.position(0, 3);
        configureAndAdd(pieces, 0, piece);

        piece = new Artillery();
        piece.position(5, 0);
        configureAndAdd(pieces, 1, piece);

        piece = new Marine();
        piece.position(5, 3);
        configureAndAdd(pieces, 1, piece);

        piece = new Bonus(Bonus.Type.REPAIR);
        piece.position(0, 5);
        configureAndAdd(pieces, -1, piece);

        piece = new Bonus(Bonus.Type.DUPLICATE);
        piece.position(5, 5);
        configureAndAdd(pieces, -1, piece);
    }
}
