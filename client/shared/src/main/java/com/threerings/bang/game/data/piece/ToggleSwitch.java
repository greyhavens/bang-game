//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.ToggleSwitchSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

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

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ToggleSwitchSprite();
    }

    @Override // documentation inherited
    public boolean updateSpriteOnTick ()
    {
        return true;
    }

    @Override // documentation inherited
    public void init ()
    {
        super.init();
        lastActed = (short)(-1 * getTicksPerMove());
        state = State.SQUARE;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "occupier", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "activator", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "state", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "occupier", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "activator", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.ToggleSwitch.class, "state", this, out);
    }
}
