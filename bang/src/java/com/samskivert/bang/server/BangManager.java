//
// $Id$

package com.samskivert.bang.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.Interval;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.PresentsServer;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.data.BodyObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.BangCodes;
import com.samskivert.bang.data.BangMarshaller;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.ModifyBoardEvent;
import com.samskivert.bang.data.PiecePath;
import com.samskivert.bang.data.Terrain;
import com.samskivert.bang.data.generate.CompoundGenerator;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.PlayerPiece;
import com.samskivert.bang.data.piece.Tank;

import static com.samskivert.bang.Log.log;

/**
 * Handles the server-side of the game.
 */
public class BangManager extends GameManager
    implements BangProvider
{
    // documentation inherited from interface BangProvider
    public void setPath (ClientObject caller, PiecePath path)
    {
        BodyObject user = (BodyObject)caller;

        Piece piece = (Piece)_bangobj.pieces.get(path.pieceId);
        if (piece == null) {
            log.info("No such piece " + path.pieceId +
                     " [who=" + user.who() + "].");
            return;
        }

        // let me ask you this... do I own it?
        int pidx = _bangobj.getPlayerIndex(user.username);
        if (pidx != piece.owner) {
            log.warning("Requested to move not-our piece [who=" + user.who() +
                        ", piece=" + piece + "].");
            return;
        }

        // register the path in our table
        _paths.put(path.pieceId, path);

        // start the piece at the beginning of its path
        piece.pathPos = 0;
        _bangobj.updatePieces(piece);
    }

    // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.TICK)) {
            tick(_bangobj.tick);

        } else {
            super.attributeChanged(event);
        }
    }

    // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BangObject.class;
    }

    // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();
        _bangobj = (BangObject)_gameobj;
        _bangobj.setService(
            (BangMarshaller)PresentsServer.invmgr.registerDispatcher(
                new BangDispatcher(this), false));
    }

    // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();
        PresentsServer.invmgr.clearDispatcher(_bangobj.service);
    }

    // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // set up the game object
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        _bangobj.setBoard(createBoard(pieces));
        _bangobj.setPieces(new DSet(pieces.iterator()));

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.entries(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

//         // fire off messages for each of our goals
//         for (Iterator iter = _bangobj.goals.entries(); iter.hasNext(); ) {
//             SpeakProvider.sendInfo(_bangobj, BangCodes.BANG_MSGS,
//                                    ((Goal)iter.next()).getDescription());
//         }

        // queue up the board tick
        _ticker.schedule(2000L, true);
    }

    /**
     * Called when the board tick is incremented.
     */
    protected void tick (short tick)
    {
        log.fine("Ticking [tick=" + tick +
                 ", pcount=" + _bangobj.pieces.size() + "].");

        Piece[] pieces = _bangobj.getPieceArray();

//         // first, check whether all of our goals have been met or botched
//         boolean goalsRemain = false;
//         for (Iterator giter = _bangobj.goals.entries(); giter.hasNext(); ) {
//             Goal goal = (Goal)giter.next();
//             if (!goal.isMet(_bangobj.board, pieces) &&
//                 !goal.isBotched(_bangobj.board, pieces)) {
//                 // if we have at least one unmet goal we can stop checking
//                 goalsRemain = true;
//                 break;
//             }
//         }

        // next check to see whether any of our pieces have energy remaining
        boolean haveEnergy = false;
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof PlayerPiece) &&
                pieces[ii].canTakeStep()) {
                haveEnergy = true;
                break;
            }
        }

        // the game ends when none of our pieces have energy or we've
        // accomplished or botched all of our goals
        if (!haveEnergy /* || !goalsRemain */) {
            // if the piece ran out of energy, let the player know
            if (/* goalsRemain && */ !haveEnergy) {
                SpeakProvider.sendInfo(
                    _bangobj, BangCodes.BANG_MSGS, "m.out_of_energy");
            }
            endGame();
            return;
        }

        // move all of our pieces along any path they have configured
        Iterator<PiecePath> iter = _paths.values().iterator();
        while (iter.hasNext()) {
            PiecePath path = iter.next();
            Piece piece = (Piece)_bangobj.pieces.get(path.pieceId);
            if (piece == null || tickPath(piece, path)) {
                log.fine("Finished " + path + ".");
                // if the piece has gone away, or if we complete our path,
                // remove it
                iter.remove();
            }
        }

        // recreate our pieces array as pieces may have moved
        pieces = _bangobj.getPieceArray();

        // then give any piece a chance to react to the state of the board
        // now that everyone has moved
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece piece = pieces[ii];
            // skip pieces that were eaten
            if (!_bangobj.pieces.containsKey(piece.pieceId)) {
                continue;
            }
            if (piece.react(_bangobj, pieces)) {
                _bangobj.updatePieces(piece);
            }
        }
    }

    /**
     * Moves the supplied piece further along its configured path.
     *
     * @return true if the bug reached the final goal on the path, false
     * if not.
     */
    protected boolean tickPath (Piece piece, PiecePath path)
    {
        log.fine("Moving " + path + ".");
        int nx = path.getNextX(piece), ny = path.getNextY(piece);

        // make sure the piece has the energy to move that far
        int steps = Math.abs(piece.x-nx) + Math.abs(piece.y-ny);
        if (piece.energy < steps * piece.energyPerStep()) {
            log.info("Piece out of energy [piece=" + piece + "].");
            piece.pathPos = -1;
            _bangobj.updatePieces(piece);
            return true;
        }

        // try moving the piece
        Piece npiece = movePiece(piece, nx, ny);
        if (npiece == null) {
            return false;
        }

        // check to see if we've reached the end of our path
        boolean reachedGoal = path.reachedGoal(npiece);
        if (reachedGoal) {
            npiece.pathPos = -1;
        }

        // finally broadcast our updated piece
        _bangobj.updatePieces(npiece);
        return reachedGoal;
    }

    /**
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return a new piece at the new location if the piece was moved,
     * null if it was not movable for some reason.
     */
    protected Piece movePiece (Piece piece, int x, int y)
    {
        // validate that the move is legal (proper length, can traverse
        // all tiles along the way, no pieces intervene, etc.)
        if (!piece.canMoveTo(_bangobj.board, x, y)) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", x=" + x + ", y=" + y + "].");
            return null;
        }

        // calculate the distance we're moving
        int steps = Math.abs(piece.x-x) + Math.abs(piece.y-y);

        // clone the piece so that we can investigate the hypothetical
        piece = (Piece)piece.clone();
        piece.position(x, y);

        // ensure that intervening pieces do not block this move; also
        // track any piece that we end up overlapping
        ArrayList<Piece> lappers = _bangobj.getOverlappers(piece);
        Piece lapper = null;
        if (lappers != null) {
            for (Piece p : lappers) {
                if (p.preventsOverlap(piece)) {
                    return null;
                } else if (lapper != null) {
                    log.warning("Multiple overlapping pieces [mover=" + piece +
                                ", lap1=" + lapper + ", lap2=" + p + "].");
                } else {
                    lapper = p;
                }
            }
        }

        // consume the energy needed to make this move (we checked that
        // this was possible before we even called movePiece)
        piece.consumeEnergy(steps);

        // interact with any piece occupying our target space
        if (lapper != null) {
            switch (piece.maybeInteract(lapper)) {
            case CONSUMED:
                _bangobj.removeFromPieces(lapper.getKey());
                break;

            case ENTERED:
                // update the piece we entered as we likely modified it in
                // doing so
                _bangobj.updatePieces(lapper);
                // TODO: generate a special event indicating that the
                // piece entered so that we can animate it
                _bangobj.removeFromPieces(piece.getKey());
                // short-circuit the remaining move processing
                return piece;

            case INTERACTED:
                // update the piece we interacted with, we'll update
                // ourselves momentarily
                _bangobj.updatePieces(lapper);
                break;

            case NOTHING:
                break;
            }
        }

        // allow the piece to modify the board
        Terrain terrain = piece.modifyBoard(_bangobj.board, x, y);
        if (terrain != Terrain.NONE) {
            // update the board immediately and then dispatch the event
            _bangobj.board.setTile(x, y, terrain);
            _bangobj.getManager().postEvent(
                new ModifyBoardEvent(_bangobj.getOid(), x, y, terrain));
        }

        return piece;
    }

    // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // cancel the board tick
        _ticker.cancel();

