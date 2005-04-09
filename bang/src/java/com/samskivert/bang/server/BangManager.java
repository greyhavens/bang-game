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
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;
import com.threerings.util.RandomUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsServer;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.data.BodyObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.BangBoard;
import com.samskivert.bang.data.BangCodes;
import com.samskivert.bang.data.BangMarshaller;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.PieceDSet;
import com.samskivert.bang.data.Terrain;
import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.effect.ShotEffect;
import com.samskivert.bang.data.generate.CompoundGenerator;
import com.samskivert.bang.data.generate.ScenarioGenerator;
import com.samskivert.bang.data.generate.SkirmishScenario;
import com.samskivert.bang.data.generate.TestScenario;
import com.samskivert.bang.data.piece.Bonus;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.PlayerPiece;
import com.samskivert.bang.data.surprise.MissileSurprise;
import com.samskivert.bang.data.surprise.Surprise;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

import static com.samskivert.bang.Log.log;

/**
 * Handles the server-side of the game.
 */
public class BangManager extends GameManager
    implements BangCodes, BangProvider
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
    public void move (ClientObject caller, int pieceId, short x, short y,
                      int targetId, BangService.InvocationListener il)
        throws InvocationException
    {
        BodyObject user = (BodyObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            log.info("Rejecting move request [who=" + user.who() +
                     ", piece=" + piece + "].");
            return;
        }
        if (piece.ticksUntilMovable(_bangobj.tick) > 0) {
            log.info("Rejecting premature move/fire request [who=" + user.who() +
                     ", piece=" + piece.info() + "].");
            return;
        }

        Piece target = (Piece)_bangobj.pieces.get(targetId);
        Piece mpiece = null;
        try {
            _bangobj.startTransaction();

            // if they specified a non-NOOP move, execute it
            if (x != piece.x || y != piece.y) {
                mpiece = movePiece(piece, x, y);
                if (mpiece == null) {
                    throw new InvocationException(MOVE_BLOCKED);
                }
            }

            // if they specified a target, shoot at it
            if (target != null) {
                _attacks.clear();
                _bangobj.board.computeAttacks(
                    piece.getFireDistance(), x, y, _attacks);
                if (_attacks.contains(target.x, target.y)) {
                    ShotEffect effect = piece.shoot(target);
                    effect.prepare(_bangobj);
                    _bangobj.setEffect(effect);
                    // if they did not move in this same action, we need
                    // to set their last acted tick
                    if (mpiece == null) {
                        piece.lastActed = _bangobj.tick;
                        _bangobj.updatePieces(piece);
                    }
                } else {
                    throw new InvocationException(TARGET_MOVED);
                }
            }

        } finally {
            _bangobj.commitTransaction();
        }
    }

    // documentation inherited from interface BangProvider
    public void surprise (ClientObject caller, int surpriseId, short x, short y)
    {
        BodyObject user = (BodyObject)caller;
        Surprise s = (Surprise)_bangobj.surprises.get(surpriseId);
        if (s == null || s.owner != _bangobj.getPlayerIndex(user.username)) {
            log.info("Rejecting invalid surprise request [who=" + user.who() +
                     ", sid=" + surpriseId + ", surprise=" + s + "].");
            return;
        }

        log.info("surprise! " + s);

        // remove it from their list
        _bangobj.removeFromSurprises(surpriseId);

        // and activate it
        Effect effect = s.activate(x, y);
        effect.prepare(_bangobj);
        _bangobj.setEffect(effect);
    }

    // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.TICK)) {
            tick(_bangobj.tick);

        } else if (name.equals(BangObject.EFFECT)) {
            _bangobj.effect.apply(_bangobj, null);

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
        _ticker.schedule(10000L, true);
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

    /**
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return the cloned and moved piece if the piece was moved, null if
     * it was not movable for some reason.
     */
    protected Piece movePiece (Piece piece, int x, int y)
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
            return null;
        }

        // validate that the move is legal
        _moves.clear();
        _bangobj.board.computeMoves(piece, _moves);
        if (!_moves.contains(x, y)) {
            log.warning("Piece requested illegal move [piece=" + piece +
                        ", x=" + x + ", y=" + y + "].");
            return null;
        }

        // clone and move the piece
        Piece mpiece = (Piece)piece.clone();
        mpiece.position(x, y);
        mpiece.lastActed = _bangobj.tick;
        mpiece.consumeEnergy(steps);

        // ensure that we don't land on a piece that prevents us from
        // overlapping it and make a note of any piece that we land on
        // that does not prevent overlap
        ArrayList<Piece> lappers = _bangobj.getOverlappers(mpiece);
        Piece lapper = null;
        if (lappers != null) {
            for (Piece p : lappers) {
                if (p.preventsOverlap(mpiece)) {
                    return null;
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
                return mpiece;

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
            _bangobj.setEffect(effect);
        }
        _effects.clear();

        return mpiece;
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

        // have a 1 in 15 chance of adding a bonus for each player for
        // which there is not already a bonus on the board
        int bprob = (pcount - bonuses), rando = RandomUtil.getInt(150);
        if (bprob == 0 || rando > bprob*10) {
//             log.info("No bonus, probability " + bprob + " in 10 (" +
//                      rando + ").");
            return;
        }

        // determine the player with the lowest power
        int lowidx = RandomUtil.getInt(pcount);
        // start with a random non-zero power having player
        for (int ii = 0; ii < pcount; ii++) {
            if (power[lowidx] != 0) {
                break;
            } else {
                lowidx = (lowidx + 1) % pcount;
            }
        }
        // then look for anyone with less power
        for (int ii = 0; ii < pcount; ii++) {
            int ppower = power[ii];
            if (ppower > 0 && ppower < power[lowidx]) {
                lowidx = ii;
            }
        }

        log.info("Placing bonus near " + _bangobj.players[lowidx] +
                 " [idx=" + lowidx +
                 ", power=" + StringUtil.toString(power) + "].");

        // now compute the centroid of their live pieces
        int ppieces = 0, sumx = 0, sumy = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.owner == lowidx && p.isAlive()) {
                ppieces++;
                sumx += p.x;
                sumy += p.y;
            }
        }
        int cx = sumx/ppieces, cy = sumy/ppieces;

        int bwid = _bangobj.board.getWidth(), bhei = _bangobj.board.getHeight();

//         // find a position randomly dispersed from there
//         cx = cx - bwid/10 + RandomUtil.getInt(bwid/5);
//         cy = cy - bhei/10 + RandomUtil.getInt(bhei/5);

//         // pick a random position on the board
//         int cx = RandomUtil.getInt(bwid), cy = RandomUtil.getInt(bhei);

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
        double pfact = 1.0 * spower / tpower;
        if (pfact < 0.2) {
            bonus = new Bonus(Bonus.Type.SURPRISE);
        } else if (Math.random() > pfact) {
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
        ScenarioGenerator scen = null;
        if (System.getProperty("test") != null) {
            scen = new TestScenario();
        } else {
            scen = new SkirmishScenario();
        }
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

    /** Used to indicate when all players are ready. */
    protected ArrayIntSet _ready = new ArrayIntSet();

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to compute a piece's potential moves or attacks when
     * validating a move request. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    /** Used to track effects during a move. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();
}
