//
// $Id$

package com.samskivert.bang.data.generate;

import java.util.ArrayList;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.piece.Piece;

/**
 * Provides a framework for various routines to be combined to generate
 * random environments.
 */
public abstract class EnvironmentGenerator
{
    /**
     * Instructs the generator to create an environment, modifying the
     * supplied board and adding any created pieces to the supplied list.
     *
     * @param config the game configuration.
     */
    public abstract void generate (
        ToyBoxGameConfig config, BangBoard board, ArrayList<Piece> pieces);
}
