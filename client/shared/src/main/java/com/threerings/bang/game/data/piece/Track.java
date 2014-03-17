//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.ArrayList;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TrackSprite;
import com.threerings.bang.game.data.BangObject;

/**
 * A piece of track for the train.
 */
public class Track extends Piece
{
    /** An isolated piece of track. */
    public static final byte SINGLETON = 0;
    
    /** A place where the track begins or ends. */
    public static final byte TERMINAL = 1;
    
    /** A straight piece of track. */
    public static final byte STRAIGHT = 2;
    
    /** A T-junction. */
    public static final byte T_JUNCTION = 3;
    
    /** A cross junction. */
    public static final byte X_JUNCTION = 4;
    
    /** A turn. */
    public static final byte TURN = 5;
    
    /** The type of this track (singleton, terminal, etc.) */
    public byte type;
    
    /** Identifies the connected group to which this piece of track belongs. */
    public transient int group;
    
    /** Used on the server to record when this track was visited in a depth-first search. */
    public transient int visited;
    
    /**
     * Creates a piece of track with the default type, for use in the editor.
     */
    public Track ()
    {
    }
    
    /**
     * Creates a piece of track with the given type.
     */
    public Track (byte type)
    {
        this.type = type;
    }
    
    @Override // documentation inherited
    public float getHeight ()
    {
        return 0.0928f;
    }
    
    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return preventsGroundOverlap() && !(lapper instanceof Train) &&
            !lapper.isAirborne();
    }
    
    /**
     * Determines whether this track is a singleton or terminal, and thus
     * prevents ground vehicles from overlapping.
     */
    public boolean preventsGroundOverlap ()
    {
        return type == SINGLETON || type == TERMINAL;
    }
    
    /**
     * Returns an array containing the pieces of track adjacent to this one.
     */
    public Track[] getAdjacent (BangObject bangobj)
    {
        if (_adjacent == null) {
            ArrayList<Track> adjacent = new ArrayList<Track>();
            for (int ii = 0; ii < DIRECTIONS.length; ii++) {
                Track adj = bangobj.getTracks().get(
                    coord(x + DX[ii], y + DY[ii]));
                if (adj != null && isConnectedTo(adj.x, adj.y)) {
                    adjacent.add(adj);
                }
            }
            _adjacent = adjacent.toArray(new Track[adjacent.size()]);
        }
        return _adjacent;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TrackSprite();
    }
    
    @Override // documentation inherited
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        type = capsule.readByte("type", (byte)0);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(type, "type", (byte)0);
    }
    
    /**
     * Determines whether this piece of track can be assumed to be connected
     * to another piece of track at the specified coordinates based on this
     * track's type and orientation.
     */
    protected boolean isConnectedTo (int tx, int ty)
    {
        switch (type) {
            default:
            case SINGLETON:
                return false;
            case TERMINAL:
                return isInDirection(tx, ty, orientation);
            case STRAIGHT:
                return isInDirection(tx, ty, orientation) ||
                    isInDirection(tx, ty, (orientation + 2) % 4);
            case T_JUNCTION:
                return getDistance(tx, ty) == 1 &&
                    !isInDirection(tx, ty, orientation);
            case X_JUNCTION:
                return getDistance(tx, ty) == 1;
            case TURN:
                return isInDirection(tx, ty, orientation) ||
                    isInDirection(tx, ty, (orientation + 1) % 4);
        }
    }
    
    /**
     * Checks whether the specified position lies one step away in the given
     * position.
     */
    protected boolean isInDirection (int tx, int ty, int dir)
    {
        return tx == x + DX[dir] && ty == y + DY[dir];
    }
    
    @Override // documentation inherited
    protected int computeOrientation (int nx, int ny)
    {
        // our orientation never changes
        return orientation;
    }
    
    /** The lazily initialized list of adjacent sections of track. */
    protected transient Track[] _adjacent;
}
