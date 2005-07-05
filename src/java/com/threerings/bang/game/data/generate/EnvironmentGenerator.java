//
// $Id$

package com.threerings.bang.game.data.generate;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Provides a framework for various routines to be combined to generate
 * random environments.
 */
public abstract class EnvironmentGenerator
{
    /**
     * Instructs the generator to create an environment, modifying the
     * supplied board and adding any created pieces to the supplied list.
     */
    public abstract void generate (BangBoard board, ArrayList<Piece> pieces);
}
