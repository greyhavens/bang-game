//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.effect.Effect;

/**
 * Represents a temporary hindrance on a unit.  Hindrances can adjust
 * a unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Hindrance extends Influence
{
    public Effect affectTarget (Piece target)
    {
        return null;
    }
}
