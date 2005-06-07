//
// $Id$

package com.threerings.bang.data.generate;

import java.util.ArrayList;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.Unit;

/**
 * Generates a quick test scenario.
 */
public class TestScenario extends ScenarioGenerator
{
    @Override // documentation inherited
    public void generate (
        BangConfig config, BangBoard board, ArrayList<Piece> pieces)
    {
        Piece piece = Unit.getUnit("artillery");
        piece.position(0, 0);
        configureAndAdd(pieces, 0, piece);
        piece = Unit.getUnit("gunslinger");
        piece.position(0, 3);
        configureAndAdd(pieces, 0, piece);

        piece = Unit.getUnit("artillery");
        piece.position(5, 0);
        configureAndAdd(pieces, 1, piece);

        piece = Unit.getUnit("gunslinger");
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
