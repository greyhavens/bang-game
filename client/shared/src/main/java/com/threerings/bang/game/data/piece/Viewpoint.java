//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ViewpointSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.util.PieceUtil;

/**
 * A viewpoint used to position the camera.
 */
public class Viewpoint extends Piece
{
    /** The name of this viewpoint. */
    public String name;
    
    /** The fine x, y, and orientation offsets. */
    public byte fx, fy, forient;
    
    /** The pitch of the viewpoint. */
    public short pitch;
    
    /** The elevation of the viewpoint. */
    public short elevation;
    
    @Override // documentation inherited
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        return elevation;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ViewpointSprite();
    }

    @Override // documentation inherited
    public void assignPieceId (BangObject bangobj)
    {
        super.assignPieceId(bangobj);
        name = "viewpoint" + pieceId;
    }
    
    @Override // documentation inherited
    public String toString ()
    {
        return name;
    }
    
    /**
     * Rotates this piece in fine coordinates.
     *
     * @param dorient the fine orientation increment
     * @param dpitch the pitch increment
     */
    public void rotateFine (int dorient, int dpitch)
    {
        pitch = (short)Math.max(Math.min(pitch + dpitch, +255), -256);
        forient = PieceUtil.rotateFine(this, forient, dorient);
    }
    
    /**
     * Translates this piece in fine coordinates.
     *
     * @param dx the fine x increment
     * @param dy the fine y increment
     */
    public void translateFine (int dx, int dy)
    {
        int nfx = fx + dx, nfy = fy + dy;
        if (nfx < -128) {
            position(x - 1, y);
            nfx += 256;
            
        } else if (nfx > 127) {
            position(x + 1, y);
            nfx -= 256;
        }
        if (nfy < -128) {
            position(x, y - 1);
            nfy += 256;
            
        } else if (nfy > 127) {
            position(x, y + 1);
            nfy -= 256;
        }
        fx = (byte)nfx;
        fy = (byte)nfy;           
    }
    
    @Override // documentation inherited
    public boolean rotate (int direction)
    {
        super.rotate(direction);
        forient = 0;
        return true;
    }
    
    @Override // documentation inherited
    protected void updatePosition (int nx, int ny)
    {
        super.updatePosition(nx, ny);
        fx = fy = 0;
    }
    
    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation doesn't change with position
        return orientation;
    }

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
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "name", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "fx", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "fy", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "forient", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "pitch", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.Viewpoint.class, "elevation", this, ins);
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
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "name", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "fx", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "fy", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "forient", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "pitch", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.Viewpoint.class, "elevation", this, out);
    }
}
