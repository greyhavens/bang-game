//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.samskivert.util.QuickSort;
import com.threerings.media.util.MathUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.TrainEffect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PieceUtil;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Implements a particular gameplay scenario.
 */
public abstract class Scenario
    implements GameCodes, PieceCodes
{
    /**
     * Allows a scenario to filter out custom marker pieces prior to the
     * start of the round. <em>Note:</em> this is called before {@link
     * #init}.
     *
     * @param bangobj the game object.
     * @param starts a list of start markers for all the players.
     * @param pieces the remaining pieces on the board.
     */
    public void filterMarkers (BangObject bangobj, ArrayList<Piece> starts,
                               ArrayList<Piece> pieces)
    {
        // nothing to do by default
    }

    /**
     * Called to initialize a scenario when it is created.
     */
    public void init (BangManager bangmgr)
    {
        _bangmgr = bangmgr;
    }

    /**
     * Determines the next phase of the game. Normally a game transitions from
     * {@link BangObject#SELECT_PHASE} to {@link BangObject#BUYING_PHASE} to
     * {@link BangObject#IN_PLAY}, but the tutorial scenario skips some of
     * those phases.
     */
    public void startNextPhase (BangObject bangobj)
    {
        switch (bangobj.state) {
        case BangObject.POST_ROUND:
        case BangObject.PRE_GAME:
            bangobj.setState(BangObject.SELECT_PHASE);
            break;

        case BangObject.SELECT_PHASE:
            bangobj.setState(BangObject.BUYING_PHASE);
            break;

        case BangObject.BUYING_PHASE:
            _bangmgr.startGame();
            break;

        default:
            log.warning("Unable to start next phase [game=" + bangobj.which() +
                        ", state=" + bangobj.state + "].");
            break;
        }
    }

    /**
     * Returns the maximum duration of this scenario in ticks.
     */
    public short getDuration (BangConfig bconfig)
    {
        return (short)Math.ceil(getBaseDuration() / (bconfig.teamSize+1f));
    }

    /**
     * Called when a round is about to start.
     *
     * @throws InvocationException containing a translatable string
     * indicating why the scenario is booched, which will be displayed to
     * the players and the game will be cancelled.
     */
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        // clear our respawn queue
        _respawns.clear();

        // set up our maximum duration timer
        _startStamp = System.currentTimeMillis();
        _warnStage = 0;

        // this will contain the starting spot for each player
        _startSpots = new Point[bangobj.players.length];
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Piece p = starts.get(ii);
            _startSpots[ii] = new Point(p.x, p.y);
        }
    }

    /**
     * Called at the start of every game tick to allow the scenario to affect
     * the game state and determine whether or not the game should be ended.
     * If the scenario wishes to end the game early, it should set {@link
     * BangObject#lastTick} to the tick on which the game should end (if it is
     * set to the current tick the game will be ended when this call returns).
     */
    public void tick (BangObject bangobj, short tick)
    {
        // respawn new pieces
        while (_respawns.size() > 0) {
            if (_respawns.get(0).getRespawnTick() > tick) {
                break;
            }

            Unit unit = _respawns.remove(0);
            log.info("Respawning " + unit + ".");

            // reassign the unit to its original owner
            unit.owner = unit.originalOwner;

            // figure out where to put this guy
            Point spot = _startSpots[unit.owner];
            Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for to-be-respawned " +
                            "unit [unit=" + unit + ", spot=" + spot + "].");
                // stick him back on the queue for a few ticks later
                unit.setRespawnTick((short)(tick + RESPAWN_TICKS));
                _respawns.add(unit);
                continue;
            }

            // reset the units vital statistics
            unit.damage = 0;
            unit.influence = null;
            unit.setRespawnTick((short)0);

            // if the unit is still in play for some reason, remove it first
            if (bangobj.pieces.containsKey(unit.getKey())) {
                bangobj.board.clearShadow(unit);
                bangobj.removeFromPieces(unit.getKey());
            }

            // then position it and add it back at its new location
            unit.position(bspot.x, bspot.y);
            bangobj.addToPieces(unit);
            bangobj.board.shadowPiece(unit);
        }

        // update train pieces
        updateTrains(bangobj);
    }

    /**
     * Called when a piece makes a move in the game. The scenario can deploy
     * effects (via {@link BangManager#deployEffect}) as a result of the move.
     */
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Unit) {
            // check to see if this unit spooked any cattle
            Piece[] pieces = bangobj.getPieceArray();
            for (int ii = 0; ii < pieces.length; ii++) {
                if (pieces[ii] instanceof Cow &&
                    piece.getDistance(pieces[ii]) == 1) {
                    Effect effect = ((Cow)pieces[ii]).spook(
                        bangobj.board, (Unit)piece);
                    if (effect != null) {
                        _bangmgr.deployEffect(piece.owner, effect);
                    }
                }
            }
        }
    }

    /**
     * Called when a piece was killed. The scenario can choose to respawn
     * the piece later, and do whatever else is appropriate.
     *
     * @return any effect that should be applied as a result of the piece being
     * killed or null. The returned effect should already be initialized.
     */
    public Effect pieceWasKilled (BangObject bangobj, Piece piece)
    {
        if (respawnPieces()) {
            maybeQueueForRespawn(piece, bangobj.tick);
        }
        return null;
    }

    /**
     * Called when a round has ended, giving the scenario a chance to award any
     * final cash and increment associated statistics.
     */
    public void roundDidEnd (BangObject bangobj)
    {
        // nothing by default
    }

    /**
     * Gives the scenario an opportunity to record statistics for the supplied
     * player at the end of the game.
     */
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        // nothing by default
    }

    /**
     * Updates the train pieces on the board.
     */
    protected void updateTrains (BangObject bangobj)
    {
        // find the trains and terminals on the board; if there are no trains
        // but there are terminals, consider creating a train
        ArrayList<Train> trains = getTrains(bangobj);
        ArrayList<Track> terminals = getTerminals(bangobj);
        if (trains.size() == 0) {
            if (terminals.size() > 0 && Math.random() < 1f/AVG_TRAIN_TICKS) {
                createTrain(bangobj, terminals);
            }
            return;
        }

        // update the oldest train first; if it moves, move the rest in order
        QuickSort.sort(trains, PIECE_ID_COMPARATOR);
        if (updateTrain(bangobj, trains.get(0))) {
            for (int i = 1, size = trains.size(); i < size; i++) {
                Train previous = trains.get(i-1);
                moveTrain(bangobj, trains.get(i), previous.x, previous.y);
            }
        }

        // see if there is a terminal that can pump out another car; if so,
        // consider pumping another one out
        Train last = trains.get(trains.size() - 1);
        Track terminal = getTerminalBehind(last, terminals);
        if (terminal != null && last.type != Train.CABOOSE) {
            createTrain(bangobj, last, terminal,
                Math.random() < 1f/AVG_TRAIN_CARS);
        }
    }

    /**
     * Adds a new train engine to the board.
     */
    protected void createTrain (BangObject bangobj, ArrayList<Track> terminals)
    {
        // pick a random terminal
        Track terminal = (Track)RandomUtil.pickRandom(terminals);

        // create the engine there
        Train train = new Train();
        train.assignPieceId(bangobj);
        train.x = terminal.x;
        train.y = terminal.y;
        train.orientation = terminal.orientation;
        train.type = Train.ENGINE;
        train.nextX = (short)(train.x + DX[train.orientation]);
        train.nextY = (short)(train.y + DY[train.orientation]);
        bangobj.addToPieces(train);
    }

    /**
     * Adds a new train car or caboose to the board.
     */
    protected void createTrain (BangObject bangobj, Train last,
        Track terminal, boolean caboose)
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

        // find the adjacent track pieces excluding the one behind
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            if (piece instanceof Track &&
                ((Track)piece).isConnectedTo(train.nextX, train.nextY) &&
                (piece.x != train.x || piece.y != train.y)) {
                tracks.add((Track)piece);
            }
        }

        // if there's nowhere to go, flag to disappear on next tick; otherwise,
        // move to a random piece of track
        if (tracks.size() == 0) {
            moveTrain(bangobj, train, Train.UNSET, Train.UNSET);

        } else {
            Track track = (Track)RandomUtil.pickRandom(tracks);
            moveTrain(bangobj, train, track.x, track.y);
        }

        return true;
    }

    /**
     * Searches for a piece that would block the train from moving to the
     * specified coordinates.  If there's a {@link Unit}, return that;
     * otherwise, return any blocking piece.
     */
    protected Piece getBlockingPiece (BangObject bangobj, Train train, int x,
        int y)
    {
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
    protected Point getPushLocation (BangObject bangobj, Train train,
        Unit unit)
    {
        // only consider passable locations; prefer locations without
        // tracks and the location in front in that order
        int fwd = PieceUtil.getDirection(train, unit);
        int[] dirs = new int[] { fwd, (fwd + 1) % DIRECTIONS.length,
            (fwd + 3) % DIRECTIONS.length }; // fwd, left, right
        ArrayList<Point> passable = new ArrayList<Point>(),
            trackless = new ArrayList<Point>();
        for (int i = 0; i < dirs.length; i++) {
            int x = unit.x + DX[dirs[i]], y = unit.y + DY[dirs[i]];
            if (unit.canTraverse(bangobj.board, x, y)) {
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
        return (pts.size() == 2) ? (Point)RandomUtil.pickRandom(pts) :
            pts.get(0);
    }

    /**
     * Determines whether there is a track piece at the specified coordinates.
     */
    protected boolean hasTracks (BangObject bangobj, int tx, int ty)
    {
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            if (piece instanceof Track && piece.intersects(tx, ty)) {
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

    /**
     * Gets a list of all trains on the board.
     */
    protected ArrayList<Train> getTrains (BangObject bangobj)
    {
        ArrayList<Train> trains = new ArrayList<Train>();
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Object piece = it.next();
            if (piece instanceof Train) {
                trains.add((Train)piece);
            }
        }
        return trains;
    }

    /**
     * Gets a list of all terminals on the board.
     */
    protected ArrayList<Track> getTerminals (BangObject bangobj)
    {
        ArrayList<Track> terminals = new ArrayList<Track>();
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Object piece = it.next();
            if (piece instanceof Track &&
                ((Track)piece).type == Track.TERMINAL) {
                terminals.add((Track)piece);
            }
        }
        return terminals;
    }

    /**
     * Returns the terminal behind the specified train, or <code>null</code>
     * if there isn't one.
     */
    protected Track getTerminalBehind (Train train, ArrayList<Track> terminals)
    {
        for (int i = 0, size = terminals.size(); i < size; i++) {
            if (train.isBehind(terminals.get(i))) {
                return terminals.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the base duration of the scenario in ticks assuming 1.75 seconds
     * per tick. This will be scaled by the expected average number of units
     * per player to obtain a real duration.
     */
    protected short getBaseDuration ()
    {
        return BASE_SCENARIO_TICKS;
    }

    /**
     * If a scenario wishes for pieces to respawn, it should override this
     * method and return true.
     */
    protected boolean respawnPieces ()
    {
        return false;
    }

    /**
     * Called when a piece "dies" to potentially queue it up for
     * respawning (assuming it's a unit and that it's marked for respawn.
     */
    protected void maybeQueueForRespawn (Piece piece, short tick)
    {
        if (!(piece instanceof Unit) || ((Unit)piece).originalOwner == -1) {
            return;
        }
        Unit unit = (Unit)piece;
        unit.setRespawnTick((short)(tick + RESPAWN_TICKS));
        _respawns.add(unit);
        log.info("Queued for respawn " + unit + ".");
    }

    /**
     * Helper function useful when initializing scenarios. Determines the
     * player whose start marker is closest to the specified piece and is
     * therefore the <em>owner</em> of that piece.
     *
     * @return -1 if no start markers exist at all or the player index of
     * the closest marker.
     */
    protected int getOwner (Piece target, ArrayList<Piece> starts)
    {
        int mindist2 = Integer.MAX_VALUE, idx = -1;
        for (int ii = 0, ll = starts.size(); ii < ll; ii++) {
            Piece start = starts.get(ii);
            int dist2 = MathUtil.distanceSq(
                target.x, target.y, start.x, start.y);
            if (dist2 < mindist2) {
                mindist2 = dist2;
                idx = ii;
            }
        }
        return idx;
    }

    /** The Bang game manager. */
    protected BangManager _bangmgr;

    /** Used to track the locations where players are started. */
    protected Point[] _startSpots;

    /** A list of units waiting to be respawned. */
    protected ArrayList<Unit> _respawns = new ArrayList<Unit>();

    /** The time at which the current round started. */
    protected long _startStamp;

    /** Used to track when we've warned about the end of the round. */
    protected int _warnStage = -1;

    /** The number of ticks that must elapse before a unit is respawned. */
    protected static final int RESPAWN_TICKS = 12;

    /** The base number of ticks that we allow per round (scaled by the
     * anticipated average number of units per-player). */
    protected static final short BASE_SCENARIO_TICKS = 300;

    /** A set of times (in seconds prior to the end of the round) at which
     * we warn the players. */
    protected static final long[] TIME_WARNINGS = {
        60*1000L, 30*1000L, 10*1000L };

    /** The average number of ticks to let pass before we create a train when
     * there is no train on the board. */
    protected static final int AVG_TRAIN_TICKS = 3;

    /** The average number of cars on a train. */
    protected static final int AVG_TRAIN_CARS = 2;

    /** Compares pieces based on their piece ids. */
    protected static final Comparator<Piece> PIECE_ID_COMPARATOR =
        new Comparator<Piece>() {
        public int compare (Piece p1, Piece p2) {
            return p1.pieceId - p2.pieceId;
        }
    };
}
