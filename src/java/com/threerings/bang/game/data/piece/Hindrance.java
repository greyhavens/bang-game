//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Represents a temporary hindrance on a unit.  Hindrances can adjust
 * a unit's various basic properties and they can expire after a certain
 * number of ticks.
 */
public abstract class Hindrance extends Influence
{
    /**
     * Create a specialized ShotEffect that will override the standard
     * Piece.shoot value;
     */
    public ShotEffect shoot (
            BangObject bangobj, Unit shooter, Piece target, float scale)
    {
        return null;
    }

    public Effect affectTarget (Piece target)
    {
        return null;
    }
}
