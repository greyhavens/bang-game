//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ControlTrainEffect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.TrainEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceUtil;

/**
 * Handles the behavior of trains.
 */
public class TrainDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        // create lists of tracks and terminals
        ArrayList<Track> tracks = new ArrayList<Track>(),
            terminals = new ArrayList<Track>();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Track) {
                Track track = (Track)piece;
                tracks.add(track);
                if (track.type == Track.TERMINAL) {
                    terminals.add(track);
                }
            }
        }
        _tracks = tracks.toArray(new Track[tracks.size()]);
        _terminals = terminals.toArray(new Track[terminals.size()]);
    }
    
    @Override // documentation inherited
    public void tick (BangObject bangobj, short tick)
    {
        // find the trains and terminals on the board; if there are no trains
        // but there are terminals, consider creating a train
        if (_trains.size() == 0) {
            if (_terminals.length > 0 && Math.random() < 1f/AVG_TRAIN_TICKS) {
                createTrain(bangobj);
            }
            return;
        }

        // update the oldest train first; if it moves, move the rest in order
        _mtrains = _trains.toArray(_mtrains);
        Train last = _mtrains[0];
        if (updateTrain(bangobj, _mtrains[0])) {
            for (int ii = 1; ii < _mtrains.length && _mtrains[ii] != null;
                ii++) {
                Train previous = _mtrains[ii - 1];
                moveTrain(bangobj, last = _mtrains[ii], previous.x,
                    previous.y);
            }
        }

        // see if there is a terminal that can pump out another car; if so,
        // consider pumping another one out
        Track terminal = getTerminalBehind(last);
        if (terminal != null && last.type != Train.CABOOSE) {
            createTrain(bangobj, last, terminal,
                Math.random() < 1f/AVG_TRAIN_CARS);
        }
    }

    /**
     * Returns the terminal behind the specified train, or <code>null</code>
     * if there isn't one.
     */
    protected Track getTerminalBehind (Train train)
    {
        for (int ii = 0; ii < _terminals.length; ii++) {
            if (train.isBehind(_terminals[ii])) {
                return _terminals[ii];
            }
        }
        return null;
    }

    /**
     * Adds a new train engine to the board.
     */
    protected void createTrain (BangObject bangobj)
    {
        // pick a random terminal
        Track terminal = RandomUtil.pickRandom(_terminals);

        // create the engine there
        Train train = new Train();
        train.assignPieceId(bangobj);
        train.x = terminal.x;
        train.y = terminal.y;
        train.orientation = terminal.orientation;
        train.type = Train.ENGINE;
        train.nextX = (short)(train.x + PieceCodes.DX[train.orientation]);
        train.nextY = (short)(train.y + PieceCodes.DY[train.orientation]);
        bangobj.addToPieces(train);
        _trains.add(train);
    }

    /**
     * Adds a new train car or caboose to the board.
     */
    protected void createTrain (
        BangObject bangobj, Train last, Track terminal, boolean caboose)
    {
        Train train = new Train();
        train.assignPieceId(bangobj);
        train.x = terminal.x;
        train.y = terminal.y;
        train.orientation = terminal.orientation;
        train.type = caboose ? Train.CABOOSE :
            Train.CAR_TYPES[RandomUtil.getInt(Train.CAR_TYPES.length)];
        train.nextX = last.x;
        train.nextY = last.y;
        bangobj.addToPieces(train);
        _trains.add(train);
    }

    /**
     * Updates the first in a sequence of train pieces.  This is the one that
     * will determine whether the entire train moves or not.
     *
     * @return true if the rest of the train should move, false otherwise
     */
    protected boolean updateTrain (BangObject bangobj, Train train)
    {
        // see if we've been flagged to disappear on this tick
        if (train.nextX == Train.UNSET) {
            bangobj.board.clearShadow(train);
            bangobj.removeFromPieces(train.getKey());
            _trains.remove(train);
            train.position(Train.UNSET, Train.UNSET); // suck the rest in
            return true;
        }

        // see if the next position is blocked
        Piece blocker = getBlockingPiece(bangobj, train, train.nextX,
            train.nextY);
        if (blocker instanceof Unit) {
            if (!pushUnit(bangobj, train, (Unit)blocker)) {
                return false;
            }

        } else if (blocker != null) {
            return false;
        }

        // make sure we have a reference to the next piece of track
        if (train.nextTrack == null) {
            for (int ii = 0; ii < _tracks.length; ii++) {
                if (_tracks[ii].intersects(train.nextX, train.nextY)) {
                    train.nextTrack = _tracks[ii];
                    break;
                }
            }
        }
        
        // if the train is under control and there's a path, follow it;
        // otherwise, choose a random adjacent section of track excluding
        // the one currently occupied
        Track track;
        if (train.path != null && !train.path.isEmpty()) {
            track = train.path.remove(0);
        } else {
            if (train.path != null && train.path.isEmpty()) {
                // release the train from control
                _bangmgr.deployEffect(-1, new ControlTrainEffect());
            }
            Track[] adjacent = train.nextTrack.getAdjacent(bangobj);
            Track behind = null;
            for (int ii = 0; ii < adjacent.length; ii++) {
                if (train.intersects(adjacent[ii])) {
                    behind = adjacent[ii];
                    break;
                }
            }
            track = (behind == null) ? RandomUtil.pickRandom(adjacent) :
                RandomUtil.pickRandom(adjacent, behind);
        }
        
        // if there's nowhere to go, flag to disappear on next tick; otherwise,
        // move to a random piece of track
        if (track == null) {
            moveTrain(bangobj, train, Train.UNSET, Train.UNSET);
        } else {
            train.nextTrack = track;
            moveTrain(bangobj, train, track.x, track.y);
        }

        return true;
    }

    /**
     * Searches for a piece that would block the train from moving to the
     * specified coordinates.  If there's a {@link Unit}, return that;
     * otherwise, return any blocking piece.
     */
    protected Piece getBlockingPiece (
        BangObject bangobj, Train train, int x, int y)
    {
        if (bangobj.board.isOccupiable(x, y)) {
            return null; // quick check for common case
        }
        Piece blocker = null;
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            if (piece.intersects(x, y) &&
                train.preventsOverlap(piece)) {
                if (piece instanceof Unit) {
                    return piece;

                } else {
                    blocker = piece;
                }
            }
        }
        return blocker;
    }

    /**
     * Handles the collision between a train and a unit.
     *
     * @return true if the train pushed the unit out of the way, false
     * otherwise
     */
    protected boolean pushUnit (BangObject bangobj, Train train, Unit unit)
    {
        Point pt = getPushLocation(bangobj, train, unit);
        if (pt == null) {
            _bangmgr.deployEffect(-1, new TrainEffect(unit, unit.x, unit.y));
            return false;

        } else {
            _bangmgr.deployEffect(-1, new TrainEffect(unit, pt.x, pt.y));
            return true;
        }
    }

    /**
     * Returns the location to which the specified unit will be pushed by
     * the given train, or <code>null</code> if the unit can't be pushed
     * anywhere.
     */
    protected Point getPushLocation (
        BangObject bangobj, Train train, Unit unit)
    {
        // only consider passable locations; prefer locations without tracks
        // and the location in front in that order
        int fwd = PieceUtil.getDirection(train, unit);
        int[] dirs = new int[] {
            fwd, // fwd
            (fwd + 1) % PieceCodes.DIRECTIONS.length, // left
            (fwd + 3) % PieceCodes.DIRECTIONS.length }; // right
        ArrayList<Point> passable = new ArrayList<Point>(),
            trackless = new ArrayList<Point>();
        for (int i = 0; i < dirs.length; i++) {
            int x = unit.x + PieceCodes.DX[dirs[i]];
            int y = unit.y + PieceCodes.DY[dirs[i]];
            if (bangobj.board.isOccupiable(x, y)) {
                Point pt = new Point(x, y);
                passable.add(pt);
                if (!hasTracks(bangobj, x, y)) {
                    trackless.add(pt);
                }
            }
        }
        if (passable.isEmpty()) {
            return null;
        }

        ArrayList<Point> pts = (trackless.isEmpty() ? passable : trackless);
        return (pts.size() == 2) ? RandomUtil.pickRandom(pts) :
            pts.get(0);
    }

    /**
     * Determines whether there is a track piece at the specified coordinates.
     */
    protected boolean hasTracks (BangObject bangobj, int tx, int ty)
    {
        for (int ii = 0; ii < _tracks.length; ii++) {
            if (_tracks[ii].intersects(tx, ty)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the train, performing any necessary updates.
     *
     * @param nx the next x coordinate to be occupied by the train
     * @param ny the next y coordinate to be occupied by the train
     */
    protected void moveTrain (BangObject bangobj, Train train, int nx, int ny)
    {
        MoveEffect effect = new MoveEffect();
        effect.init(train);
        effect.nx = (short)nx;
        effect.ny = (short)ny;
        _bangmgr.deployEffect(-1, effect);
    }

    /** The pieces of track on the board. */
    protected Track[] _tracks;
    
    /** The terminals on the board. */
    protected Track[] _terminals;

    /** The train pieces on the board. */
    protected ArrayList<Train> _trains = new ArrayList<Train>();
    
    /** Used to hold trains in the process of moving them. */
    protected Train[] _mtrains = new Train[0];
    
    /** The average number of ticks to let pass before we create a train when
     * there is no train on the board. */
    protected static final int AVG_TRAIN_TICKS = 3;

    /** The average number of cars on a train. */
    protected static final int AVG_TRAIN_CARS = 2;
}
