//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.effect.RepairEffect;

/**
 * A bonus that repairs the piece that picks it up.
 */
public class Repair extends Bonus
{
    @Override // documentation inherited
    public Effect affect (Piece other)
    {
        // simply repair this piece's damage
        return new RepairEffect(other.pieceId);
    }
}
