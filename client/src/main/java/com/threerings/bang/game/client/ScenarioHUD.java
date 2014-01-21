//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BStyleSheet;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.BLayoutManager;

import com.threerings.bang.game.data.piece.Piece;

/**
 * A base class for scenario specific HUDs.
 */
public abstract class ScenarioHUD extends BWindow
{
    public ScenarioHUD (BStyleSheet style, BLayoutManager layout)
    {
        super(style, layout);
    }

    public abstract void pieceWasAffected (Piece piece, String effect);
}
