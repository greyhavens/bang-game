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
import com.samskivert.util.IntIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
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
import com.samskivert.bang.data.piece.BonusFactory;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.data.piece.PlayerPiece;
import com.samskivert.bang.data.surprise.RepairSurprise;
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
            log.info("Rejecting premature move/fire request " +
                     "[who=" + user.who() + ", piece=" + piece.info() + "].");
            return;
        }

        Piece target = (Piece)_bangobj.pieces.get(targetId);
        Piece mpiece = null;
        try {
            _bangobj.startTransaction();

            // if they specified a non-NOOP move, execute it
            if (x != piece.x || y != piece.y) {
                mpiece = movePiece(user, piece, x, y);
                if (mpiece == null) {
                    throw new InvocationException(MOVE_BLOCKED);
                }
            }

            // if they specified a target, shoot at it
            if (target != null) {
                // make sure the target is valid
                if (!piece.validTarget(target)) {
                    log.info("Target not valid " + target + ".");
                    // target already dead or something
                    return;
                }

                // make sure the target is still within range
                _attacks.clear();
                _bangobj.board.computeAttacks(
                    piece.getFireDistance(), x, y, _attacks);
                if (!_attacks.contains(target.x, target.y)) {
                    throw new InvocationException(TARGET_MOVED);
                }

                ShotEffect effect = piece.shoot(target);
                effect.prepare(_bangobj, _damage);
                _bangobj.setEffect(effect);
                recordDamage(user, _damage);

                // if they did not move in this same action, we need to
                // set their last acted tick
                if (mpiece == null) {
                    piece.lastActed = _bangobj.tick;
                    _bangobj.updatePieces(piece);
                }
            }

            // finally update our game statistics
            _bangobj.updateStats();

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
        effect.prepare(_bangobj, _damage);
        _bangobj.setEffect(effect);
        recordDamage(user, _damage);
    }

    // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.TICK)) {
            tick(_bangobj.tick);

        } else if (name.equals(BangObject.EFFECT)) {
            ((Effect)event.getValue()).apply(_bangobj, null);

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
        _bangobj.pstats = new BangObject.PlayerData[getPlayerSlots()];
        for (int ii = 0; ii < _bangobj.pstats.length; ii++) {
            _bangobj.pstats[ii] = new BangObject.PlayerData();
        }
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

        // TEMP: give everyone a repair surprise to start
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            RepairSurprise s = new RepairSurprise();
            s.init(ii);
            _bangobj.addToSurprises(s);
        }

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

        // queue up the board tick
        int avgPer = _bangobj.getAveragePieceCount();
        _ticker.schedule(avgPer * 2000L, false);
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
            if (addBonuses()) {
                _bangobj.updateStats();
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
     * Attempts to move the specified piece to the specified coordinates.
     * Various checks are made to ensure that it is a legal move.
     *
     * @return the cloned and moved piece if the piece was moved, null if
     * it was not movable for some reason.
     */
    protected Piece movePiece (BodyObject user, Piece piece, int x, int y)
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
            effect.prepare(_bangobj, _damage);
            _bangobj.setEffect(effect);
            recordDamage(user, _damage);
        }
        _effects.clear();

        return mpiece;
    }

    /**
     * Called following each tick to determine whether or not new bonuses
     * should be added to the board.
     *
     * @return true if a bonus was added, false if not.
     */
    protected boolean addBonuses ()
    {
        Piece[] pieces = _bangobj.getPieceArray();

//         int[] nonactors = new int[pcount];
//         short prevTick = (short)(_bangobj.tick-1);
//         for (int ii = 0; ii < pieces.length; ii++) {
//             Piece p = pieces[ii];
//             if (p.isAlive() && p.owner >= 0) {
//                 if (p.ticksUntilMovable(prevTick) == 0) {
//                     nonactors[p.owner]++;
//                 }
//             }
//         }
//         log.info("Non-actors: " + StringUtil.toString(nonactors));

        // have a 1 in 5 chance of adding a bonus for each live player for
        // which there is not already a bonus on the board
        int bprob = (_bangobj.gstats.livePlayers - _bangobj.gstats.bonuses);
        int rando = RandomUtil.getInt(50);
        if (bprob == 0 || rando > bprob*10) {
//             log.info("No bonus, probability " + bprob + " in 10 (" +
//                      rando + ").");
            return false;
        }

        // determine whether everyone is within 20% of the average score
        boolean outlier = _bangobj.havePowerOutlier(0.1);

        // select an algorithm by which to place the bonus:
        // - if we are very early in the game, just put it in the middle
        //   of the board
        // - if the players are mostly even, ?
        // - if some player is in last place, put it near them

        Point spot;
        if (outlier) {
            spot = findSpotNearChuckanut(pieces);
        } else {
            spot = new Point(_bangobj.board.getWidth()/2,
                             _bangobj.board.getHeight()/2);
        }

        // locate the nearest spot to that which can be occupied by our piece
        Point bspot = _bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            log.info("Dropping bonus for lack of occupiable location " +
                     spot + ".");
            return false;
        }

        // now we have a location, determine which player has the shortest
        // path to this bonus and use that player's power to determine how
        // powerful a bonus to deploy
        int spath = Integer.MAX_VALUE, spidx = -1;
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
//             log.info(piece.info() + " is " + path.size() +
//                      " steps from " + bspot);
            if (path.size() < spath) {
                spath = path.size();
                spidx = piece.owner;
            }
        }

        Piece bonus;
        double pfact = _bangobj.pstats[spidx].powerFactor;
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

        log.info("Placed bonus: " + bonus.info());
        return true;
    }

    /** Helper function for {@link #addBonuses}. */
    protected Point findSpotNearChuckanut (Piece[] pieces)
    {
        int lowidx = _bangobj.getLowestPowerIndex();
        log.info("Placing bonus near " + _bangobj.players[lowidx] + ".");

        // compute and return the centroid of their live pieces
        int ppieces = 0, sumx = 0, sumy = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.owner == lowidx && p.isAlive()) {
                ppieces++;
                sumx += p.x;
                sumy += p.y;
            }
        }
        return new Point(sumx/ppieces, sumy/ppieces);
    }        

    /** Records damage done by the specified user to various pieces. */
    protected void recordDamage (BodyObject user, IntIntMap damage)
    {
        int pidx = _bangobj.getPlayerIndex(user.username);
        if (pidx < 0) {
            log.warning("Requested to record damage by non-player!? " +
                        "[user=" + user.who() + ", damage=" + damage + "].");
            return;
        }

        int total = 0;
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            int ddone = damage.get(ii);
            if (ddone <= 0) {
                continue;
            }
            if (ii == pidx) {
                // make them lose 150%
                ddone = -3 * ddone / 2;
                // report the boochage
                String msg = MessageBundle.tcompose(
                    "m.self_damage", user.username);
                SpeakProvider.sendInfo(_bangobj, BangCodes.BANG_MSGS, msg);
            }
            total += ddone;
        }
        _bangobj.setFundsAt(_bangobj.funds[pidx] + total, pidx);

        // finally clear out the damage index
        damage.clear();
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
            if (_bangobj.isInPlay()) {
                // queue ourselves up to expire in a time proportional to
                // the average number of pieces per player
                int avgPer = _bangobj.getAveragePieceCount();
                _ticker.schedule(2000L * avgPer);
            }
        }
    };

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** This guy is used to determine which bonuses to give out. */
    protected BonusFactory _bfactory = new BonusFactory();

    /** Used to indicate when all players are ready. */
    protected ArrayIntSet _ready = new ArrayIntSet();

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to record damage done during an attack. */
    protected IntIntMap _damage = new IntIntMap();

    /** Used to compute a piece's potential moves or attacks when
     * validating a move request. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    /** Used to track effects during a move. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();
}
