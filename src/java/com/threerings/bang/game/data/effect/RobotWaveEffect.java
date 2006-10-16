//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.RobotWaveHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;

import static com.threerings.bang.Log.*;

/**
 * An effect that signals the beginning or end of a wave of robots.
 */
public class RobotWaveEffect extends Effect
{
    /** Indicates that a tree was counted towards the wave score. */
    public static final String TREE_COUNTED = "indian_post/tree_bed/counted";
    
    /** The number of the wave beginning, or -1 if the wave is ending. */
    public int wave = -1;
    
    /** The difficulty level of the wave. */
    public int difficulty = -1;
    
    /** The piece ids of the trees that will be sprouted at the wave's end. */
    public int[] treeIds = NO_PIECES;
    
    /** The piece ids of the robots that will be cleared at the wave's end. */
    public int[] botIds = NO_PIECES;
    
    /** After applying an end-of-wave effect, this will contain the number of
     * living trees. */
    public transient int living;
    
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
    public RobotWaveEffect ()
    {
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return concatenate(treeIds, botIds);
    }
    
    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle[] { bangobj.board.getPlayableArea() };
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (wave > 0) {
            return;
        }
        
        // find the ids of all trees and logging robots
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof TreeBed) {
                treeIds = ArrayUtil.append(treeIds, piece.pieceId);
            } else if (piece instanceof LoggingRobot) {
                botIds = ArrayUtil.append(botIds, piece.pieceId);
            }
        }
    }

    // documentation inherited    
    public boolean apply (BangObject bangobj, Observer observer)
    {
        if (wave > 0) {
            return true;
        }
        
        // count the living trees
        for (int treeId : treeIds) {
            TreeBed tree = (TreeBed)bangobj.pieces.get(treeId);
            if (tree == null) {
                log.warning("Missing tree bed to reset [pieceId=" +
                    treeId + "].");
                continue;
            }
            if (tree.isAlive()) {
                living++;
                reportEffect(observer, tree, TREE_COUNTED);
            }
        }
        
        // clear all logging robots
        for (int botId : botIds) {
            LoggingRobot bot = (LoggingRobot)bangobj.pieces.get(botId);
            if (bot == null) {
                log.warning("Missing robot to clear [pieceId=" +
                    botId + "].");
                continue;
            }
            removeAndReport(bangobj, bot, observer);
        }
        
        return true;
    }
    
    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new RobotWaveHandler();
    }
}
