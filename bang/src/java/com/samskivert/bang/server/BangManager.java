//
// $Id$

package com.samskivert.bang.server;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Interval;

import com.threerings.util.Name;
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
import com.samskivert.bang.data.PieceDSet;
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
    public void purchasePiece (ClientObject caller, Piece piece)
    {
    }

    // documentation inherited from interface BangProvider
    public void readyToPlay (ClientObject caller)
    {
    }

    // documentation inherited from interface BangProvider
    public void move (ClientObject caller, int pieceId, short x, short y)
    {
        BodyObject user = (BodyObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            log.info("Rejecting move request [who=" + user.who() +
                     ", piece=" + piece + "].");
            return;
        }

        try {
            _bangobj.startTransaction();
            movePiece(piece, x, y);
        } finally {
            _bangobj.commitTransaction();
        }
    }

    // documentation inherited from interface BangProvider
    public void fire (ClientObject caller, int pieceId, int targetId)
    {
        BodyObject user = (BodyObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        Piece target = (Piece)_bangobj.pieces.get(targetId);
        if (piece == null || piece.owner != pidx || target == null) {
            log.info("Rejecting fire request [who=" + user.who() +
                     ", piece=" + piece + ", target=" + target + "].");
            return;
        }
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

        // set up the bang object
        _bangobj = (BangObject)_gameobj;
        _bangobj.setService(
            (BangMarshaller)PresentsServer.invmgr.registerDispatcher(
                new BangDispatcher(this), false));
//         _bangobj.addListener(_applier);

        // create our per-player arrays
        _bangobj.reserves = new int[getPlayerSlots()];
        _bangobj.funds = new int[getPlayerSlots()];
    }

    // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();
        PresentsServer.invmgr.clearDispatcher(_bangobj.service);
    }

//     // documentation inherited
//     protected void playersAllHere ()
//     {
//         // when the players all arrive, go into pre-game
// //         // start up the game if we're not a party game and if we haven't
// //         // already done so
// //         if (!isPartyGame() &&
// //             _gameobj.state == GameObject.AWAITING_PLAYERS) {
// //             startGame();
// //         }

//         startPreGame();
//     }

    /** Starts the pre-game buying phase. */
    protected void startPreGame ()
    {
        // clear out the readiness status of each player
        _ready.clear();

        // transition to the pre-game buying phase
        _bangobj.setState(BangObject.PRE_GAME);
    }

    // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // set up the game object
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        _bangobj.setBoard(createBoard(pieces));
        _bangobj.setPieces(new PieceDSet(pieces.iterator()));
        _bangobj.board.shadowPieces(pieces.iterator());

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

        // queue up the board tick
        _ticker.schedule(5000L, true);
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

//         // these shots will be communicated to the client for animation
//         ArrayList<Shot> shots = new ArrayList<Shot>();
//         // these effects will be communicated to the client and
//         // applied to the board
//         ArrayList<Effect> effects = new ArrayList<Effect>();

//         try {
//             // batch our tick update
//             _bangobj.startTransaction();

//             // randomize the piece array before executing moves to insure
//             // that no one is favored during potential conflicts
//             ArrayUtil.shuffle(pieces);

//             // TODO: maybe now sort them by remaining energy or lowest
//             // damage to give players some general idea as to who goes
//             // first

//             // these pieces will be updated
//             PieceSet updates = new PieceSet();

//             // move all of the pieces along their paths
//             for (int ii = 0; ii < pieces.length; ii++) {
//                 Piece piece = pieces[ii];
//                 PiecePath path = _paths.get(piece.pieceId);
//                 if (path == null) {
//                     continue;
//                 }

//                 if (tickPath(piece, path, updates, effects)) {
//                     log.fine("Finished " + path + ".");
//                     _paths.remove(piece.pieceId);
//                 }
//             }

//             // recreate our pieces array; pieces may have been removed
//             pieces = _bangobj.getPieceArray();

//             // then give any piece a chance to react to the state of the board
//             // now that everyone has moved
//             for (int ii = 0; ii < pieces.length; ii++) {
//                 Piece piece = pieces[ii];
//                 // skip pieces that were eaten or are fully damaged
//                 if (!_bangobj.pieces.containsKey(piece.pieceId) ||
//                     piece.damage >= 100) {
//                     continue;
//                 }
//                 piece.react(_bangobj, pieces, updates, shots);
//             }

//             // finally update the pieces that need updating
//             for (Piece piece : updates.values()) {
//                 // skip pieces that were eaten
//                 if (!_bangobj.pieces.containsKey(piece.pieceId)) {
//                     continue;
//                 }
//                 _bangobj.updatePieces(piece);
//             }

//         } finally {
//             _bangobj.commitTransaction();
//         }

//         // update our board "shadow"
//         _bangobj.board.prepareShadow();
//         _bangobj.board.shadowPieces(_bangobj.pieces.iterator());

//         // now "prepare" the effects which will determine the exact result
//         // of the effects and potentially make intermediate modifications
//         // to our board shadow to ensure that subsequent effects operate
//         // properly
//         Effect[] efvec = new Effect[effects.size()];
//         effects.toArray(efvec);
//         for (int ii = 0; ii < efvec.length; ii++) {
//             efvec[ii].prepare(_bangobj);
//         }