//         // report the state of our goals
//         Piece[] pieces = _bangobj.getPieceArray();
//         for (Iterator giter = _bangobj.goals.entries(); giter.hasNext(); ) {
//             Goal goal = (Goal)giter.next();
//             String msg = "";
//             if (goal.isMet(_bangobj.board, pieces)) {
//                 msg = goal.getMetMessage();
//             } else {
//                 msg = goal.getBotchedMessage();
//             }
//             SpeakProvider.sendInfo(_bangobj, BangCodes.BANG_MSGS, msg);
//         }
    }

    /**
     * Creates the bang board based on the game config, filling in the
     * supplied pieces array with the starting pieces.
     */
    protected BangBoard createBoard (ArrayList<Piece> pieces)
    {
        ToyBoxGameConfig bconfig = (ToyBoxGameConfig)_gameconfig;

        // generate a random board
        int size = (Integer)bconfig.params.get("board_size");
        BangBoard board = new BangBoard(size, size);
        CompoundGenerator gen = new CompoundGenerator();
        gen.generate(50, board, pieces);

        Tank tank = new Tank();
        tank.assignPieceId();
        tank.position(5, 5);
        tank.owner = 0;
        pieces.add(tank);
        tank = new Tank();
        tank.assignPieceId();
        tank.position(7, 5);
        tank.owner = 1;
        pieces.add(tank);

        return board;
    }

//     /** Configures our goals for this game. */
//     protected DSet configureGoals ()
//     {
//         ArrayList<Goal> goals = new ArrayList<Goal>();
//         Piece[] pieces = _bangobj.getPieceArray();

//         // check our various goals to see which should be added
//         Goal goal = new AntHillGoal();
//         if (goal.isReachable(_bangobj.board, pieces)) {
//             goals.add(goal);
//         }
//         goal = new PollinateGoal();
//         if (goal.isReachable(_bangobj.board, pieces)) {
//             goals.add(goal);
//         }

//         return new DSet(goals.iterator());
//     }

    /** Triggers our board tick once every N seconds. */
    protected Interval _ticker = _ticker = new Interval(PresentsServer.omgr) {
        public void expired () {
            int nextTick = (_bangobj.tick + 1) % Short.MAX_VALUE;
            _bangobj.setTick((short)nextTick);
        }
    };

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
