//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Duplicates a unit as a Gunslinger.
 */
public class DuplicateGunslingerEffect extends DuplicateEffect
{
    @Override // documentation inherited
    protected Unit duplicate (BangObject bangobj, Unit unit)
    {
        return unit.duplicate(bangobj, "frontier_town/gunslinger");
    }
}