//         // this lets the clients know the updates are done and gives
//         // them a chance to to post-tick processing
//         Object[] args = new Object[] {
//             shots.toArray(new Shot[shots.size()]), efvec
//         };
//         _bangobj.postMessage("ticked", args);
//     }

//     /** Applies the effects of shots fired and effects after the normal
//      * tick processing has completed. The client will do this same
//      * processing to its data. */
//     protected void postTick (Shot[] shots, Effect[] effects)
//     {
//         // first apply the shots
//         for (int ii = 0; ii < shots.length; ii++) {
//             Piece p = _bangobj.applyShot(shots[ii]);
//             if (p != null && !p.isAlive() && p.removeWhenDead()) {
//                 // this will happen on both the client and server, so we
//                 // don't use the distributed mechanism
//                 _bangobj.pieces.removeDirect(p);
//             }
//         }

//         // next apply the effects
//         for (int ii = 0; ii < effects.length; ii++) {
//             effects[ii].apply(_bangobj, null);
//         }

        try {
            _bangobj.startTransaction();
            // potentially create and add new bonuses
            addBonuses();
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

//     /**
//      * Moves the supplied piece further along its configured path.
//      *
//      * @return true if the piece reached the final goal on the path, false
//      * if not.
//      */
//     protected boolean tickPath (Piece piece, PiecePath path, PieceSet updates,
//                                 ArrayList<Effect> effects)
//     {
//         log.fine("Moving " + path + ".");
//         int nx = path.getNextX(piece), ny = path.getNextY(piece);

//         for (int ii = 0; ii < 2; ii++) {
//             // make sure the piece has the energy to move that far and is
//             // not fully damaged
//             int steps = Math.abs(piece.x-nx) + Math.abs(piece.y-ny);
//             int energy = steps * piece.energyPerStep();
//             if (!piece.isAlive() || piece.energy < energy) {
//                 piece.pathPos = -1;
//                 updates.add(piece);
//                 return true;
//             }

//             // try moving the piece
//             if (!movePiece(piece, nx, ny, updates, effects)) {
//                 return false;
//             }

//             // note that we want to update our piece
//             updates.add(piece);

//             // check to see if we've reached the end of our path
//             if (path.reachedGoal(piece)) {
//                 piece.pathPos = -1;
//                 return true;
//             }

//             // otherwise see if we can make an additional move this turn
//             nx = path.getNextX(piece);
//             ny = path.getNextY(piece);
//             if (!piece.canBonusMove(nx, ny)) {
//                 break;
//             }
//         }

//         return false;
//     }

    /**
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return true if the piece was moved, false if it was not movable
     * for some reason.
     */
    protected boolean movePiece (Piece piece, int x, int y)
    {
        // make sure we are alive, have energy and are ready to move
        int steps = Math.abs(piece.x-x) + Math.abs(piece.y-y);
        int energy = steps * piece.energyPerStep();
        if (!piece.isAlive() || piece.energy < energy ||
            piece.ticksUntilMovable(_bangobj.tick) > 0) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", alive=" + piece.isAlive() +
                        ", denergy=" + (energy - piece.energy) +
                        ", mticks=" + piece.ticksUntilMovable(_bangobj.tick) +
                        "].");
            return false;
        }

        // validate that the move is legal
        _moves.clear();
        _bangobj.board.computeMoves(piece, _moves);
        if (!_moves.contains(x, y)) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", x=" + x + ", y=" + y + "].");
            return false;
        }

        // clone and move the piece
        Piece mpiece = (Piece)piece.clone();
        mpiece.position(x, y);
        mpiece.lastMoved = _bangobj.tick;
        mpiece.consumeEnergy(steps);

        // ensure that we don't land on a piece that prevents us from
        // overlapping it and make a note of any piece that we land on
        // that does not prevent overlap
        ArrayList<Piece> lappers = _bangobj.getOverlappers(mpiece);
        Piece lapper = null;
        if (lappers != null) {
            for (Piece p : lappers) {
                if (p.preventsOverlap(mpiece)) {
                    return false;
                } else if (lapper != null) {
                    log.warning("Multiple overlapping pieces [mover=" + mpiece +
                                ", lap1=" + lapper + ", lap2=" + p + "].");
                } else {
                    lapper = p;
                }
            }
        }

        // update our board shadow
        _bangobj.board.updateShadow(piece, mpiece);

        // interact with any piece occupying our target space
        if (lapper != null) {
            switch (mpiece.maybeInteract(lapper, _effects)) {
            case CONSUMED:
                _bangobj.removeFromPieces(lapper.getKey());
                break;

            case ENTERED:
                // update the piece we entered as we likely modified it in
                // doing so
                _bangobj.updatePieces(lapper);
                // TODO: generate a special event indicating that the
                // piece entered so that we can animate it
                _bangobj.removeFromPieces(mpiece.getKey());
                // short-circuit the remaining move processing
                return true;

            case INTERACTED:
                // update the piece we interacted with, we'll update
                // ourselves momentarily
                _bangobj.updatePieces(lapper);
                break;

            case NOTHING:
                break;
            }
        }

        // update the piece in the distributed set
        _bangobj.updatePieces(mpiece);

        // finally effect and effects
        for (Effect effect : _effects) {
            effect.prepare(_bangobj);
            effect.apply(_bangobj, null);
            _bangobj.setEffect(effect);
        }
        _effects.clear();

        return true;
    }

    /**
     * Called following each tick to determine whether or not new bonuses
     * should be added to the board.
     */
    protected void addBonuses ()
    {
        Piece[] pieces = _bangobj.getPieceArray();

        // first do some counting
        int pcount = _bangobj.players.length, tpower = 0, bonuses = 0;
        int[] alive = new int[pcount], power = new int[pcount];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Bonus) {
                bonuses++;
            } else if (p.isAlive() && p.owner >= 0) {
                alive[p.owner]++;
                int pp = (100 - p.damage);
                power[p.owner] += pp;
                tpower += pp;
            }
        }

        // have a 1 in 20 chance of adding a bonus for each player for
        // which there is not already a bonus on the board
        int bprob = (pcount - bonuses), rando = RandomUtil.getInt(200);
        if (bprob == 0 || rando > bprob*10) {
//             log.info("No bonus, probability " + bprob + " in 10 (" +
//                      rando + ").");
            return;
        }

