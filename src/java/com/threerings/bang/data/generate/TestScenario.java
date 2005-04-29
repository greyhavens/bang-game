//
// $Id$

package com.threerings.bang.data.generate;

import java.util.ArrayList;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.piece.Artillery;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Gunslinger;
import com.threerings.bang.data.piece.Piece;

/**
 * Generates a quick test scenario.
 */
public class TestScenario extends ScenarioGenerator
{
    @Override // documentation inherited
    public void generate (
        BangConfig config, BangBoard board, ArrayList<Piece> pieces)
    {
        Piece piece = new Artillery();
        piece.position(0, 0);
        configureAndAdd(pieces, 0, piece);
        piece = new Gunslinger();
        piece.position(0, 3);
        configureAndAdd(pieces, 0, piece);

        piece = new Artillery();
        piece.position(5, 0);
        configureAndAdd(pieces, 1, piece);

        piece = new Gunslinger();
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
