//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.util.PieceUtil;

import static com.threerings.bang.Log.log;

/**
 * A piece representing a prop.
 */
public class Prop extends BigPiece
{
    /** The fine x offset of this piece. */
    public byte fx;
    
    /** The fine y offset of this piece. */
    public byte fy;
    
    /** The fine orientation offset of this piece. */
    public byte forient;
    
    /** The fine elevation offset of this piece. */
    public byte felev;
    
    /** The pitch of the piece. */
    public byte pitch;
    
    /** The roll of the piece. */
    public byte roll;
    
    /**
     * Instantiates a prop of the specified type.
     */
    public static Prop getProp (String type)
    {
        PropConfig config = PropConfig.getConfig(type);
        Prop prop = null;
        try {
            if (config.propClass != null) {
                prop = (Prop)
                    Class.forName(config.propClass).newInstance();
            } else {
                prop = new Prop();
            }
            prop.init(config);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create prop [type=" + type +
                    ", class=" + config.propClass + "].", e);
        }
        return prop;
    }

    /** Returns the type of the prop. */
    public String getType ()
    {
        return _type;
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        init(PropConfig.getConfig(_type));
    }

    @Override // documentation inherited
    public void persistTo (ObjectOutputStream oout)
        throws IOException
    {
        super.persistTo(oout);
        oout.writeByte(fx);
        oout.writeByte(fy);
        oout.writeByte(forient);
        oout.writeByte(felev);
        oout.writeByte(pitch);
        oout.writeByte(roll);
    }
    
    @Override // documentation inherited
    public void unpersistFrom (ObjectInputStream oin)
        throws IOException
    {
        super.unpersistFrom(oin);
        fx = oin.readByte();
        fy = oin.readByte();
        forient = oin.readByte();
        felev = oin.readByte();
        pitch = oin.readByte();
        roll = oin.readByte();
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new PropSprite(_type);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer();
    }

    /**
     * Rotates this piece in fine units, which divide the 90 degree rotations
     * up by 256.
     *
     * @param amount the amount by which to rotate, where positive amounts
     * rotate counter-clockwise and negative amounts rotate clockwise
     */
    public void rotateFine (int amount)
    {
        forient = PieceUtil.rotateFine(this, forient, amount);
    }
    
    @Override // documentation inherited
    public boolean rotate (int direction)
    {
        super.rotate(direction);
        forient = pitch = roll = 0;
        return true;
    }
    
    /**
     * Positions this piece in fine coordinates, which divide the tile
     * coordinates up by 256.
     */
    public void positionFine (int px, int py)
    {
        position(px / 256, py / 256);
        fx = (byte)((px % 256) - 128);
        fy = (byte)((py % 256) - 128);
    }
    
    /**
     * Moves this piece up or down by the specified amount.
     */
    public void elevate (int amount)
    {
        felev = (byte)Math.min(Math.max(felev + amount, Byte.MIN_VALUE),
            Byte.MAX_VALUE);
    }
    
    @Override // documentation inherited
    protected void updatePosition (int nx, int ny)
    {
        super.updatePosition(nx, ny);
        fx = fy = felev = 0;
    }
    
    /**
     * Provides the prop with its configuration.
     */
    protected void init (PropConfig config)
    {
        _config = config;
        _type = config.type;
        _width = _config.width;
        _height = _config.height;
        recomputeBounds();
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append(_type).append(", ");
        super.toString(buf);
    }

    protected String _type;
    protected transient PropConfig _config;
}