//         // determine the player with the lowest power
//         int lowidx = RandomUtil.getInt(pcount);
//         // start with a random non-zero power having player
//         for (int ii = 0; ii < pcount; ii++) {
//             if (power[lowidx] != 0) {
//                 break;
//             } else {
//                 lowidx = (lowidx + 1) % pcount;
//             }
//         }
//         // then look for anyone with less power
//         for (int ii = 0; ii < pcount; ii++) {
//             int ppower = power[ii];
//             if (ppower > 0 && ppower < power[lowidx]) {
//                 lowidx = ii;
//             }
//         }

//         // if that player has less than 50% 
//         log.info("Placing bonus near " + _bangobj.players[lowidx] + ".");

//         // now compute the centroid of their live pieces
//         int ppieces = 0, sumx = 0, sumy = 0;
//         for (int ii = 0; ii < pieces.length; ii++) {
//             Piece p = pieces[ii];
//             if (p.owner == lowidx && p.isAlive()) {
//                 ppieces++;
//                 sumx += p.x;
//                 sumy += p.y;
//             }
//         }
//         int cx = sumx/ppieces, cy = sumy/ppieces;

        int bwid = _bangobj.board.getWidth(), bhei = _bangobj.board.getHeight();

//         // find a position randomly dispersed from there
//         cx = cx - bwid/10 + RandomUtil.getInt(bwid/5);
//         cy = cy - bhei/10 + RandomUtil.getInt(bhei/5);

        // pick a random position on the board
        int cx = RandomUtil.getInt(bwid), cy = RandomUtil.getInt(bhei);

        // locate the nearest spot to that which can be occupied by our piece
        Point bspot = _bangobj.board.getOccupiableSpot(cx, cy, 3);
        if (bspot == null) {
            log.info("Dropping bonus for lack of occupiable location " +
                     "[cx=" + cx + ", cy=" + cy + "].");
            return;
        }

        // now we have a location, determine which player has the shortest
        // path to this bonus and use that player's power to determine how
        // powerful a bonus to deploy
        int spath = Integer.MAX_VALUE, spower = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece piece = pieces[ii];
            if (piece.owner < 0 || !piece.isAlive()) {
                continue;
            }
            List path = _bangobj.board.computePath(
                piece, bspot.x, bspot.y);
            if (path == null) {
                log.warning("Unable to compute path to " + bspot +
                            " for " + piece.info() + "?");
                continue;
            }
            log.info(piece.info() + " is " + path.size() +
                     " steps from " + bspot);
            if (path.size() < spath) {
                spath = path.size();
                spower = power[piece.owner];
            }
        }

        Piece bonus;
        if (Math.random() > 1.0 * spower / tpower) {
            bonus = new Bonus(Bonus.Type.DUPLICATE);
        } else {
            bonus = new Bonus(Bonus.Type.REPAIR);
        }
        bonus.assignPieceId();
        bonus.position(bspot.x, bspot.y);
        _bangobj.addToPieces(bonus);

        log.info("Shortest path: " + spath + ", power: " + spower +
                 " of " + tpower + " -> " + bonus.info());
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
//         TestScenario scen = new TestScenario();
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

//     /** Applies the shot and effect modifications associated with a tick
//      * after the tick has been processed. */
//     protected MessageListener _applier = new MessageListener() {
//         public void messageReceived (MessageEvent event) {
//             if (event.getName().equals("ticked")) {
//                 Object[] args = event.getArgs();
//                 postTick((Shot[])args[0], (Effect[])args[1]);
//             }
//         }
//     };

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Used to indicate when all players are ready. */
    protected ArrayIntSet _ready = new ArrayIntSet();

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to compute a piece's potential moves when validating a move
     * request. */
    protected PointSet _moves = new PointSet();

    /** Used to track effects during a move. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();
}
