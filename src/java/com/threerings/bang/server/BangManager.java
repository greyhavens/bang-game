//
// $Id$

package com.threerings.bang.server;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

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

import com.threerings.bang.data.card.AreaRepair;
import com.threerings.bang.data.card.Card;
import com.threerings.bang.data.effect.Effect;
import com.threerings.bang.data.effect.ShotEffect;
import com.threerings.bang.data.generate.SkirmishScenario;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.BonusMarker;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.PlayerPiece;
import com.threerings.bang.data.piece.Unit;

import com.threerings.bang.client.BangService;
import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangMarshaller;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.PieceDSet;
import com.threerings.bang.data.Terrain;
import com.threerings.bang.util.BoardUtil;
import com.threerings.bang.util.PieceSet;
import com.threerings.bang.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side of the game.
 */
public class BangManager extends GameManager
    implements BangCodes, BangProvider
{
    // documentation inherited from interface BangProvider
    public void purchasePieces (ClientObject caller, Piece[] pieces)
    {
        BodyObject user = (BodyObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);
        if (pidx == -1) {
            log.warning("Request to purchase pieces by non-player " +
                        "[who=" + user.who() + "].");
            return;
        }
        purchasePieces(pidx, pieces);
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
    public void playCard (ClientObject caller, int cardId, short x, short y)
    {
        BodyObject user = (BodyObject)caller;
        Card card = (Card)_bangobj.cards.get(cardId);
        if (card == null ||
            card.owner != _bangobj.getPlayerIndex(user.username)) {
            log.info("Rejecting invalid card request [who=" + user.who() +
                     ", sid=" + cardId + ", card=" + card + "].");
            return;
        }

        log.info("Playing card: " + card);

        // remove it from their list
        _bangobj.removeFromCards(cardId);

        // and activate it
        Effect effect = card.activate(x, y);
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
        _bconfig = (BangConfig)_gameconfig;

        // create our per-player arrays
        _bangobj.reserves = new int[getPlayerSlots()];
        Arrays.fill(_bangobj.reserves, _bconfig.startingCash);
        _bangobj.points = new int[getPlayerSlots()];
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

    // documentation inherited
    protected void playersAllHere ()
    {
        // when the players all arrive, go into the buying phase
        startRound();
    }

    /** Starts the pre-game buying phase. */
    protected void startRound ()
    {
        // clear out the readiness status of each player
        _ready.clear();
        _purchases.clear();

        // transition to the pre-game buying phase
        _bangobj.setState(BangObject.PRE_ROUND);

        // configure purchases for our AIs
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            if (isAI(ii) || isTest()) {
                Piece[] pieces = new Piece[] {
                    Unit.getUnit("artillery"), Unit.getUnit("steamgunman"),
                    Unit.getUnit("dirigible") };
                purchasePieces(ii, pieces);
            }
        }
    }

    /**
     * Configures the specified player's purchases for this round and
     * starts the game if they are the last to configure.
     */
    protected void purchasePieces (int pidx, Piece[] pieces)
    {
        // total up the cost
        int totalCost = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            totalCost += pieces[ii].getCost();
        }
        if (totalCost > _bangobj.reserves[pidx]) {
            log.warning("Rejecting bogus purchase request " +
                        "[who=" + _bangobj.players[pidx] +
                        ", total=" + totalCost +
                        ", funds=" + _bangobj.reserves[pidx] + "].");
            return;
        }

        // initialize and prepare the pieces
        for (int ii = 0; ii < pieces.length; ii++) {
            pieces[ii].init();
            pieces[ii].owner = pidx;
            _purchases.add(pieces[ii]);
        }

        // finally decrement their funds
        _bangobj.setReservesAt(_bangobj.reserves[pidx] - totalCost, pidx);

        // note that this player is ready and potentially fire away
        _ready.add(pidx);
        if (_ready.size() == getPlayerSlots()) {
            startGame();
        }
    }

    // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // create a fresh knockout array
        _knockoutOrder = new int[getPlayerSlots()];

        // set up the game object
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        _bangobj.setBoard(createBoard(pieces));

        // extract the bonus spawn markers from the pieces array
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (p instanceof BonusMarker) {
                _bonusSpots.add(p.x, p.y);
                iter.remove();
            }
        }
        _bangobj.setPieces(new PieceDSet(pieces.iterator()));
        _bangobj.board.shadowPieces(pieces.iterator());

        // TEMP: give everyone an area repair to start
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            AreaRepair card = new AreaRepair();
            card.init(_bangobj, ii);
            _bangobj.addToCards(card);
        }

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

        // queue up the board tick
        int avgPer = _bangobj.getAveragePieceCount();
        _ticker.schedule(avgPer * getBaseTick(), false);
        _bangobj.tick = (short)0;
    }

    /**
     * Called when the board tick is incremented.
     */
    protected void tick (short tick)
    {
        log.fine("Ticking [tick=" + tick +
                 ", pcount=" + _bangobj.pieces.size() + "].");

        Piece[] pieces = _bangobj.getPieceArray();

        // allow pieces to tick down and possibly die
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.isAlive() && p.tick(tick)) {
                // if they died, possibly remove them from the board
                if (!p.isAlive() && p.removeWhenDead()) {
                    _bangobj.removePieceDirect(p);
                }
            }
        }

        // next check to see whether anyone's pieces are still alive
        _havers.clear();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof PlayerPiece) &&
                pieces[ii].isAlive()) {
                _havers.add(pieces[ii].owner);
            }
        }

        // score points for anyone who is knocked out as of this tick
        int score = IntListUtil.getMaxValue(_knockoutOrder) + 1;
        for (int ii = 0; ii < _knockoutOrder.length; ii++) {
            if (_knockoutOrder[ii] == 0 && !_havers.contains(ii)) {
                _knockoutOrder[ii] = score;
                _bangobj.setPointsAt(_bangobj.points[ii] + score, ii);
                String msg = MessageBundle.tcompose(
                    "m.knocked_out", _bangobj.players[ii]);
                SpeakProvider.sendInfo(_bangobj, BangCodes.BANG_MSGS, msg);
            }
        }

        // the game ends when one or zero players are left standing
        if (_havers.size() < 2) {
            // score points for the last player standing
            int winidx = _havers.get(0);
            _bangobj.setPointsAt(_bangobj.points[winidx] + score + 1, winidx);

            // if this is the last round, end the game
            log.info("round " + _bangobj.roundId +
                     ", rounds: " + _bconfig.rounds);
            if (_bangobj.roundId == _bconfig.rounds) {
                // assign final points based on total remaining cash
                for (int ii = 0; ii < getPlayerSlots(); ii++) {
                    int tcash = _bangobj.funds[ii] + _bangobj.reserves[ii];
                    int points = tcash / 250;
                    if (points > 0) {
                        _bangobj.setPointsAt(_bangobj.points[ii] + points, ii);
                        String msg = MessageBundle.tcompose(
                            "m.cash_score", _bangobj.players[ii],
                            "" + points, "" + tcash);
                        SpeakProvider.sendInfo(
                            _bangobj, BangCodes.BANG_MSGS, msg);
                    }
                }
                endGame();
            } else {
                _bangobj.setState(BangObject.POST_ROUND);
                // give them a moment to stare at the board then end the round
                new Interval(PresentsServer.omgr) {
                    public void expired () {
                        endRound();
                    }
                }.schedule(5000L);
            }

            // cancel the board tick
            _ticker.cancel();
            return;
        }

        try {
            _bangobj.startTransaction();
            // potentially create and add new bonuses
            if (addBonus()) {
                _bangobj.updateStats();
            }
        } finally {
            _bangobj.commitTransaction();
        }
    }

    protected void endRound ()
    {
        // transfer players winnings to their reserves
        for (int ii = 0; ii < _bangobj.funds.length; ii++) {
            _bangobj.reserves[ii] = Math.max(
                _bangobj.reserves[ii] + _bangobj.funds[ii],
                _bconfig.startingCash/2);
            _bangobj.funds[ii] = 0;
        }
        _bangobj.setReserves(_bangobj.reserves);
        _bangobj.setFunds(_bangobj.funds);

        // start the next round
        startRound();
    }

    @Override // documentation inherited
    protected void assignWinners (boolean[] winners)
    {
        int[] windexes = IntListUtil.getMaxIndexes(_bangobj.points);
        for (int ii = 0; ii < windexes.length; ii++) {
            winners[windexes[ii]] = true;
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
        _bangobj.board.computeMoves(piece, _moves, null);
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
    protected boolean addBonus ()
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

        // have a 1 in 4 chance of adding a bonus for each live player for
        // which there is not already a bonus on the board
        int bprob = (_bangobj.gstats.livePlayers - _bangobj.gstats.bonuses);
        int rando = RandomUtil.getInt(40);
        if (bprob == 0 || rando > bprob*10) {
//             log.info("No bonus, probability " + bprob + " in 10 (" +
//                      rando + ").");
            return false;
        }

        // determine (roughly) who can get to bonus spots on this tick
        int[] weights = new int[_bonusSpots.size()];
        ArrayIntSet[] reachers = new ArrayIntSet[weights.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.owner < 0 || !p.isAlive() ||
                p.ticksUntilMovable(_bangobj.tick) > 0) {
                continue;
            }
            for (int bb = 0; bb < reachers.length; bb++) {
                int x = _bonusSpots.getX(bb), y = _bonusSpots.getY(bb);
                if (p.getDistance(x, y) > p.getMoveDistance()) {
                    continue;
                }
                _moves.clear();
                _bangobj.board.computeMoves(p, _moves, null);
                if (!_moves.contains(x, y)) {
                    continue;
                }
                log.info(p.info() + " can reach " + x + "/" + y);
                if (reachers[bb] == null) {
                    reachers[bb] = new ArrayIntSet();
                    reachers[bb].add(p.owner);
                }
            }
        }

        // now convert reachability into weightings for each of the spots
        for (int ii = 0; ii < weights.length; ii++) {
            if (reachers[ii] == null) {
                log.info("Spot " + ii + " is a wash.");
                // if no one can reach it, give it a base probability
                weights[ii] = 1;

            } else if (reachers[ii].size() == 1) {
                log.info("Spot " + ii + " is a one man spot.");
                // if only one player can reach it, give it a probability
                // inversely proportional to that player's power
                int pidx = reachers[ii].get(0);
                double ifactor = 1.0 - _bangobj.pstats[pidx].powerFactor;
                weights[ii] = (int)Math.round(10 * Math.max(0, ifactor)) + 1;

            } else {
                // if multiple players can reach it, give it a nudge if
                // they are of about equal power
                double avgpow = _bangobj.getAveragePower(reachers[ii]);
                boolean outlier = false;
                for (int pp = 0; pp < reachers[ii].size(); pp++) {
                    int pidx = reachers[ii].get(pp);
                    double power = _bangobj.pstats[pidx].power;
                    if (power < 0.9 * avgpow || power > 1.1 * avgpow) {
                        outlier = true;
                    }
                }
                log.info("Spot " + ii + " is a multi-man spot: " + outlier);
                weights[ii] = outlier ? 1 : 5;
            }
        }

        // now select a spot based on our weightings
        int spidx = RandomUtil.getWeightedIndex(weights);
        Point spot = new Point(_bonusSpots.getX(spidx),
                               _bonusSpots.getY(spidx));
        log.info("Selecting from " + StringUtil.toString(weights) + ": " +
                 spidx + " -> " + spot.x + "/" + spot.y + ".");

        // locate the nearest spot to that which can be occupied by our piece
        Point bspot = _bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            log.info("Dropping bonus for lack of spot " + spot + ".");
            return false;
        }

        // now turn to the bonus factory for guidance
        Piece bonus = Bonus.selectBonus(_bangobj, bspot, reachers[spidx]);
        bonus.assignPieceId();
        bonus.position(bspot.x, bspot.y);
        _bangobj.addToPieces(bonus);
        _bangobj.board.updateShadow(null, bonus);

