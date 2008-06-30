//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Displays homesteads for the land grab scenario.
 */
public class HomesteadSprite extends TargetableActiveSprite
{
    public HomesteadSprite ()
    {
        super("props", "frontier_town/special/homestead");
    }

    @Override // documentation inherited
    public Coloring getColoringType () {
        return Coloring.DYNAMIC;
    }

    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        int oowner = _powner;
        super.updated(piece, tick);
        _target.updated(piece, tick);

        // build it up or tear it down
        if (oowner != piece.owner) {
            queueAction(oowner == -1 ? "unclaimed_build" : "claimed_dying");
        }
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        String prefix = (pidx == _powner) ?
            "own" : (pidx == -1 ? "unclaimed" : "other");
        return prefix + "_frontier_town/special/homestead";
    }

    @Override // documentation inherited
    protected String[] getIdleAnimations ()
    {
        return new String[] { (_powner == -1 ? "un" : "") + "claimed_idle" };
    }
}
