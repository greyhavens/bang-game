//
// $Id$

package com.samskivert.bang.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
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
import com.samskivert.bang.data.generate.SkirmishScenario;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.PlayerPiece;
import com.samskivert.bang.util.PieceSet;

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

        // next check to see whether anyone's pieces have energy remaining
        _havers.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof PlayerPiece) &&
                pieces[ii].canTakeStep()) {
                _havers.add(pieces[ii].owner);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            endGame();
            return;
        }

        try {
            // batch our tick update
            _bangobj.startTransaction();
            // these pieces will be updated
            PieceSet updates = new PieceSet();

            // move all of our pieces along any path they have configured
            Iterator<PiecePath> iter = _paths.values().iterator();
            while (iter.hasNext()) {
                PiecePath path = iter.next();
                Piece piece = (Piece)_bangobj.pieces.get(path.pieceId);
                if (piece == null || tickPath(piece, path, updates)) {
                    log.fine("Finished " + path + ".");
                    // if the piece has gone away, or if we complete our path,
                    // remove it
                    iter.remove();
                }
            }

            // recreate our pieces array; pieces may have been removed
            pieces = _bangobj.getPieceArray();

            // then give any piece a chance to react to the state of the board
            // now that everyone has moved
            for (int ii = 0; ii < pieces.length; ii++) {
                Piece piece = pieces[ii];
                // skip pieces that were eaten
                if (!_bangobj.pieces.containsKey(piece.pieceId)) {
                    continue;
                }
                piece.react(_bangobj, pieces, updates);
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
     * @return true if the bug reached the final goal on the path, false
     * if not.
     */
    protected boolean tickPath (Piece piece, PiecePath path, PieceSet updates)
    {
        log.fine("Moving " + path + ".");
        int nx = path.getNextX(piece), ny = path.getNextY(piece);

        // make sure the piece has the energy to move that far
        int steps = Math.abs(piece.x-nx) + Math.abs(piece.y-ny);
        if (piece.energy < steps * piece.energyPerStep()) {
            log.info("Piece out of energy [piece=" + piece + "].");
            piece.pathPos = -1;
            updates.add(piece);
            return true;
        }

        // try moving the piece
        if (!movePiece(piece, nx, ny, updates)) {
            return false;
        }

        // check to see if we've reached the end of our path
        boolean reachedGoal = path.reachedGoal(piece);
        if (reachedGoal) {
            piece.pathPos = -1;
        }

        // note that we want to update our piece
        updates.add(piece);
        return reachedGoal;
    }

    /**
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return true if the piece was moved, false if it was not movable
     * for some reason.
     */
    protected boolean movePiece (Piece piece, int x, int y, PieceSet updates)
    {
        // validate that the move is legal (proper length, can traverse
        // all tiles along the way, no pieces intervene, etc.)
        if (!piece.canMoveTo(_bangobj.board, x, y)) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", x=" + x + ", y=" + y + "].");
            return false;
        }

        // calculate the distance we're moving
        int steps = Math.abs(piece.x-x) + Math.abs(piece.y-y);

        // clone the piece so that we can investigate the hypothetical
        Piece hpiece = (Piece)piece.clone();
        hpiece.position(x, y);

        // ensure that intervening pieces do not block this move; also
        // track any piece that we end up overlapping
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
            switch (piece.maybeInteract(lapper)) {
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
        SkirmishScenario scen = new SkirmishScenario();
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

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
