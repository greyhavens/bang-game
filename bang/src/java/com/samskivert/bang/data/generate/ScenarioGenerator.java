//
// $Id$

package com.samskivert.bang.data.generate;

import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

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
     * @param config the game configuration.
     */
    public abstract void generate (
        ToyBoxGameConfig config, BangBoard board, ArrayList<Piece> pieces);
}
