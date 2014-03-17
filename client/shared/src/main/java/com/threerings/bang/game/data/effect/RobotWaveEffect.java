//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.RobotWaveHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;

import static com.threerings.bang.Log.*;

/**
 * An effect that signals the beginning or end of a wave of robots.
 */
public class RobotWaveEffect extends Effect
{
    /** The maximum wave performance level (the minimum is zero). */
    public static final int MAX_PERFORMANCE = 4;
    
    /** The number of the wave beginning or ending. */
    public int wave;
    
    /** The difficulty level of the wave beginning, or -1 if ending. */
    public int difficulty = -1;
    
    /** The piece ids of the trees that will be sprouted at the wave's end. */
    public int[] treeIds = NO_PIECES;
    
    /** After applying an end-of-wave effect, this will contain the number of
     * living trees. */
    public transient int living;
    
    /**
     * Returns a performance level for the described wave (between zero and
     * {@link #MAX_PERFORMANCE}, inclusive).
     */
    public static int getPerformance (int living, int total)
    {
        return living * MAX_PERFORMANCE / total;
    }
    
    /**
     * No-arg constructor for deserialization.
     */
    public RobotWaveEffect ()
    {
    }
    
    /**
     * Creates an effect representing the start of a wave.
     */
    public RobotWaveEffect (int wave, int difficulty)
    {
        this.wave = wave;
        this.difficulty = difficulty;
    }
    
    /**
     * Creates an effect representing the end of a wave.
     */
    public RobotWaveEffect (int wave)
    {
        this.wave = wave;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return treeIds;
    }
    
    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle[] { bangobj.board.getPlayableArea() };
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (difficulty >= 0) {
            return;
        }
        
        // find the ids of all trees
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof TreeBed) {
                treeIds = ArrayUtil.append(treeIds, piece.pieceId);
            }
        }
    }

    // documentation inherited    
    public boolean apply (BangObject bangobj, Observer observer)
    {
        if (difficulty >= 0) {
            return true;
        }
        
        // count the living trees
        for (int treeId : treeIds) {
            TreeBed tree = (TreeBed)bangobj.pieces.get(treeId);
            if (tree == null) {
                log.warning("Missing tree bed to reset", "pieceId", treeId);
                continue;
            }
            if (tree.isAlive()) {
                living++;
            }
        }
        
        return true;
    }
    
    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new RobotWaveHandler();
    }

    /**
     * Returns a performance level for the wave completed, which will be
     * between zero and {@link #MAX_PERFORMANCE}, inclusive.  This method
     * will return a valid result only after the effect has been applied.
     */
    public int getPerformance ()
    {
        return getPerformance(living, treeIds.length);
    }
}