//         String msg = MessageBundle.tcompose(
//             "m.placed_bonus", "" + bspot.x, "" + bspot.y);
//         SpeakProvider.sendInfo(_bangobj, BangCodes.BANG_MSGS, msg);

        log.info("Placed bonus: " + bonus.info());
        return true;
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

    /**
     * Creates the bang board based on the game config, filling in the
     * supplied pieces array with the starting pieces.
     */
    protected BangBoard createBoard (ArrayList<Piece> pieces)
    {
        Tuple tup = null;

        // try loading up the player specified board
        try {
            byte[] bdata = _bconfig.boardData;
            if (bdata != null && bdata.length > 0) {
                tup = BoardUtil.loadBoard(bdata);
            }
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to unserialize board.", ioe);
        }

        // if that failed, load a stock board
        if (tup == null) {
            String board = (String)RandomUtil.pickRandom(BOARDS);
            if (isTest()) {
                board = "default";
            }
            try {
                ClassLoader cl = getClass().getClassLoader();
                InputStream in = cl.getResourceAsStream(
                    "rsrc/boards/" + board + ".board");
                if (in != null) {
                    tup = BoardUtil.loadBoard(IOUtils.toByteArray(in));
                } else {
                    log.warning("Unable to load default board! " +
                                "[cl=" + cl + "].");
                }
            } catch (IOException ioe) {
                log.log(Level.WARNING, "Failed to load default board.", ioe);
            }
        }

        BangBoard board = null;
        if (tup != null) {
            board = (BangBoard)tup.left;
            Piece[] pvec = (Piece[])tup.right;
            int maxPieceId = 0;
            for (int ii = 0; ii < pvec.length; ii++) {
                if (pvec[ii].pieceId > maxPieceId) {
                    maxPieceId = pvec[ii].pieceId;
                }
            }
            Collections.addAll(pieces, pvec);
            Piece.setNextPieceId(maxPieceId);
        } else {
            board = new BangBoard(5, 5);
        }

        // now add the player pieces
        SkirmishScenario scen = new SkirmishScenario(_purchases);
        scen.generate(_bconfig, board, pieces);
        return board;
    }

    /** Used to accelerate things when testing. */
    protected long getBaseTick ()
    {
        return isTest() ? 500L : 2000L;
    }

    /** Indicates that we're testing and to do wacky stuff. */
    protected static boolean isTest ()
    {
        return (System.getProperty("test") != null);
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
                _ticker.schedule(getBaseTick() * avgPer);
            }
        }
    };

    /** A casted reference to our game configuration. */
    protected BangConfig _bconfig;

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** The purchases made by players in the buying phase. */
    protected PieceSet _purchases = new PieceSet();

    /** Used to indicate when all players are ready. */
    protected ArrayIntSet _ready = new ArrayIntSet();

    /** Used to calculate winners. */
    protected ArrayIntSet _havers = new ArrayIntSet();

    /** Used to track the order in which players are knocked out. */
    protected int[] _knockoutOrder;

    /** Used to record damage done during an attack. */
    protected IntIntMap _damage = new IntIntMap();

    /** Used to compute a piece's potential moves or attacks when
     * validating a move request. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    /** Used to track the locations of all bonus spawn points. */
    protected PointSet _bonusSpots = new PointSet();

    /** Used to track effects during a move. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();

    /** A list of our stock boards. */
    protected static final String[] BOARDS = {
        "alleys", "highnoon", "ring", "riverside", "square",
    };
}
