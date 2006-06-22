//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.effect.Effect;

/**
 * Represents a temporary hinderance on a unit.  Hinerances can adjust
 * a unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Hinderance extends Influence
{
    public Effect affectTarget (Piece target)
    {
        return null;
    }
}
