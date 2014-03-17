//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;

import com.jme.math.Vector3f;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;

import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.scenario.TutorialInfo;
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

    /** The scale factors of the piece. */
    public short scalex, scaley, scalez;

    /**
     * Instantiates a prop of the specified type.
     */
    public static Prop getProp (String type)
    {
        // TEMP: LEGACY
        if (type.startsWith("buildings/") &&
            !type.startsWith("buildings/frontier") &&
            !type.startsWith("buildings/indian")) {
            type = type.substring(0, 10) + "frontier_town/" +
                type.substring(10);
        }
        // END TEMP
        PropConfig config = PropConfig.getConfig(type);
        if (config == null) {
            log.warning("Requested non-existent prop", "type", type);
            return null;
        }
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
            log.warning("Failed to create prop", "type", type, "class", config.propClass, e);
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
    public Object clone ()
    {
        // make a deep copy of the sbounds and vscale
        Prop piece = (Prop)super.clone();
        piece._sbounds = (Rectangle)_sbounds.clone();
        piece._vscale = (Vector3f)_vscale.clone();
        return piece;
    }

    @Override // documentation inherited
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        if (isOwnerConfigurable()) {
            owner = capsule.readInt("owner", -1);
        }
        fx = capsule.readByte("fx", (byte)0);
        fy = capsule.readByte("fy", (byte)0);
        forient = capsule.readByte("forient", (byte)0);
        felev = capsule.readByte("felev", (byte)0);
        pitch = capsule.readByte("pitch", (byte)0);
        roll = capsule.readByte("roll", (byte)0);
        scalex = capsule.readShort("scalex", (short)0);
        scaley = capsule.readShort("scaley", (short)0);
        scalez = capsule.readShort("scalez", (short)0);
        init(PropConfig.getConfig(capsule.readString("type", null)));
    }

    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        if (isOwnerConfigurable()) {
            capsule.write(owner, "owner", -1);
        }
        capsule.write(fx, "fx", (byte)0);
        capsule.write(fy, "fy", (byte)0);
        capsule.write(forient, "forient", (byte)0);
        capsule.write(felev, "felev", (byte)0);
        capsule.write(pitch, "pitch", (byte)0);
        capsule.write(roll, "roll", (byte)0);
        capsule.write(scalex, "scalex", (short)0);
        capsule.write(scaley, "scaley", (short)0);
        capsule.write(scalez, "scalez", (short)0);
        capsule.write(_type, "type", null);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new PropSprite(_type);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return isTall() || (!isPassable() && !lapper.isAirborne());
    }

    @Override // documentation inherited
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        return super.computeElevation(board, tx, ty) + felev;
    }

    @Override // documentation inherited
    public float getHeight ()
    {
        return _config.height;
    }

    /**
     * The height of a passable prop for calculating terrain elevation.
     */
    public float getPassHeight ()
    {
        return _config.passElev;
    }

    @Override // documentation inherited
    public boolean isTall ()
    {
        return _config.tall;
    }

    @Override // documentation inherited
    public boolean isPenetrable ()
    {
        return _config.penetrable;
    }

    /**
     * Determines whether this prop can be omitted from the board view in low
     * detail modes (i.e., it has no effect on gameplay).
     */
    public boolean isOmissible (BangBoard board)
    {
        return !intersects(board.getPlayableArea()) ||
            (isPassable() && _config.passElev == 0f &&
                StringUtil.isBlank(_config.blockDir));
    }

    /**
     * Determines whether this prop performs a function in the game beyond
     * simply contributing to the board's passability and height maps.
     * If not, the piece can be omitted from the distributed piece set.
     */
    public boolean isInteractive ()
    {
        // derived classes are assumed to be interactive
        return getClass() != Prop.class;
    }

    /**
     * Determines whether this prop is passable: that is, whether units can
     * occupy its location.
     */
    public boolean isPassable ()
    {
        return _config.passable;
    }

    /**
     * Determines if this piece should be treated as a bonus occupying a tile.
     */
    public boolean shadowBonus ()
    {
        return false;
    }

    @Override // documentation inherited
    public boolean rebuildShadow ()
    {
        return true;
    }

    /**
     * Returns true if it's possible to enter this piece from the direction.
     */
    public boolean canEnter (int dir, int x, int y)
    {
        int pdir = (4 + dir - orientation) % 4;
        if (getBounds().contains(new Point(x + DX[dir], y + DY[dir]))) {
            return true;
        }
        return _config.blockDir.indexOf(ENTER_DIR[pdir]) == -1;
    }

    /**
     * Returns true if it's possible to exit this piece from the direction.
     */
    public boolean canExit (int dir, int x, int y)
    {
        int pdir = (4 + dir - orientation) % 4;
        if (getBounds().contains(new Point(x + DX[dir], y + DY[dir]))) {
            return true;
        }
        return _config.blockDir.indexOf(EXIT_DIR[pdir]) == -1;
    }

    /**
     * Determines whether the editor should allow users to place versions of
     * this prop owned by the various players.
     */
    public boolean isOwnerConfigurable ()
    {
        return false;
    }

    @Override // documentation inherited
    public boolean isValidScenario (String scenarioId)
    {
        return super.isValidScenario(scenarioId) &&
            (scenarioId == null || _config.scenario == null ||
             TutorialInfo.IDENT.equals(scenarioId) ||
             _config.scenario.equals(scenarioId));
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
     * Scales the prop.  (-32768 - 0) is 0f - 1f and (0 - 32767) is 1f - 10f
     */
    public void scale (short x, short y, short z)
    {
        scalex = x;
        scaley = y;
        scalez = z;
        recomputeScale();
        recomputeBounds();
    }

    /**
     * Scales the prop.
     */
    public void scale (Vector3f v)
    {
        v.x = Math.max(0f, Math.min(v.x, 10f));
        v.y = Math.max(0f, Math.min(v.y, 10f));
        v.z = Math.max(0f, Math.min(v.z, 10f));
        scale(ftos(v.x), ftos(v.y), ftos(v.z));
    }

    /**
     * Returns a vector3f version of the scale.
     */
    public Vector3f getScale ()
    {
        if (_vscale == null) {
            recomputeScale();
        }
        return _vscale;
    }

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        return _sbounds;
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
    public boolean intersects (Rectangle bounds)
    {
        return _sbounds.intersects(bounds);
    }

    @Override // documentation inherited
    public boolean intersects (int tx, int ty)
    {
        return _sbounds.contains(tx, ty);
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
        _length = _config.length;
        recomputeScale();
        recomputeBounds();
    }

    @Override // documentation inherited
    protected void recomputeBounds ()
    {
        super.recomputeBounds();
        if (orientation == NORTH || orientation == SOUTH) {
            int sx = (int)(_vscale.x * _width - _width) / 2;
            int sy = (int)(_vscale.y * _length - _length) / 2;
            _sbounds.setBounds(x - sx, y - sy, _width + sx*2, _length + sy*2);
        } else {
            int sx = (int)(_vscale.y * _length - _length) / 2;
            int sy = (int)(_vscale.x * _width - _width) / 2;
            _sbounds.setBounds(x - sx, y - sy, _length + sx*2, _width + sy*2);
        }
    }

    @Override // documentation inherited
    protected String infoType ()
    {
        return _type;
    }

    /** Converts a float scale value to a short scale value. */
    protected short ftos (float f)
    {
        return (short)(f < 1f ? f * 32767 - 32767 : (f - 1) * 32767 / 9);
    }

    /** Converts a short scale value to a float scale value. */
    protected float stof (short s)
    {
        return (s < 0) ? ((32767f + s) / 32767f) :
                             (1f + 9f * s / 32767f);
    }

    /** Recomputes the scale vector. */
    protected void recomputeScale ()
    {
        _vscale = new Vector3f(stof(scalex), stof(scaley), stof(scalez));
    }

    protected String _type;
    protected transient PropConfig _config;
    protected transient Vector3f _vscale;
    protected transient Rectangle _sbounds = new Rectangle();

    protected static final String[] ENTER_DIR = {"N", "E", "S", "W"};
    protected static final String[] EXIT_DIR = {"n", "e", "s", "w"};
}
