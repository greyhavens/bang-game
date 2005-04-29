//
// $Id$

package com.threerings.bang.data.generate;

import java.util.ArrayList;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.piece.Piece;

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
        BangConfig config, BangBoard board, ArrayList<Piece> pieces);
}
