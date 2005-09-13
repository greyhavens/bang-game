//
// $Id$

package com.threerings.bang.game.server;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsServer;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.client.BangService;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangMarshaller;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.game.server.scenario.CattleHerding;
import com.threerings.bang.game.server.scenario.Scenario;
import com.threerings.bang.game.server.scenario.ScenarioFactory;
import com.threerings.bang.game.server.scenario.Shootout;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side of the game.
 */
public class BangManager extends GameManager
    implements GameCodes, BangProvider
{
    // documentation inherited from interface BangProvider
    public void selectStarters (
        ClientObject caller, int bigShotId, int[] cardIds)
    {
        BangUserObject user = (BangUserObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);
        if (pidx == -1) {
            log.warning("Request to select starters by non-player " +
                        "[who=" + user.who() + "].");
            return;
        }

        // make sure we haven't already done this
        if (_bangobj.bigShots[pidx] != null) {
            log.info("Rejecting repeat starter selection " +
                     "[who=" + user.who() + "].");
            return;
        }

        // fetch the requisite items from their inventory
        BigShotItem unit = (BigShotItem)user.inventory.get(bigShotId);
        Card[] cards = null;
        if (_bangobj.roundId == 0 && cardIds != null) {
            cards = new Card[cardIds.length];
            for (int ii = 0; ii < cardIds.length; ii++) {
                // TODO: convert card items to cards
            }
        }
        selectStarters(pidx, unit, cards);
    }

    // documentation inherited from interface BangProvider
    public void purchaseUnits (ClientObject caller, String[] units)
    {
        BangUserObject user = (BangUserObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);
        if (pidx == -1) {
            log.warning("Request to purchase units by non-player " +
                        "[who=" + user.who() + "].");
            return;
        }
        purchaseUnits(pidx, units);
    }

    // documentation inherited from interface BangProvider
    public void move (ClientObject caller, int pieceId, short x, short y,
                      int targetId, BangService.InvocationListener il)
        throws InvocationException
    {
        BangUserObject user = (BangUserObject)caller;
        int pidx = _bangobj.getPlayerIndex(user.username);

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || !(piece instanceof Unit) || piece.owner != pidx) {
            log.warning("Rejecting illegal move request [who=" + user.who() +
                        ", piece=" + piece + "].");
            return;
        }
        Unit unit = (Unit)piece;
        int ticksTilMove = unit.ticksUntilMovable(_bangobj.tick);
        if (ticksTilMove > 0) {
            log.warning("Rejecting premature move/fire request " +
                        "[who=" + user.who() + ", piece=" + unit.info() +
                        ", ticksTilMove=" + ticksTilMove +
                        ", tick=" + _bangobj.tick +
                        ", lastActed=" + unit.lastActed + "].");
            return;
        }

        Piece target = (Piece)_bangobj.pieces.get(targetId);
        Piece munit = null, shooter = unit;
        try {
            _bangobj.startTransaction();

            // if they specified a non-NOOP move, execute it
            if (x != unit.x || y != unit.y) {
                munit = moveUnit(unit, x, y, target);
                if (munit == null) {
                    throw new InvocationException(MOVE_BLOCKED);
                }
                shooter = munit;
            }

            // if they specified a target, shoot at it
            if (target != null) {
                // make sure the target is valid
                if (!shooter.validTarget(target, false)) {
                    // target already dead or something
                    throw new InvocationException(TARGET_NO_LONGER_VALID);
                }

                // make sure the target is still within range
                if (!shooter.targetInRange(target.x, target.y)) {
                    throw new InvocationException(TARGET_MOVED);
                }

                // effect the initial shot
                ShotEffect effect = shooter.shoot(_bangobj, target);
                deployEffect(shooter.owner, effect);
                _bangobj.stats[shooter.owner].incrementStat(
                    Stat.Type.SHOTS_FIRED, 1);

                // effect any collateral damage
                Effect[] ceffects = shooter.collateralDamage(
                    _bangobj, target, effect.newDamage);
                int ccount = (ceffects == null) ? 0 : ceffects.length;
                for (int ii = 0; ii < ccount; ii++) {
                    deployEffect(shooter.owner, ceffects[ii]);
                }

                // allow the target to return fire
                effect = target.returnFire(_bangobj, shooter, effect.newDamage);
                if (effect != null) {
                    deployEffect(target.owner, effect);
                    _bangobj.stats[target.owner].incrementStat(
                        Stat.Type.SHOTS_FIRED, 1);
                }

                // if they did not move in this same action, we need to
                // set their last acted tick
                if (munit == null) {
                    unit.lastActed = _bangobj.tick;
                    _bangobj.updatePieces(unit);
                }
            }

            // finally update our metrics
            _bangobj.updateData();

        } finally {
            _bangobj.commitTransaction();
        }
    }

    // documentation inherited from interface BangProvider
    public void playCard (ClientObject caller, int cardId, short x, short y)
    {
        BangUserObject user = (BangUserObject)caller;
        Card card = (Card)_bangobj.cards.get(cardId);
        if (card == null ||
            card.owner != _bangobj.getPlayerIndex(user.username)) {
            log.warning("Rejecting invalid card request [who=" + user.who() +
                        ", sid=" + cardId + ", card=" + card + "].");
            return;
        }

        log.info("Playing card: " + card);

        // remove it from their list
        _bangobj.removeFromCards(cardId);

        // activate it
        Effect effect = card.activate(x, y);
        deployEffect(card.owner, effect);

        // note that this player played a card
        _bangobj.stats[card.owner].incrementStat(Stat.Type.CARDS_PLAYED, 1);
    }

    // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.TICK)) {
            tick(_bangobj.tick);

        } else if (name.equals(BangObject.EFFECT)) {
            ((Effect)event.getValue()).apply(_bangobj, _effector);

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

        // select the boards we'll use for each round
        _boards = BangServer.boardmgr.selectBoards(
            _bconfig.players.length, _bconfig.scenarios, isTest());

        // TODO: get the town info from somewhere
        _bangobj.setTownId(BangCodes.FRONTIER_TOWN);

        // create our per-player arrays
        int slots = getPlayerSlots();
        _bangobj.funds = new int[slots];
        _bangobj.pdata = new BangObject.PlayerData[slots];
        _bangobj.stats = new StatSet[slots];
        for (int ii = 0; ii < slots; ii++) {
            _bangobj.pdata[ii] = new BangObject.PlayerData();
            _bangobj.stats[ii] = new StatSet();
        }

        // start with some cash if we're testing
//         if (isTest()) {
            Arrays.fill(_bangobj.funds, 300);
//         }
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
        // create the appropriate scenario to handle this round
        _bangobj.setScenarioId(_bconfig.scenarios[_bangobj.roundId]);
        _scenario = ScenarioFactory.createScenario(_bangobj.scenarioId);

        // clear out the readiness status of each player
        _ready.clear();
        _purchases.clear();

        // set up the board and pieces so it's visible while purchasing
        BoardRecord brec = _boards[_bangobj.roundId];
        _bangobj.setBoardName(brec.name);
        _bangobj.setBoard(brec.getBoard());
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        Piece[] pvec = brec.getPieces();
        int maxPieceId = 0;
        for (int ii = 0; ii < pvec.length; ii++) {
            if (pvec[ii].pieceId > maxPieceId) {
                maxPieceId = pvec[ii].pieceId;
            }
        }
        Collections.addAll(pieces, pvec);
        Piece.setNextPieceId(maxPieceId);

        // extract and remove all player start markers
        _starts.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.START)) {
                _starts.add(p);
                iter.remove();
            }
        }
        // if we lack sufficient numbers, create some random ones
        for (int ii = _starts.size(); ii < getPlayerSlots(); ii++) {
            Marker p = new Marker(Marker.START);
            p.x = (short)RandomUtil.getInt(_bangobj.board.getWidth());
            p.y = (short)RandomUtil.getInt(_bangobj.board.getHeight());
            _starts.add(p);
        }
        Collections.shuffle(_starts);

        // extract the bonus spawn markers from the pieces array
        _bonusSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.BONUS)) {
                _bonusSpots.add(p.x, p.y);
                iter.remove();
            }
        }

        // give the scenario a shot at its own custom markers
        _scenario.filterMarkers(_bangobj, _starts, pieces);

        // now remove any remaining marker pieces
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (p instanceof Marker) {
                iter.remove();
            }
        }

        // configure the game object and board with the pieces
        _bangobj.setPieces(new PieceDSet(pieces.iterator()));
        _bangobj.board.shadowPieces(pieces.iterator());

        // clear out the selected big shots array
        _bangobj.setBigShots(new Unit[getPlayerSlots()]);

        // transition to the pre-game selection phase
        _bangobj.setState(BangObject.SELECT_PHASE);

        // configure purchases for our AIs
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            if (isAI(ii) || (isTest() && _bconfig.teamSize == 4)) {
                selectStarters(ii, null, null);
                String[] units = new String[] {
                    "artillery", "steamgunman", "gunslinger", "dirigible" };
                purchaseUnits(ii, units);
            }
        }
    }

    /**
     * Selects the starting configuration for this player.
     */
    protected void selectStarters (int pidx, BigShotItem item, Card[] cards)
    {
        try {
            _bangobj.startTransaction();

            // if they supplied cards, fill those in
            if (cards != null) {
                for (int ii = 0; ii < cards.length; ii++) {
                    cards[ii].init(_bangobj, pidx);
                    _bangobj.addToCards(cards[ii]);
                }
            }

            // if they failed to select a big shot (or are an AI) give
            // them a default
            if (item == null) {
                item = new BigShotItem(-1, "tactician");
            }

            // configure their big shot selection
            Unit unit = Unit.getUnit(item.getType());
            unit.init();
            unit.owner = pidx;
            _bangobj.setBigShotsAt(unit, pidx);
            log.info(getPlayerName(pidx) + " selected " + item + ".");

        } finally {
            _bangobj.commitTransaction();
        }

        // if everyone has selected their starters, move to the next phase
        for (int ii = 0; ii < _bangobj.bigShots.length; ii++) {
            if (_bangobj.bigShots[ii] == null) {
                return;
            }
        }
        _bangobj.setState(BangObject.BUYING_PHASE);
    }

    /**
     * Configures the specified player's purchases for this round and
     * starts the game if they are the last to configure.
     */
    protected void purchaseUnits (int pidx, String[] types)
    {
        // create an array of units from the requested types
        Unit[] units = new Unit[types.length];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = Unit.getUnit(types[ii]);
            if (units[ii].getCost() < 0) {
                log.warning("Player requested to purchase illegal unit " +
                            "[who=" + _bangobj.players[pidx] +
                            ", unit=" + types[ii] + "].");
                return; // nothing doing
            }
        }

        // make sure they haven't already purchased units
        for (Piece piece : _purchases.values()) {
            if (piece.owner == pidx) {
                log.warning("Rejecting repeat purchase request " +
                            "[who=" + _bangobj.players[pidx] + "].");
                return;
            }
        }

        // TODO: make sure they didn't request too many pieces

        // total up the cost
        int totalCost = 0;
        for (int ii = 0; ii < units.length; ii++) {
            totalCost += units[ii].getCost();
        }
        if (totalCost > _bangobj.funds[pidx]) {
            log.warning("Rejecting bogus purchase request " +
                        "[who=" + _bangobj.players[pidx] +
                        ", total=" + totalCost +
                        ", funds=" + _bangobj.funds[pidx] + "].");
            return;
        }

        // initialize and prepare the units
        for (int ii = 0; ii < units.length; ii++) {
            units[ii].init();
            units[ii].owner = pidx;
            _purchases.add(units[ii]);
        }

        // finally decrement their funds
        _bangobj.setFundsAt(_bangobj.funds[pidx] - totalCost, pidx);

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

        // note the time at which we started
        _startStamp = System.currentTimeMillis();

        // add the selected big shots to the purchases
        for (int ii = 0; ii < _bangobj.bigShots.length; ii++) {
            if (_bangobj.bigShots[ii] != null) {
                _purchases.add(_bangobj.bigShots[ii]);
            }
        }

        // now place and add the player pieces
        try {
            _bangobj.startTransaction();

            // let the scenario know that we're about to start
            try {
                _scenario.init(_bangobj, _starts, _bonusSpots, _purchases);
            } catch (InvocationException ie) {
                log.warning("Scenario initialization failed [game=" + where() +
                            ", error=" + ie.getMessage() + "].");
                SpeakProvider.sendAttention(
                    _bangobj, GAME_MSGS, ie.getMessage());
                // TODO: cancel the round (or let the scenario cancel it
                // on the first tick?)
            }

            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                // first filter out this player's pieces
                ArrayList<Piece> ppieces = new ArrayList<Piece>();
                for (Piece piece : _purchases.values()) {
                    if (piece.owner == ii) {
                        ppieces.add(piece);
                    }
                }

                // now position each of them
                Piece p = _starts.remove(0);
                ArrayList<Point> spots = _bangobj.board.getOccupiableSpots(
                    ppieces.size(), p.x, p.y, 4);
                while (spots.size() > 0 && ppieces.size() > 0) {
                    Point spot = spots.remove(0);
                    Piece piece = ppieces.remove(0);
                    piece.position(spot.x, spot.y);
                    _bangobj.addToPieces(piece);
                    _bangobj.board.updateShadow(null, piece);
                }
            }

        } finally {
            _bangobj.commitTransaction();
        }

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

        // queue up the board tick
        int avgPer = _bangobj.getAverageUnitCount();
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
            if (!p.isAlive()) {
                if (p.expireWreckage(tick)) {
                    log.info("Expiring wreckage " + p.pieceId +
                             " l:" + p.lastActed + " t:" + tick);
                    _bangobj.removeFromPieces(p.getKey());
                    _bangobj.board.updateShadow(p, null);
                }
                continue;
            }

            int ox = p.x, oy = p.y;
            if (p.tick(tick, _bangobj.board, pieces)) {
                Effect effect = null;

                // if the piece died, make a note and maybe remove it
                if (!p.isAlive()) {
                    p.wasKilled(tick);
                    _scenario.pieceWasKilled(_bangobj, p);
                    if (p.removeWhenDead()) {
                        _bangobj.removeFromPieces(p.getKey());
                        _bangobj.board.updateShadow(p, null);
                    }

                // if the piece moved, let the scenario know about it
                } else if (p.x != ox || p.y != oy) {
                    effect = _scenario.pieceMoved(_bangobj, p);
                }

                // if the piece didn't die, update it
                if (_bangobj.pieces.containsKey(p.getKey())) {
                    _bangobj.updatePieces(p);
                }

                // after the piece has been updated, we can safely apply
                // any effects to it
                if (effect != null) {
                    effect.init(p);
                    deployEffect(-1, effect);
                }
            }
        }

        // move our AI pieces randomly
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Unit && pieces[ii].isAlive() &&
                isAI(pieces[ii].owner) &&
                pieces[ii].ticksUntilMovable(tick) == 0) {
                Unit unit = (Unit)pieces[ii];
                _moves.clear();
                _bangobj.board.computeMoves(unit, _moves, null);
                if (_moves.size() > 0) {
                    int midx = RandomUtil.getInt(_moves.size());
                    moveUnit(unit, _moves.getX(midx), _moves.getY(midx), null);
                }
            }
        }

        // tick the scenario and determine whether we should end the game
        if (_scenario.tick(_bangobj, tick)) {
            // broadcast our updated statistics
            _bangobj.setStats(_bangobj.stats);

            // if this is the last round, end the game
            if (_bangobj.roundId == _bconfig.getRounds()) {
                endGame();
            } else {
                endRound();
            }

            // cancel the board tick
            _ticker.cancel();
            return;
        }

        try {
            _bangobj.startTransaction();
            // potentially create and add new bonuses
            if (addBonus()) {
                _bangobj.updateData();
            }
        } finally {
            _bangobj.commitTransaction();
        }
    }

    protected void endRound ()
    {
        // trigger the display of the post round bits
        _bangobj.setState(BangObject.POST_ROUND);

        // start the next round
        startRound();
    }

    @Override // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // note the duration of the game (in minutes)
        int gameTime = (int)(System.currentTimeMillis() - _startStamp) / 60000;

        // record various statistics
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            BangUserObject user = (BangUserObject)getPlayer(ii);
            if (user == null || !_gameobj.isActivePlayer(ii)) {
                continue;
            }
            StatSet stats = _bangobj.stats[ii];

            try {
                // send all the stat updates out in one dobj event
                user.startTransaction();

                // if the game wasn't at least one minute long, certain
                // stats don't count
                if (gameTime > 0) {
                    user.stats.incrementStat(Stat.Type.GAMES_PLAYED, 1);
                    user.stats.incrementStat(Stat.Type.GAME_TIME, gameTime);
                    if (_gameobj.winners[ii]) {
                        user.stats.incrementStat(Stat.Type.GAMES_WON, 1);
                    }
                }

                // these stats count regardless of the game duration
                for (int ss = 0; ss < ACCUM_STATS.length; ss++) {
                    Stat.Type type = ACCUM_STATS[ss];
                    user.stats.incrementStat(type, stats.getIntStat(type));
                }
                user.stats.maxStat(Stat.Type.HIGHEST_EARNINGS,
                                   stats.getIntStat(Stat.Type.CASH_EARNED));
                user.stats.maxStat(Stat.Type.MOST_KILLS,
                                   stats.getIntStat(Stat.Type.UNITS_KILLED));

                // TODO: award and report "take home" cash

            } finally {
                user.commitTransaction();
            }
        }

        for (int ii = 0; ii < _bangobj.stats.length; ii++) {
            log.info(getPlayerName(ii) + ": " + _bangobj.stats[ii]);
        }
    }

    @Override // documentation inherited
    protected void assignWinners (boolean[] winners)
    {
        int[] windexes = IntListUtil.getMaxIndexes(_bangobj.funds);
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
    protected Unit moveUnit (Unit unit, int x, int y, Piece target)
    {
        // compute the possible moves for this unit
        _moves.clear();
        _bangobj.board.computeMoves(unit, _moves, null);

        // if we have not specified an exact move, locate one now
        if (x == Short.MAX_VALUE) {
            // the target must no longer be around, so abandon ship
            if (target == null) {
                return null;
            }

            Point spot = unit.computeShotLocation(target, _moves);
            if (spot == null) {
                log.info("Unable to find place from which to shoot. " +
                         "[piece=" + unit.info() + ", target=" + target.info() +
                         ", moves=" + _moves + "].");
                return null;
            }
            x = spot.x;
            y = spot.y;

            // if we decided not to move, just pretend like we did the job
            if (x == unit.x && y == unit.y) {
                return unit;
            }
        }

        // make sure we are alive, and are ready to move
        int steps = Math.abs(unit.x-x) + Math.abs(unit.y-y);
        if (!unit.isAlive() || unit.ticksUntilMovable(_bangobj.tick) > 0) {
            log.warning("Unit requested illegal move [unit=" + unit +
                        ", alive=" + unit.isAlive() +
                        ", mticks=" + unit.ticksUntilMovable(_bangobj.tick) +
                        "].");
            return null;
        }

        // validate that the move is legal
        if (!_moves.contains(x, y)) {
            log.warning("Unit requested illegal move [unit=" + unit +
                        ", x=" + x + ", y=" + y + ", moves=" + _moves + "].");
            Piece[] pvec = _bangobj.getPieceArray();
            for (int ii = 0; ii < pvec.length; ii++) {
                System.err.println(pvec[ii]);
            }
            _bangobj.board.dumpOccupiability(_moves);

            // reshadow all the pieces to try to correct the error
            _bangobj.board.shadowPieces(_bangobj.pieces.iterator());
            log.warning("Reshadowed dump:");
            _bangobj.board.dumpOccupiability(_moves);

            // now try the whole process again
            _moves.clear();
            _bangobj.board.computeMoves(unit, _moves, null);
            if (!_moves.contains(x, y)) {
                log.warning("Move still illegal: ");
                _bangobj.board.dumpOccupiability(_moves);
                return null;
            }
        }

        // clone and move the unit
        Unit munit = (Unit)unit.clone();
        munit.position(x, y);
        munit.lastActed = _bangobj.tick;

        // if they specified a target, make sure it is still valid
        if (target != null) {
            if (!munit.validTarget(target, false)) {
                log.info("Target no longer valid [shooter=" + munit.info() +
                         ", target=" + target.info() + "].");
                return null;
//                 // target already dead or something
//                 throw new InvocationException(TARGET_NO_LONGER_VALID);
            }
            if (!munit.targetInRange(target.x, target.y)) {
                log.info("Target no longer in range [shooter=" + munit.info() +
                         ", target=" + target.info() + "].");
                return null;
//                 throw new InvocationException(TARGET_MOVED);
            }
        }

        // ensure that we don't land on a piece that prevents us from
        // overlapping it and make a note of any piece that we land on
        // that does not prevent overlap
        ArrayList<Piece> lappers = _bangobj.getOverlappers(munit);
        Piece lapper = null;
        if (lappers != null) {
            for (Piece p : lappers) {
                if (p.preventsOverlap(munit)) {
                    return null;
                } else if (lapper != null) {
                    log.warning("Multiple overlapping pieces [mover=" + munit +
                                ", lap1=" + lapper + ", lap2=" + p + "].");
                } else {
                    lapper = p;
                }
            }
        }

        // update our board shadow
        _bangobj.board.updateShadow(unit, munit);

        // record the move to this player's statistics
        _bangobj.stats[munit.owner].incrementStat(
            Stat.Type.DISTANCE_MOVED, steps);

        // interact with any piece occupying our target space
        if (lapper != null) {
            switch (munit.maybeInteract(lapper, _effects)) {
            case CONSUMED:
                _bangobj.removeFromPieces(lapper.getKey());
                // note that this player collected a bonus
                if (lapper instanceof Bonus) {
                    _bangobj.stats[munit.owner].incrementStat(
                        Stat.Type.BONUSES_COLLECTED, 1);
                }
                break;

            case ENTERED:
                // update the piece we entered as we likely modified it in
                // doing so
                _bangobj.updatePieces(lapper);
                // TODO: generate a special event indicating that the
                // unit entered so that we can animate it
                _bangobj.removeFromPieces(munit.getKey());
                // short-circuit the remaining move processing
                return munit;

            case INTERACTED:
                // update the piece we interacted with, we'll update
                // ourselves momentarily
                _bangobj.updatePieces(lapper);
                break;

            case NOTHING:
                break;
            }
        }

        // let the scenario know that the unit moved
        Effect meffect = _scenario.pieceMoved(_bangobj, munit);
        if (meffect != null) {
            meffect.init(munit);
            _effects.add(meffect);
        }

        // update the unit in the distributed set
        _bangobj.updatePieces(munit);

        // finally effect the effects
        for (Effect effect : _effects) {
            deployEffect(unit.owner, effect);
        }
        _effects.clear();

        return munit;
    }

    /**
     * Prepares an effect and posts it to the game object, recording
     * damage done in the process.
     */
    protected void deployEffect (int effector, Effect effect)
    {
        effect.prepare(_bangobj, _damage);
        _bangobj.setEffect(effect);
        if (effector != -1) {
            recordDamage(effector, _damage);
        }
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
        int bprob = (_bangobj.gdata.livePlayers - _bangobj.gdata.bonuses);
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
            if (!(p instanceof Unit) || p.owner < 0 || !p.isAlive() ||
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
                double ifactor = 1.0 - _bangobj.pdata[pidx].powerFactor;
                weights[ii] = (int)Math.round(10 * Math.max(0, ifactor)) + 1;

            } else {
                // if multiple players can reach it, give it a nudge if
                // they are of about equal power
                double avgpow = _bangobj.getAveragePower(reachers[ii]);
                boolean outlier = false;
                for (int pp = 0; pp < reachers[ii].size(); pp++) {
                    int pidx = reachers[ii].get(pp);
                    double power = _bangobj.pdata[pidx].power;
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
        if (bonus != null) {
            bonus.assignPieceId();
            bonus.position(bspot.x, bspot.y);
            _bangobj.addToPieces(bonus);
            _bangobj.board.updateShadow(null, bonus);

//         String msg = MessageBundle.tcompose(
//             "m.placed_bonus", "" + bspot.x, "" + bspot.y);
//         SpeakProvider.sendInfo(_bangobj, GAME_MSGS, msg);

            log.info("Placed bonus: " + bonus.info());
        }

        return true;
    }

    /** Records damage done by the specified user to various pieces. */
    protected void recordDamage (int pidx, IntIntMap damage)
    {
        int total = 0;
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            int ddone = damage.get(ii);
            if (ddone <= 0) {
                continue;
            }
            // deduct 150% if you shoot yourself
            if (ii == pidx) {
                ddone = -3 * ddone / 2;
            }
            total += ddone;
        }

        // record the damage dealt statistic
        _bangobj.stats[pidx].incrementStat(Stat.Type.DAMAGE_DEALT, total);

        // award cash for the damage dealt
        total /= 10; // you get $1 for each 10 points of damage
        _bangobj.grantCash(pidx, total);

        // finally clear out the damage index
        damage.clear();
    }

    /** Used to accelerate things when testing. */
    protected long getBaseTick ()
    {
        // start out with a base tick of two seconds and scale it down as
        // the game progresses; cap it at ten minutes
        long delta = System.currentTimeMillis() - _startStamp;
        delta = Math.min(delta, TIME_SCALE_CAP);
        // scale from 1/1 to 1/2 over the course of ten minutes
        float factor = 1f + 1f * delta / TIME_SCALE_CAP;
        long abase = (long)Math.round(BASE_TICK_TIME / factor);
        return isTest() ? 500L : abase;
    }

    /** Indicates that we're testing and to do wacky stuff. */
    protected boolean isTest ()
    {
        // if it's a two player game and one player is an AI, we're
        // testing
        return (_AIs != null && getPlayerSlots() == 2);
    }

    /** Triggers our board tick once every N seconds. */
    protected Interval _ticker = _ticker = new Interval(PresentsServer.omgr) {
        public void expired () {
            int nextTick = (_bangobj.tick + 1) % Short.MAX_VALUE;
            _bangobj.setTick((short)nextTick);
            if (_bangobj.isInPlay()) {
                // queue ourselves up to expire in a time proportional to
                // the average number of pieces per player
                int avgPer = _bangobj.getAverageUnitCount();
                _ticker.schedule(getBaseTick() * avgPer);
            }
        }
    };

    /** Handles post-processing when effects are applied. */
    protected Effect.Observer _effector = new Effect.Observer() {
        public void pieceAdded (Piece piece) {
        }

        public void pieceAffected (Piece piece, String effect) {
            if (!piece.isAlive()) {
                piece.wasKilled(_bangobj.tick);
                // if the scenario modifies the killed piece, broadcast
                // those modifications to the clients
                if (_scenario.pieceWasKilled(_bangobj, piece)) {
                    _bangobj.updatePieces(piece);
                }
            }
        }

        public void pieceRemoved (Piece piece) {
        }
    };

    /** A casted reference to our game configuration. */
    protected BangConfig _bconfig;

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Contains information on our selection of boards. */
    protected BoardRecord[] _boards;

    /** Implements our gameplay scenario. */
    protected Scenario _scenario;

    /** The time at which the round started. */
    protected long _startStamp;

    /** The purchases made by players in the buying phase. */
    protected PieceSet _purchases = new PieceSet();

    /** Used to indicate when all players are ready. */
    protected ArrayIntSet _ready = new ArrayIntSet();

    /** Used to record damage done during an attack. */
    protected IntIntMap _damage = new IntIntMap();

    /** Used to compute a piece's potential moves or attacks when
     * validating a move request. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    /** Used to track the locations of all bonus spawn points. */
    protected PointSet _bonusSpots = new PointSet();

    /** Used to track the locations where players can start. */
    protected ArrayList<Piece> _starts = new ArrayList<Piece>();

    /** Used to track effects during a move. */
    protected ArrayList<Effect> _effects = new ArrayList<Effect>();

    /** The item ids of all cards used by players in this game. These will
     * be destroyed if the game completes normally. */
    protected ArrayIntSet _usedCards = new ArrayIntSet();

    /** Our starting base tick time. */
    protected static final long BASE_TICK_TIME = 2000L;

    /** We stop reducing the tick time after ten minutes. */
    protected static final long TIME_SCALE_CAP = 10 * 60 * 1000L;

    /** Stats that we accumulate at the end of the game into the player's
     * persistent stats. */
    protected static final Stat.Type[] ACCUM_STATS = {
        Stat.Type.UNITS_KILLED,
        Stat.Type.UNITS_LOST,
        Stat.Type.BONUSES_COLLECTED,
        Stat.Type.CARDS_PLAYED,
        Stat.Type.CASH_EARNED
    };
}
