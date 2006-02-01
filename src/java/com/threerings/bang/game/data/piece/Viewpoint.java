//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ViewpointSprite;
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
    public void persistTo (ObjectOutputStream oout)
        throws IOException
    {
        super.persistTo(oout);
        oout.writeUTF(name);
        oout.writeByte(fx);
        oout.writeByte(fy);
        oout.writeByte(forient);
        oout.writeShort(pitch);
        oout.writeShort(elevation);
    }
    
    @Override // documentation inherited
    public void unpersistFrom (ObjectInputStream oin)
        throws IOException
    {
        super.unpersistFrom(oin);
        name = oin.readUTF();
        fx = oin.readByte();
        fy = oin.readByte();
        forient = oin.readByte();
        pitch = oin.readShort();
        elevation = oin.readShort();
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ViewpointSprite();
    }

    @Override // documentation inherited
    public void assignPieceId ()
    {
        super.assignPieceId();
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
}
