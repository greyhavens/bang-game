//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.BangBoard;

/**
 * Handles the behavior of the wendigo in the wendigo attack scenario.
 */
public class Wendigo extends Piece
    implements PieceCodes
{
    public Wendigo ()
    {
    }

    public Wendigo (boolean claw)
    {
        _claw = claw;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new WendigoSprite(_claw);
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return 4;
    }

    @Override // documentation inherited
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        int z = board.getHeightfieldElevation(tx, ty);
        if (orientation == EAST || orientation == WEST) {
            ty++;
        } else {
            tx++;
        }
        return (z + board.getHeightfieldElevation(tx, ty)) / 2;
    }

    protected boolean _claw = false;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "owner", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "lastActed", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "x", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "y", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "orientation", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "damage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Piece.class, "scenId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Wendigo.class, "_claw", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "owner", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "lastActed", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "x", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "y", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "orientation", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "damage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Piece.class, "scenId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Wendigo.class, "_claw", this, out);
    }
}
