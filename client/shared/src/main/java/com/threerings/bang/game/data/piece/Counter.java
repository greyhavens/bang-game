//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.CounterSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A prop that maintains a (displayable) count.
 */
public class Counter extends Prop
{
    /** The count. */
    public int count;

    @Override // documentation inherited
    public boolean isOmissible (BangBoard board)
    {
        return false;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new CounterSprite(_type);
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Counter.class, "count", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Counter.class, "count", this, out);
    }
}
