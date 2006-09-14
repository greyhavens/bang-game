//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Toggles safe zone activation in wendigo attack.
 */
public class ToggleSwitch extends Prop
{
    public static enum State { SQUARE, CIRCLE };

    public int occupier = -1;
    public int activator = -1;

    public State state;

    public boolean isActive (short tick)
    {
        return (occupier == -1 && activator == -1 && 
                ticksUntilMovable(tick) == 0);
    }
}
