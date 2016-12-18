//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.client.Config;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Train;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a train piece.
 */
public class TrainSprite extends MobileSprite
{
    public TrainSprite (byte type)
    {
        super("extras/frontier_town/train", TYPE_NAMES[type]);
    }

    @Override // documentation inherited
    public Coloring getColoringType ()
    {
        return Coloring.DYNAMIC;
    }

    @Override // documentation inherited
    public boolean updatePosition (BangBoard board)
    {
        super.updatePosition(board);

        // store our last x and y for path forming
        Train train = (Train)_piece;
        _lastLastX = train.lastX;
        _lastLastY = train.lastY;

        return isMoving();
    }

    @Override // documentation inherited
    protected void createDustManager ()
    {
        // trains do not kick up dust
    }

    @Override // documentation inherited
    protected void moveSprite (BangBoard board)
    {
        if (!_fastAnimation) {
            super.moveSprite(board);
            return;
        }

        // Special handling for fast animated trains
        if (!isMoving()) {
            Train train = (Train)_piece;
            setLocation(board, train.x, train.y);

            // figure out the proper rotation
            Vector3f temp = new Vector3f();
            Vector3f first = new Vector3f(train.lastX, train.lastY, 0f);
            Vector3f second = (train.nextX != Train.UNSET ?
                        new Vector3f(train.nextX, train.nextY, 0f) :
                        new Vector3f(train.x, train.y, 0f));
            second.subtract(first, temp);
            Quaternion rotate = new Quaternion();
            PathUtil.computeRotation(UP, FORWARD, temp, rotate);
            setLocalRotation(rotate);
        }
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board)
    {
        Train train = (Train)_piece;
        boolean last = (_lastLastX != Train.UNSET);
        boolean next = (train.nextX != Train.UNSET);
        int ncoords = 2 + (last ? 1 : 0) + (next ? 1 : 0), idx = 0;
        Vector3f[] coords = new Vector3f[ncoords];
        float[] durations = new float[ncoords - 1];

        if (last) {
            setCoord(board, coords, idx++, _lastLastX, _lastLastY, false);
        }
        durations[idx] = 1f / Config.getMovementSpeed();
        setCoord(board, coords, idx++, train.lastX, train.lastY, false);
        setCoord(board, coords, idx++, train.x, train.y, false);
        if (next) {
            setCoord(board, coords, idx, train.nextX, train.nextY, false);
        }
        return new TrainPath(coords, durations, last);
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
            // bail out after the first leg
            float naccum = _accum + time;
            boolean completed = false;
            if (naccum > _durations[_current]) {
                time += (_durations[_current] - naccum);
                completed = true;
            }
            super.update(time);
            if (completed && _sprite.isMoving()) {
                _sprite.pathCompleted();
            }
        }
    }

    /** The position two ticks back, used to form curves. */
    protected short _lastLastX = Train.UNSET, _lastLastY = Train.UNSET;

    /** The owner corresponding to the current colorizations. */
    protected int _owner = -1;

    /** The model names for each train type. */
    protected static final String[] TYPE_NAMES = { "locomotive", "caboose",
        "cattle", "freight", "coal" };
}
