//
// $Id$

package com.samskivert.bang.data.generate;

import java.util.ArrayList;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.piece.Piece;

/**
 * Provides a framework for generating the pieces and goals that make up a
 * scenario (generally done after an environment is generated).
 */
public abstract class ScenarioGenerator
{
    /**
     * Instructs the generator to perform its generation, modifying the
     * supplied board and adding any created pieces to the supplied list.
     *
     * @param difficulty a number between 0 and 100 indicating the desired
     * difficulty of the generated scenario.
     */
    public abstract void generate (
        int difficulty, BangBoard board, ArrayList<Piece> pieces);
}
