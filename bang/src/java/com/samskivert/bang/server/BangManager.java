//
// $Id$

package com.samskivert.bang.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Interval;

import com.threerings.util.RandomUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.server.PresentsServer;

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
import com.samskivert.bang.data.Shot;
import com.samskivert.bang.data.Terrain;
import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.generate.CompoundGenerator;
import com.samskivert.bang.data.generate.SkirmishScenario;
import com.samskivert.bang.data.generate.TestScenario;
import com.samskivert.bang.data.piece.Bonus;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.PlayerPiece;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

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
        _bangobj.addListener(_applier);
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
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

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

        // next check to see whether anyone's pieces are still alive
        _havers.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof PlayerPiece) &&
                pieces[ii].isAlive()) {
                _havers.add(pieces[ii].owner);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            endGame();
            return;
        }

        // these shots will be communicated to the client for animation
        ArrayList<Shot> shots = new ArrayList<Shot>();
        // these effects will be communicated to the client and
        // applied to the board
        ArrayList<Effect> effects = new ArrayList<Effect>();

        try {
            // batch our tick update
            _bangobj.startTransaction();

            // randomize the piece array before executing moves to insure
            // that no one is favored during potential conflicts
            ArrayUtil.shuffle(pieces);

            // TODO: maybe now sort them by remaining energy or lowest
            // damage to give players some general idea as to who goes
            // first

            // these pieces will be updated
            PieceSet updates = new PieceSet();

            // move all of the pieces along their paths
            for (int ii = 0; ii < pieces.length; ii++) {
                Piece piece = pieces[ii];
                PiecePath path = _paths.get(piece.pieceId);
                if (path == null) {
                    continue;
                }

                if (tickPath(piece, path, updates, effects)) {
                    log.fine("Finished " + path + ".");
                    _paths.remove(piece.pieceId);
                }
            }

            // recreate our pieces array; pieces may have been removed
            pieces = _bangobj.getPieceArray();

            // TEMPorary HACKery: if this is a one player game, move
            // player 2s pieces around randomly
//             if (_bangobj.players.length == 1) {
//                 PointSet moves = new PointSet();
//                 for (int ii = 0; ii < pieces.length; ii++) {
//                     Piece p = pieces[ii];
//                     if (p.owner != 1 || !p.isAlive()) {
//                         continue;
//                     }
//                     moves.clear();
//                     p.enumerateLegalMoves(p.x, p.y, moves);
//                     if (moves.size() == 0) {
//                         continue;
//                     }
//                     int idx = RandomUtil.getInt(moves.size());
//                     movePiece(p, moves.getX(idx), moves.getY(idx), updates);
//                 }
//             }

            // then give any piece a chance to react to the state of the board
            // now that everyone has moved
            for (int ii = 0; ii < pieces.length; ii++) {
                Piece piece = pieces[ii];
                // skip pieces that were eaten or are fully damaged
                if (!_bangobj.pieces.containsKey(piece.pieceId) ||
                    piece.damage >= 100) {
                    continue;
                }
                piece.react(_bangobj, pieces, updates, shots);
            }

            // finally update the pieces that need updating
            for (Piece piece : updates.values()) {
                // skip pieces that were eaten
                if (!_bangobj.pieces.containsKey(piece.pieceId)) {
                    continue;
                }
                _bangobj.updatePieces(piece);
            }

        } finally {
            _bangobj.commitTransaction();
        }

        // this lets the clients know the updates are done and gives
        // them a chance to to post-tick processing
        Object[] args = new Object[] {
            shots.toArray(new Shot[shots.size()]),
            effects.toArray(new Effect[effects.size()])
        };
        _bangobj.postMessage("ticked", args);
    }

    /** Applies the effects of shots fired and full-board effects after
     * the normal tick processing has completed. The client will do this
     * same processing to its data. */
    protected void postTick (Shot[] shots, Effect[] effects)
    {
        // first apply the shots
        for (int ii = 0; ii < shots.length; ii++) {
            _bangobj.applyShot(shots[ii]);
        }

        // optimize for the common case
        if (effects.length == 0) {
            return;
        }

        try {
            _bangobj.startTransaction();

            // next apply the full board effects
            ArrayList<Piece> additions = new ArrayList<Piece>();
            PieceSet removals = new PieceSet();
            _bangobj.applyEffects(effects, additions, removals);

            // now determine whether any new bonuses should be added to
            // the board
            addBonuses(additions);

            // add the additions
            for (int ii = 0, ll = additions.size(); ii < ll; ii++) {
                _bangobj.addToPieces(additions.get(ii));
            }

            // remove the removals
            for (Piece piece : removals.values()) {
                _bangobj.removeFromPieces(piece.getKey());
            }

        } finally {
            _bangobj.commitTransaction();
        }
    }

    @Override // documentation inherited
    protected void assignWinners (boolean[] winners)
    {
        for (int ii = 0; ii < winners.length; ii++) {
            winners[ii] = _havers.contains(ii);
        }
    }

    /**
     * Moves the supplied piece further along its configured path.
     *
     * @return true if the piece reached the final goal on the path, false
     * if not.
     */
    protected boolean tickPath (Piece piece, PiecePath path, PieceSet updates,
                                ArrayList<Effect> effects)
    {
        log.fine("Moving " + path + ".");
        int nx = path.getNextX(piece), ny = path.getNextY(piece);

        for (int ii = 0; ii < 2; ii++) {
            // make sure the piece has the energy to move that far and is
            // not fully damaged
            int steps = Math.abs(piece.x-nx) + Math.abs(piece.y-ny);
            int energy = steps * piece.energyPerStep();
            if (!piece.isAlive() || piece.energy < energy) {
                piece.pathPos = -1;
                updates.add(piece);
                return true;
            }

            // try moving the piece
            if (!movePiece(piece, nx, ny, updates, effects)) {
                return false;
            }

            // note that we want to update our piece
            updates.add(piece);

            // check to see if we've reached the end of our path
            if (path.reachedGoal(piece)) {
                piece.pathPos = -1;
                return true;
            }

            // otherwise see if we can make an additional move this turn
            nx = path.getNextX(piece);
            ny = path.getNextY(piece);
            if (!piece.canBonusMove(nx, ny)) {
                break;
            }
        }

        return false;
    }

    /**
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return true if the piece was moved, false if it was not movable
     * for some reason.
     */
    protected boolean movePiece (Piece piece, int x, int y, PieceSet updates,
                                 ArrayList<Effect> effects)
    {
        if (x < 0 || y < 0 || x >= _bangobj.board.getWidth() ||
            y >= _bangobj.board.getHeight()) {
            return false;
        }

        // validate that the move is legal (proper length, can traverse
        // all tiles along the way, no pieces intervene, etc.)
        if (!piece.canMoveTo(_bangobj.board, x, y)) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", x=" + x + ", y=" + y + "].");
            return false;
        }

        // calculate the distance we're moving (this should always be one
        // but we leave this in in case want to change things later)
        int steps = Math.abs(piece.x-x) + Math.abs(piece.y-y);

        // clone the piece so that we can investigate the hypothetical
        Piece hpiece = (Piece)piece.clone();
        hpiece.position(x, y);

        // ensure that we don't land on a piece that prevents us from
        // overlapping it and make a note of any piece that we land on
        // that does not prevent overlap
        ArrayList<Piece> lappers = _bangobj.getOverlappers(hpiece);
        Piece lapper = null;
        if (lappers != null) {
            for (Piece p : lappers) {
                if (p.preventsOverlap(hpiece)) {
                    return false;
                } else if (lapper != null) {
                    log.warning("Multiple overlapping pieces [mover=" + hpiece +
                                ", lap1=" + lapper + ", lap2=" + p + "].");
                } else {
                    lapper = p;
                }
            }
        }

        // if we were able to move, go ahead and update our real piece
        piece.position(x, y);

        // consume the energy needed to make this move (we checked that
        // this was possible before we even called movePiece)
        piece.consumeEnergy(steps);

        // interact with any piece occupying our target space
        if (lapper != null) {
            switch (piece.maybeInteract(lapper, effects)) {
            case CONSUMED:
                _bangobj.removeFromPieces(lapper.getKey());
                break;

            case ENTERED:
                // update the piece we entered as we likely modified it in
                // doing so
                updates.add(lapper);
                // TODO: generate a special event indicating that the
                // piece entered so that we can animate it
                _bangobj.removeFromPieces(piece.getKey());
                // short-circuit the remaining move processing
                return true;

            case INTERACTED:
                // update the piece we interacted with, we'll update
                // ourselves momentarily
                updates.add(lapper);
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

        return true;
    }

    /**
     * Called following each tick to determine whether or not new
     * bonuses should be added to the board.
     */
    protected void addBonuses (ArrayList<Piece> additions)
    {
        Piece[] pieces = _bangobj.getPieceArray();

        // first do some counting
        int[] alive = new int[_bangobj.players.length];
        int[] undamage = new int[_bangobj.players.length];
        int bonuses = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Bonus) {
                bonuses++;
            } else if (p.isAlive() && p.owner >= 0) {
                alive[p.owner]++;
                undamage[p.owner] += (100 - p.damage);
            }
        }
    }

    // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // cancel the board tick
        _ticker.cancel();
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
        gen.generate(bconfig, board, pieces);
//         SkirmishScenario scen = new SkirmishScenario();
        TestScenario scen = new TestScenario();
        scen.generate(bconfig, board, pieces);
        return board;
    }

    /** Triggers our board tick once every N seconds. */
    protected Interval _ticker = _ticker = new Interval(PresentsServer.omgr) {
        public void expired () {
            int nextTick = (_bangobj.tick + 1) % Short.MAX_VALUE;
            _bangobj.setTick((short)nextTick);
        }
    };

    /** Applies the shot and effect modifications associated with a tick
     * after the tick has been processed. */
    protected MessageListener _applier = new MessageListener() {
        public void messageReceived (MessageEvent event) {
            if (event.getName().equals("ticked")) {
                Object[] args = event.getArgs();
                postTick((Shot[])args[0], (Effect[])args[1]);
            }
        }
    };

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
