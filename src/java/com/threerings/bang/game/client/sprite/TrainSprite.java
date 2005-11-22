//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.Path;

import com.threerings.bang.client.Config;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Train;

/**
 * Displays a train piece.
 */
public class TrainSprite extends MobileSprite
{
    public TrainSprite (byte type)
    {
        super("extras/train", TYPE_NAMES[type]);
    }
    
    @Override // documentation inherited
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        Train otrain = (Train)opiece, ntrain = (Train)npiece;
        boolean last = (otrain.lastX != Train.UNSET),
            next = (ntrain.nextX != Train.UNSET);
        int ncoords = 2 + (last ? 1 : 0) + (next ? 1 : 0), idx = 0;
        Vector3f[] coords = new Vector3f[ncoords];
        float[] durations = new float[ncoords - 1];
        
        if (last) {
            setCoord(board, coords, idx++, otrain.lastX, otrain.lastY);
        }
        durations[idx] = 1f / Config.display.getMovementSpeed();
        setCoord(board, coords, idx++, otrain.x, otrain.y);
        setCoord(board, coords, idx++, ntrain.x, ntrain.y);
        if (next) {
            setCoord(board, coords, idx, ntrain.nextX, ntrain.nextY);
        }
        return new TrainPath(coords, durations, last);
    }

    @Override // documentation inherited
    protected void reorient ()
    {
        // don't do it; whatever the path left us at is good
    }
    
    /** A special path class for trains that incorporates the previous and next
     * positions. */
    protected class TrainPath extends MoveUnitPath
    {
        public TrainPath (Vector3f[] coords, float[] durations, boolean last)
        {
            super(TrainSprite.this, coords, durations);
            if (last) {
                advance();
            }
        }
        
        @Override // documentation inherited
        public void update (float time)
        {
            // bail out (without changing the position) after the first
            // leg
            float naccum = _accum + time;
            if (naccum > _durations[_current]) {
                _sprite.pathCompleted();
                return;
                
            } else {
                super.update(time);
            }
        }
    }
    
    /** The model names for each train type. */    
    protected static final String[] TYPE_NAMES = { "locomotive", "cattle",
        "cattle", "freight" };
}
