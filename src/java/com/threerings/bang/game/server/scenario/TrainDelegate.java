//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ControlTrainEffect;
import com.threerings.bang.game.data.effect.PuntEffect;
import com.threerings.bang.game.data.effect.TrainEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceUtil;

import static com.threerings.bang.Log.log;

/**
 * Handles the behavior of trains.
 */
public class TrainDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        // divide the tracks into connected groups
        List<TrackGroup> tgroups = Lists.newArrayList();
        for (Track track : bangobj.getTracks().values()) {
            if (track.type != Track.TERMINAL || track.visited != 0) {
                continue;
            }
            tgroups.add(new TrackGroup(bangobj, tgroups.size(), track));
        }
        _tgroups = tgroups.toArray(new TrackGroup[tgroups.size()]);
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = super.tick(bangobj, tick);
        // tick each track group separately
        for (TrackGroup tgroup : _tgroups) {
            validate = tgroup.tick(bangobj) || validate;
        }
        return validate;
    }

    /**
     * Searches for a piece that would block the train from moving to the specified coordinates.
     * If there's a {@link Unit}, return that; otherwise, return any blocking piece.
     */
    protected static Piece getBlockingPiece (BangObject bangobj, Train train, int x, int y)
    {
        if (bangobj.board.isOccupiable(x, y)) {
            return null; // quick check for common case
        }
        Piece blocker = null;
        for (Piece piece : bangobj.pieces) {
            if (piece.intersects(x, y) && train.preventsOverlap(piece)) {
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
     * Returns the location to which the specified unit will be pushed by
     * the given train, or <code>null</code> if the unit can't be pushed
     * anywhere.
     */
    protected static Point getPushLocation (BangObject bangobj, Train train, Unit unit)
    {
        // only consider passable locations; prefer locations without tracks
        // and the location in front in that order
        int fwd = PieceUtil.getDirection(train, unit);
        int[] dirs = new int[] {
            fwd, // fwd
            (fwd + 1) % PieceCodes.DIRECTIONS.length, // left
            (fwd + 3) % PieceCodes.DIRECTIONS.length }; // right
        List<Point> passable = Lists.newArrayList(), trackless = Lists.newArrayList();
        for (int i = 0; i < dirs.length; i++) {
            int x = unit.x + PieceCodes.DX[dirs[i]];
            int y = unit.y + PieceCodes.DY[dirs[i]];
            if (bangobj.board.isOccupiable(x, y) && bangobj.board.canCross(unit.x, unit.y, x, y)) {
                Point pt = new Point(x, y);
                passable.add(pt);
                if (!bangobj.getTracks().containsKey(Piece.coord(x, y))) {
                    trackless.add(pt);
                }
            }
        }
        if (passable.isEmpty()) {
            return null;
        }

        List<Point> pts = (trackless.isEmpty() ? passable : trackless);
        return (pts.size() == 2) ? RandomUtil.pickRandom(pts) :
            pts.get(0);
    }

    /**
     * Determines the maximum number of cars that trains starting from the given track
     * can have without running into their own tails.
     *
     * @param terminals any terminals encountered in the search will be added to this list
     * @param group all pieces of track encountered will be marked with this group id
     */
    protected static int getMaxTrainLength (
        BangObject bangobj, Track track, List<Track> terminals, int group, int step)
    {
        if (track.visited == 0) { // first time encountered
            track.group = group;
            if (track.type == Track.TERMINAL) {
                terminals.add(track);
            }
        }
        track.visited = step;
        int maxlen = MAX_MAX_TRAIN_LENGTH;
        for (Track adj : track.getAdjacent(bangobj)) {
            if (step > 1 && adj.visited == step - 1) {
                continue; // ignore last spot visited
            }
            maxlen = Math.min(maxlen, (adj.visited > 0) ?
                step - adj.visited :
                getMaxTrainLength(bangobj, adj, terminals, group, step + 1));
        }
        track.visited = -1;
        return maxlen;
    }

    /** Represents a single connected group of tracks. */
    protected class TrackGroup
    {
        /**
         * Initializes the group using a single starting terminal.
         */
        public TrackGroup (BangObject bangobj, int idx, Track terminal)
        {
            _idx = idx;
            List<Track> terminals = Lists.newArrayList();
            _maxTrainLength = getMaxTrainLength(bangobj, terminal, terminals, idx, 1);
            _terminals = terminals.toArray(new Track[terminals.size()]);
        }

        /**
         * Updates the trains in this group.
         */
        public boolean tick (BangObject bangobj)
        {
            // if there are no trains, consider creating one
            if (_trains.isEmpty()) {
                if (Math.random() < 1f/AVG_SPAWN_TICKS) {
                    createTrain(bangobj);
                    return true;
                }
                return false;
            }

            // advance the train by a random number of tiles
            int tiles = RandomUtil.getInRange(MIN_TRAIN_TILES-1, MAX_TRAIN_TILES);
            for (int ii = 0; ii < tiles && !_trains.isEmpty(); ii++) {
                advanceTrain(bangobj);
            }
            return true;
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
            train.group = _idx;
            _bangmgr.addPiece(train);
            _trains.add((Train)bangobj.pieces.get(train.pieceId));

            // choose a random length
            _uncreated = RandomUtil.getInRange(MIN_TRAIN_LENGTH-2, _maxTrainLength + 1);
        }

        /**
         * Advances the train one tile.
         */
        protected void advanceTrain (BangObject bangobj)
        {
            // update the oldest train first
            Train first = _trains.get(0);
            TrainEffect effect = updateTrain(bangobj, first);
            if (effect == null) {
                return;
            }

            // pump another car out, if necessary
            if ((effect.nx == Train.UNSET || effect.nx != first.nextX ||
                    effect.ny != first.nextY) && _uncreated-- > 0) {
                Train last = _trains.get(_trains.size() - 1),
                    ntail = new Train();
                ntail.assignPieceId(bangobj);
                ntail.x = last.x;
                ntail.y = last.y;
                ntail.nextX = last.nextX;
                ntail.nextY = last.nextY;
                ntail.setOwner(bangobj, last.owner);
                ntail.orientation = last.orientation;
                ntail.type = (_uncreated == 0) ? Train.CABOOSE :
                    Train.CAR_TYPES[RandomUtil.getInt(Train.CAR_TYPES.length)];
                ntail.group = _idx;
                effect.ntail = ntail;
            }

            // deploy the effect and update the list of trains
            _bangmgr.deployEffect(-1, effect);
            if (!bangobj.pieces.contains(first)) {
                _trains.remove(first);
            }
            if (effect.ntail != null) {
                _trains.add((Train)bangobj.pieces.get(effect.ntail.pieceId));
            }
        }

        /**
         * Updates the train and returns an effect if it's doing anything.
         */
        protected TrainEffect updateTrain (BangObject bangobj, Train first)
        {
            // see if we've been flagged to disappear on this tick
            if (first.nextX == Train.UNSET) {
                return new TrainEffect(_trains, Train.UNSET, Train.UNSET);
            }

            // see if the next position is blocked
            Piece blocker = getBlockingPiece(bangobj, first, first.nextX, first.nextY);
            Point push = null;
            if (blocker instanceof Unit) {
                push = getPushLocation(bangobj, first, (Unit)blocker);
                if (push == null) { // hit without going anywhere
                    return new TrainEffect(
                        _trains, first.nextX, first.nextY, blocker,
                        new Point(blocker.x, blocker.y));
                }
            } else if (blocker != null) { // can't move forward
                return null;
            }

            // if the next position contains a bonus, we need to punt it out of the way
            if (bangobj.board.containsBonus(first.nextX, first.nextY)) {
                for (Piece piece : bangobj.pieces) {
                    if (piece instanceof Bonus && piece.intersects(first.nextX, first.nextY)) {
                        Bonus bonus = (Bonus)piece;
                        if (!bonus.getConfig().hidden) {
                            _bangmgr.deployEffect(
                                -1, PuntEffect.puntBonus(bangobj, bonus, first.pieceId));
                        }
                        break;
                    }
                }
            }

            // if the train is following a non-empty path, keep following it;
            // if it's empty, release the train from control
            if (first.path != null) {
                Point pt = first.path.remove(0);
                if (first.path.isEmpty()) {
                    _bangmgr.deployEffect(-1, new ControlTrainEffect(first.group));
                    first.path = null;
                }
                return new TrainEffect(_trains, pt.x, pt.y, blocker, push);
            }

            // find the adjacent pieces of track, excluding the one behind, and
            // choose one randomly
            Track nnext = null, next = bangobj.getTracks().get(
                Piece.coord(first.nextX, first.nextY));
            if (next != null) {
                Track[] adjacent = next.getAdjacent(bangobj);
                Track behind = null;
                for (int ii = 0; ii < adjacent.length; ii++) {
                    if (first.intersects(adjacent[ii])) {
                        behind = adjacent[ii];
                        break;
                    }
                }
                nnext = (behind == null) ? RandomUtil.pickRandom(adjacent) :
                    RandomUtil.pickRandom(adjacent, behind);

            } else {
                log.warning("Train configured to move to non-existent track!?",
                            "in", _bangmgr.where(), "first", first,
                            "tracks", bangobj.getTracks().keySet());
            }

            // if there's nowhere to go, flag to disappear on next tick
            if (nnext == null) {
                return new TrainEffect(_trains, Train.UNSET, Train.UNSET, blocker, push);
            } else {
                return new TrainEffect(_trains, nnext.x, nnext.y, blocker, push);
            }
        }

        /** The group's index in the array. */
        protected int _idx;

        /** The terminals in the group. */
        protected Track[] _terminals;

        /** The maximum length of trains in this group. */
        protected int _maxTrainLength;

        /** The currently active trains in this group. */
        protected List<Train> _trains = Lists.newArrayList();

        /** The number of trains remaining to create in this group. */
        protected int _uncreated;
    }

    /** Information concerning each connected group of track and its trains. */
    protected TrackGroup[] _tgroups;

    /** The average number of ticks to let pass before we create a train when
     * there is no train on a particular group of tracks. */
    protected static final int AVG_SPAWN_TICKS = 2;

    /** The minimum train length (including engine and caboose). */
    protected static final int MIN_TRAIN_LENGTH = 2;

    /** The maximum train length, ignoring limitations imposed by track loops. */
    protected static final int MAX_MAX_TRAIN_LENGTH = 8;

    /** The minimum number of tiles to move per tick. */
    protected static final int MIN_TRAIN_TILES = 1;

    /** The maximum number of tiles to move per tick. */
    protected static final int MAX_TRAIN_TILES = 4;
}
