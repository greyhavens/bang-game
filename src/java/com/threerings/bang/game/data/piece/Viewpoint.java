//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

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
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        name = capsule.readString("name", null);
        fx = capsule.readByte("fx", (byte)0);
        fy = capsule.readByte("fy", (byte)0);
        forient = capsule.readByte("forient", (byte)0);
        pitch = capsule.readShort("pitch", (short)0);
        elevation = capsule.readShort("elevation", (short)0);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(name, "name", null);
        capsule.write(fx, "fx", (byte)0);
        capsule.write(fy, "fy", (byte)0);
        capsule.write(forient, "forient", (byte)0);
        capsule.write(pitch, "pitch", (short)0);
        capsule.write(elevation, "elevation", (short)0);
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
}
