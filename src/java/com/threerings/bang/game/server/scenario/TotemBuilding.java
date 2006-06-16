//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;

import com.threerings.presents.server.InvocationException;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Players all have a totem base on the board.
 * <li> Totem pieces are generated and scattered randomly around the board.
 * <li> Any player unit can pick up a totem piece and carry it back to their
 * totem base, adding the piece to their totem pole.
 * <li> Players can attack an opponents totem, which has 100 health, and
 * destroy the top piece on the pole.  (Which resets the health to 100).
 * <li> Near the end of the scenario, top totem pieces are generated.
 * <li> The round ends after a top piece is added to a pole, or after a
 * fixed time limit.
 * <li> Points are granted for each totem piece on a players pole, with
 * duplicate pieces counted at half value.
 * </ul>
 */
public class TotemBuilding extends Scenario
{
    /**
     * Creates a totem building scenario and registers its delegates.
     */
    public TotemBuilding ()
    {
        registerDelegate(new RespawnDelegate());
        registerDelegate(new TotemBaseDelegate());
    }

    protected class TotemBaseDelegate extends ScenarioDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
            throws InvocationException
        {
            ArrayIntSet assigned = new ArrayIntSet();
            Piece[] pieces = bangobj.getPieceArray();
            for (int ii = 0; ii < pieces.length; ii++) {
                if (!(pieces[ii] instanceof TotemBase)) {
                    continue;
                }

                // determine which start marker to which it is nearest
                TotemBase base = (TotemBase)pieces[ii];
                int midx = _parent.getOwner(base);
                if (midx == -1 || assigned.contains(midx)) {
                    throw new InvocationException("m.no_start_marker_for_base");
                }

                // make sure we have a player associated with this start marker
                if (midx >= bangobj.players.length) {
                    continue;
                }

                // configure this totem base for play
                base.owner = midx;
                bangobj.updatePieces(base);
                _bases.add(base);
                assigned.add(midx);
            }
        }
        
        /** A list of active totem bases. */
        protected ArrayList<TotemBase> _bases = new ArrayList<TotemBase>();
    }
          
}
