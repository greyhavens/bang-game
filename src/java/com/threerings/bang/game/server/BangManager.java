//
// $Id$

package com.threerings.bang.game.server;

import java.io.ByteArrayInputStream;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsServer;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.client.BangService;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangMarshaller;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.server.scenario.Scenario;
import com.threerings.bang.game.server.scenario.ScenarioFactory;
import com.threerings.bang.game.server.scenario.Tutorial;
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
        PlayerObject user = (PlayerObject)caller;
        int pidx = getPlayerIndex(user.getVisibleName());
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
        Card[] cards = null;
        if (_bangobj.roundId == 0 && cardIds != null) {
            cards = new Card[cardIds.length];
            for (int ii = 0; ii < cardIds.length; ii++) {
                CardItem item = (CardItem)user.inventory.get(cardIds[ii]);
                // no magicking up cards
                if (item.getQuantity() <= 0) {
                    continue;
                }
                // TODO: get pissy if they try to use the same card twice
                cards[ii] = item.getCard();
                cards[ii].init(_bangobj, pidx);
                _scards.put(cards[ii].cardId, new StartingCard(pidx, item));
            }
        }
        BigShotItem unit = (BigShotItem)user.inventory.get(bigShotId);
        selectStarters(pidx, unit, cards);
    }

    // documentation inherited from interface BangProvider
    public void selectTeam (ClientObject caller, String[] units)
    {
        PlayerObject user = (PlayerObject)caller;
        int pidx = getPlayerIndex(user.getVisibleName());
        if (pidx == -1) {
            log.warning("Request to purchase units by non-player " +
                        "[who=" + user.who() + "].");
            return;
        }
        selectTeam(pidx, units);
    }

    // documentation inherited from interface BangProvider
    public void move (ClientObject caller, int pieceId, short x, short y,
                      int targetId, BangService.InvocationListener il)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        int pidx = getPlayerIndex(user.getVisibleName());

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || !(piece instanceof Unit) || piece.owner != pidx) {
            log.warning("Rejecting illegal move request [who=" + user.who() +
                        ", piece=" + piece + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        Unit unit = (Unit)piece;
        int ticksTilMove = unit.ticksUntilMovable(_bangobj.tick);
        if (ticksTilMove > 0) {
            log.warning("Rejecting premature move/fire request " +
                        "[who=" + user.who() + ", piece=" + unit.info() +
                        ", ticksTilMove=" + ticksTilMove +
                        ", tick=" + _bangobj.tick +
                        ", lastActed=" + unit.lastActed + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        Piece target = (Piece)_bangobj.pieces.get(targetId);
        moveAndShoot(unit, x, y, target);
    }

    // documentation inherited from interface BangProvider
    public void playCard (ClientObject caller, int cardId, short x, short y)
    {
        PlayerObject user = (PlayerObject)caller;
        Card card = (Card)_bangobj.cards.get(cardId);
        if (card == null ||
            card.owner != getPlayerIndex(user.getVisibleName())) {
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

        // if this card was a starting card, note that it was consumed
        StartingCard scard = (StartingCard)_scards.get(cardId);
        if (scard != null) {
            scard.played = true;
        }

        // note that this player played a card
        _bangobj.stats[card.owner].incrementStat(Stat.Type.CARDS_PLAYED, 1);
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

    /**
     * Attempts to move the specified unit to the specified coordinates and
     * optionally fire upon the specified target.
     *
     * @param unit the unit to be moved.
     * @param x the x coordinate to which to move or {@link Short#MAX_VALUE} if
     * the unit should be moved to the closest valid firing position to the
     * target.
     * @param y the y coordinate to which to move, this is ignored if {@link
     * Short#MAX_VALUE} is supplied for x.
     * @param target the (optional) target to shoot after moving.
     */
    public void moveAndShoot (Unit unit, int x, int y, Piece target)
        throws InvocationException
    {
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
//                 log.info("Shooting " + target.info() +
//                          " with " + shooter.info());
                ShotEffect effect = shooter.shoot(_bangobj, target);
                // the initial shot updates the shooter's last acted
                effect.shooterLastActed = _bangobj.tick;
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
            }

            // finally update our metrics
            _bangobj.updateData();

        } finally {
            _bangobj.commitTransaction();
        }
    }

    /**
     * Prepares an effect and posts it to the game object, recording damage
     * done in the process.
     */
    public void deployEffect (int effector, Effect effect)
    {
        // prepare the effect and record any associated damage
        effect.prepare(_bangobj, _damage);
        if (effector != -1) {
            recordDamage(effector, _damage);
        }

        // broadcast the effect to the client
        _bangobj.setEffect(effect);

        // on the server we apply the effect immediately
        effect.apply(_bangobj, _effector);
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BangObject.class;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // set up the bang object
        _bangobj = (BangObject)_gameobj;
        _bangobj.setService(
            (BangMarshaller)PresentsServer.invmgr.registerDispatcher(
                new BangDispatcher(this), false));
        _bconfig = (BangConfig)_gameconfig;

        // for testing boards may be specified by name or binary board data
        if (!StringUtil.isBlank(_bconfig.board)) {
            BoardRecord brec = BangServer.boardmgr.getBoard(
                _bconfig.players.length, _bconfig.board);
            if (brec != null) {
                _boards = new BoardRecord[_bconfig.scenarios.length];
                Arrays.fill(_boards, brec);
            } else {
                String msg = MessageBundle.tcompose(
                    "m.no_such_board", _bconfig.board);
                SpeakProvider.sendAttention(_bangobj, GAME_MSGS, msg);
            }

        } else if (_bconfig.bdata != null) {
            try {
                BoardRecord brec = new BoardRecord();
                brec.load(new ByteArrayInputStream(_bconfig.bdata));
                _boards = new BoardRecord[_bconfig.scenarios.length];
                Arrays.fill(_boards, brec);
            } catch (Exception e) {
                String msg = MessageBundle.tcompose(
                    "m.board_load_failed", e.getMessage());
                SpeakProvider.sendAttention(_bangobj, GAME_MSGS, msg);
                log.log(Level.WARNING, "Failed to load board from data.", e);
            }
        }

        // if no boards were specified otherwise, pick them randomly
        if (_boards == null) {
            _boards = BangServer.boardmgr.selectBoards(
                _bconfig.players.length, _bconfig.scenarios);
        }

        // configure the town associated with this server
        _bangobj.setTownId(ServerConfig.getTownId());

        // create our per-player arrays
        int slots = getPlayerSlots();
        _bangobj.points = new int[slots];
        _bangobj.perRoundEarnings = new int[_bconfig.scenarios.length][slots];
        for (int ii = 0; ii < _bangobj.perRoundEarnings.length; ii++) {
            Arrays.fill(_bangobj.perRoundEarnings[ii], -1);
        }
        _bangobj.pdata = new BangObject.PlayerData[slots];
        _bangobj.stats = new StatSet[slots];
        for (int ii = 0; ii < slots; ii++) {
            _bangobj.pdata[ii] = new BangObject.PlayerData();
            _bangobj.stats[ii] = new StatSet();
        }
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();
        PresentsServer.invmgr.clearDispatcher(_bangobj.service);
    }

    @Override // documentation inherited
    protected void playersAllHere ()
    {
        switch (_bangobj.state) {
        case BangObject.PRE_GAME:
            // create our player records now that we know everyone's in the
            // room and ready to go
            _precords = new PlayerRecord[getPlayerSlots()];
            for (int ii = 0; ii < _precords.length; ii++) {
                PlayerRecord prec = (_precords[ii] = new PlayerRecord());
                if (isAI(ii)) {
                    prec.playerId = -1;
                    prec.ratings = new DSet();
                } else {
                    PlayerObject user = (PlayerObject)getPlayer(ii);
                    prec.playerId = user.playerId;
                    prec.purse = user.getPurse();
                    prec.ratings = user.ratings;
                }
            }
            // when the players all arrive, go into the buying phase
            startRound();
            break;

        case BangObject.IN_PLAY:
            // queue up the first board tick
            _ticker.schedule(getTickTime(), false);
            _bangobj.tick = (short)0;
            break;

        default:
            log.warning("playersAllHere() called during invalid phase! " +
                        "[where=" + where() +
                        ", state=" + _bangobj.state + "].");
            break;
        }
    }

    @Override // documentation inherited
    protected void stateDidChange (int state, int oldState)
    {
        super.stateDidChange(state, oldState);

        // we do some custom additional stuff
        switch (state) {
        case BangObject.SELECT_PHASE:
            // select big shots for our AIs
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                if (isAI(ii)) {
                    selectStarters(ii, null, null);
                }
            }
            break;

        case BangObject.BUYING_PHASE:
            // make purchases for our AIs
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                if (isAI(ii)) {
                    String[] units = new String[] {
                        "steamgunman", "gunslinger", "dirigible" };
                    selectTeam(ii, units);
                }
            }
            break;
        }
    }

    /** Starts the pre-game buying phase. */
    protected void startRound ()
    {
        // create the appropriate scenario to handle this round
        if (_bconfig.tutorial) {
            _bangobj.setScenarioId(ScenarioCodes.TUTORIAL);
            _scenario = new Tutorial();
        } else {
            _bangobj.setScenarioId(_bconfig.scenarios[_bangobj.roundId]);
            _scenario = ScenarioFactory.createScenario(_bangobj.scenarioId);
        }
        _scenario.init(this);

        // clear out the various per-player data structures
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
        _bangobj.maxPieceId = maxPieceId;

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

        // remove any remaining marker pieces and assign piece ids
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (p instanceof Marker) {
                iter.remove();
            }
            p.assignPieceId(_bangobj);
        }

        // configure the game object and board with the pieces
        _bangobj.setPieces(new PieceDSet(pieces.iterator()));
        _bangobj.board.shadowPieces(pieces.iterator());

        // clear out the selected big shots array
        _bangobj.setBigShots(new Unit[getPlayerSlots()]);

        // transition to the pre-game selection phase
        _scenario.startNextPhase(_bangobj);
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
                    if (cards[ii] != null) {
                        _bangobj.addToCards(cards[ii]);
                    }
                }
            }

            // if they failed to select a big shot (or are an AI) give
            // them a default
            if (item == null) {
                item = new BigShotItem(-1, "tactician");
            }

            // configure their big shot selection
            Unit unit = Unit.getUnit(item.getType());
            unit.assignPieceId(_bangobj);
            unit.init();
            unit.owner = pidx;
            _bangobj.setBigShotsAt(unit, pidx);

        } finally {
            _bangobj.commitTransaction();
        }

        // if everyone has selected their starters, move to the next phase
        checkStartNextPhase();
    }

    /**
     * Configures the specified player's purchases for this round and
     * starts the game if they are the last to configure.
     */
    protected void selectTeam (int pidx, String[] types)
    {
        // make sure they haven't already purchased units
        for (Piece piece : _purchases.values()) {
            if (piece.owner == pidx) {
                log.warning("Rejecting repeat purchase request " +
                            "[who=" + _bangobj.players[pidx] + "].");
                return;
            }
        }

        // make sure they didn't request too many pieces
        if (types.length > _bconfig.teamSize) {
            log.warning("Rejecting bogus team request " +
                        "[who=" + _bangobj.players[pidx] +
                        ", types=" + StringUtil.toString(types) +
                        ", teamSize=" + _bconfig.teamSize + "].");
            return;
        }

        // create an array of units from the requested types
        Unit[] units = new Unit[types.length];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = Unit.getUnit(types[ii]);
            if (units[ii].getCost() < 0) {
                log.warning("Player requested to purchase illegal unit " +
                            "[who=" + _bangobj.players[pidx] +
                            ", unit=" + types[ii] + "].");
                return;
            }
        }

        // TODO: make sure they didn't request units to which they don't have
        // access

        // TODO: make sure they didn't request more than their allowed number
        // of each unit (currently one)

        // initialize and prepare the units
        for (int ii = 0; ii < units.length; ii++) {
            units[ii].assignPieceId(_bangobj);
            units[ii].init();
            units[ii].owner = pidx;
            _purchases.add(units[ii]);
        }

        // note that this player is ready and potentially fire away
        _ready.add(pidx);
        checkStartNextPhase();
    }

    /**
     * This is called when a player takes an action that might result in the
     * current phase ending an the next phase starting, or when a player is
     * removed from the game (in which case the next phase might need to be
     * started because we were waiting on that player).
     */
    protected void checkStartNextPhase ()
    {
        switch (_bangobj.state) {
        case BangObject.SELECT_PHASE:
            for (int ii = 0; ii < _bangobj.bigShots.length; ii++) {
                if (_bangobj.isActivePlayer(ii) &&
                    _bangobj.bigShots[ii] == null) {
                    return;
                }
            }
            _scenario.startNextPhase(_bangobj);
            break;

        case BangObject.BUYING_PHASE:
            if (_ready.size() == _bangobj.getActivePlayerCount()) {
                _scenario.startNextPhase(_bangobj);
            }
            break;

        default:
            // nothing to do in this phase
            break;
        }
    }

    @Override // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // note the time at which we started
        _startStamp = System.currentTimeMillis();

        // add the selected big shots to the purchases
        for (int ii = 0; ii < _bangobj.bigShots.length; ii++) {
            if (_bangobj.isActivePlayer(ii) && _bangobj.bigShots[ii] != null) {
                _purchases.add(_bangobj.bigShots[ii]);
            }
        }

        // now place and add the player pieces
        try {
            _bangobj.startTransaction();

            try {
                // let the scenario know that we're about to start the round
                _scenario.roundWillStart(
                    _bangobj, _starts, _bonusSpots, _purchases);

                // configure the duration of the round
                _bangobj.setDuration(_scenario.getDuration(_bconfig));
                _bangobj.setLastTick((short)(_bangobj.duration - 1));

            } catch (InvocationException ie) {
                log.warning("Scenario initialization failed [game=" + where() +
                            ", error=" + ie.getMessage() + "].");
                SpeakProvider.sendAttention(
                    _bangobj, GAME_MSGS, ie.getMessage());
                // TODO: cancel the round (or let the scenario cancel it
                // on the first tick?)
            }

            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                // skip players that have abandoned ship
                if (!_bangobj.isActivePlayer(ii)) {
                    continue;
                }

                // note that this player is participating in this round by
                // changing their perRoundEarnings from -1 to zero
                _bangobj.perRoundEarnings[_bangobj.roundId-1][ii] = 0;

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
                    _bangobj.board.shadowPiece(piece);
                }
            }

        } finally {
            _bangobj.commitTransaction();
        }

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }

        // we reuse the playerIsReady() mechanism to wait for the players to
        // all report that they're fully ready to go (they need to resolve
        // their unit models)
        Arrays.fill(_playerOids, 0);
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
                    _bangobj.board.clearShadow(p);
                }
                continue;
            }

            int ox = p.x, oy = p.y;
            Effect teffect = p.tick(tick, _bangobj.board, pieces);
            if (teffect != null) {
                deployEffect(p.owner, teffect);
            }
        }

        // tick the scenario which will do all the standard processing
        _scenario.tick(_bangobj, tick);

        // determine whether we should end the game
        if (tick >= _bangobj.lastTick) {
            // let the scenario do any end of round business
            _scenario.roundDidEnd(_bangobj);

            // broadcast our updated statistics
            _bangobj.setStats(_bangobj.stats);

            // note that all active players completed this round
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                if (_bangobj.isActivePlayer(ii)) {
                    _precords[ii].finishedRound = _bangobj.roundId;
                }
            }

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

        // move our AI pieces randomly
        if (!_bconfig.tutorial) {
            for (int ii = 0; ii < pieces.length; ii++) {
                if (pieces[ii] instanceof Unit && pieces[ii].isAlive() &&
                    isAI(pieces[ii].owner) &&
                    pieces[ii].ticksUntilMovable(tick) == 0) {
                    Unit unit = (Unit)pieces[ii];
                    _moves.clear();
                    _attacks.clear();
                    _bangobj.board.computeMoves(unit, _moves, _attacks);

                    // if we can attack someone, do that
                    Piece target = null;
                    for (int tt = 0; tt < pieces.length; tt++) {
                        Piece p = pieces[tt];
                        if (p instanceof Unit && _attacks.contains(p.x, p.y) &&
                            unit.validTarget(p, false)) {
                            target = p;
                            break;
                        }
                    }
                    if (target != null) {
                        try {
                            moveAndShoot(unit, Short.MAX_VALUE, 0, target);
                            continue;
                        } catch (InvocationException ie) {
                            // fall through and move
                        }
                    }

                    // otherwise just move
                    if (_moves.size() > 0) {
                        int midx = RandomUtil.getInt(_moves.size());
                        moveUnit(unit, _moves.getX(midx), _moves.getY(midx),
                                 null);
                    }
                }
            }
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

        // if the game was cancelled, don't do any of this
        if (_bangobj.state == BangObject.CANCELLED) {
            return;
        }

        // process any played cards
        ArrayList<StartingCard> updates = new ArrayList<StartingCard>();
        ArrayList<StartingCard> removals = new ArrayList<StartingCard>();
        for (Iterator iter = _scards.values().iterator(); iter.hasNext(); ) {
            StartingCard scard = (StartingCard)iter.next();
            if (!scard.played) {
                continue;
            }
            if (scard.item.playCard()) {
                removals.add(scard);
            } else {
                updates.add(scard);
            }
        }
        if (updates.size() > 0 || removals.size() > 0) {
            notePlayedCards(updates, removals);
        }

        // note the duration of the game (in minutes)
        int gameTime = (int)(System.currentTimeMillis() - _startStamp) / 60000;

        // update ratings if appropriate
        if (_bconfig.rated &&
            !_bconfig.scenarios[0].equals(ScenarioCodes.TUTORIAL)) {
            // update each player's per-scenario ratings
            for (int ii = 0; ii < _bconfig.scenarios.length; ii++) {
                String scenario = _bconfig.scenarios[ii];
                computeRatings(scenario, _bangobj.perRoundEarnings[ii]);
            }

            // update each player's overall rating
            computeRatings(ScenarioCodes.OVERALL, _bangobj.getFilteredPoints());
        }

        // these will track awarded cash and badges
        Award[] awards = new Award[getPlayerSlots()];

        // record various statistics
        for (int ii = 0; ii < awards.length; ii++) {
            Award award = (awards[ii] = new Award());
            award.pidx = ii;
            for (int rr = 0; rr < _ranks.length; rr++) {
                if (_ranks[rr].pidx == ii) {
                    award.rank = rr;
                    break;
                }
            }

            // compute this player's "take home" cash (if they're not an AI)
            PlayerRecord prec = _precords[ii];
            if (prec.playerId > 0) {
                award.cashEarned = computeEarnings(ii);
            }

            // if this player left the game early, don't update their stats or
            // award them a new badge
            PlayerObject user = (PlayerObject)getPlayer(ii);
            if (user == null || !_bangobj.isActivePlayer(ii)) {
                continue;
            }
            prec.user = user;

            StatSet stats = _bangobj.stats[ii];
            try {
                // send all the stat updates out in one dobj event
                user.startTransaction();

                // if the game wasn't at least one minute long, certain
                // stats don't count
                if (gameTime > 0) {
                    user.stats.incrementStat(Stat.Type.GAMES_PLAYED, 1);
                    user.stats.incrementStat(Stat.Type.GAME_TIME, gameTime);
                    if (_bangobj.winners[ii]) {
                        user.stats.incrementStat(Stat.Type.GAMES_WON, 1);
                        user.stats.incrementStat(Stat.Type.CONSEC_WINS, 1);
                        user.stats.incrementStat(Stat.Type.CONSEC_LOSSES, 0);
                    } else {
                        user.stats.incrementStat(Stat.Type.CONSEC_LOSSES, 1);
                        user.stats.incrementStat(Stat.Type.CONSEC_WINS, 0);
                    }
                }

                // these stats count regardless of the game duration
                for (int ss = 0; ss < ACCUM_STATS.length; ss++) {
                    Stat.Type type = ACCUM_STATS[ss];
                    user.stats.incrementStat(type, stats.getIntStat(type));
                }
                user.stats.maxStat(Stat.Type.HIGHEST_POINTS,
                                   stats.getIntStat(Stat.Type.POINTS_EARNED));
                user.stats.maxStat(Stat.Type.MOST_KILLS,
                                   stats.getIntStat(Stat.Type.UNITS_KILLED));
                user.stats.incrementStat(
                    Stat.Type.CASH_EARNED, award.cashEarned);

                // allow the scenario to record statistics as well
                _scenario.recordStats(_bangobj, gameTime, ii, user);

                // determine whether this player qualifies for a new badge
                award.badge = Badge.checkQualifies(user);

            } finally {
                user.commitTransaction();
            }
        }

        // broadcast the per-round earnings which will be displayed on one
        // stats panel
        _bangobj.setPerRoundEarnings(_bangobj.perRoundEarnings);

        // sort by rank and then stuff the award data into the game object
        Arrays.sort(awards);
        _bangobj.setAwards(awards);

        // and persist the awards as well
        postGamePersist(awards);
    }

    @Override // documentation inherited
    protected void playerGameDidEnd (int pidx)
    {
        super.playerGameDidEnd(pidx);

        // we may have been waiting on this player
        checkStartNextPhase();
    }

    @Override // documentation inherited
    protected void assignWinners (boolean[] winners)
    {
        // compute the final ranking of each player, resolving ties using kill
        // count, then a random ordering
        int[] points = _bangobj.getFilteredPoints();
        _ranks = new RankRecord[points.length];
        for (int ii = 0; ii < _ranks.length; ii++) {
            int kills = _bangobj.stats[ii].getIntStat(Stat.Type.UNITS_KILLED);
            _ranks[ii] = new RankRecord(ii, points[ii], kills);
        }

        // first shuffle, then sort so that ties are resolved randomly
        ArrayUtil.shuffle(_ranks);
        Arrays.sort(_ranks);

        // now ensure that each player has at least one more point than the
        // player ranked immediately below them to communicate any last ditch
        // tie resolution to the players
        for (int ii = _ranks.length-2; ii >= 0; ii--) {
            int highidx = _ranks[ii].pidx, lowidx = _ranks[ii+1].pidx;
            if (_bangobj.points[highidx] <= _bangobj.points[lowidx]) {
                _bangobj.setPointsAt(_bangobj.points[lowidx]+1, highidx);
            }
        }

        // finally pass the winner info up to the parlor services
        winners[_ranks[0].pidx] = true;
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
            if (target == null) {
                // the target must no longer be around, so abandon ship
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

        // ensure that we don't land on any piece that prevents us from
        // overlapping
        ArrayList<Piece> lappers = _bangobj.getOverlappers(munit);
        if (lappers != null) {
            for (Piece lapper : lappers) {
                if (lapper.preventsOverlap(munit)) {
                    return null;
                }
            }
        }

        // update our board shadow
        _bangobj.board.clearShadow(unit);
        _bangobj.board.shadowPiece(munit);

        // record the move to this player's statistics
        _bangobj.stats[munit.owner].incrementStat(
            Stat.Type.DISTANCE_MOVED, steps);

        // dispatch a move effect to actually move the unit
        MoveEffect meffect = new MoveEffect();
        meffect.init(munit);
        meffect.nx = munit.x;
        meffect.ny = munit.y;
        deployEffect(munit.owner, meffect);

        // interact with any pieces occupying our target space
        if (lappers != null) {
            for (Piece lapper : lappers) {
                Effect effect = munit.maybeInteract(lapper);
                if (effect != null) {
                    deployEffect(unit.owner, effect);

                    // small hackery: note that this player collected a bonus
                    if (lapper instanceof Bonus) {
                        _bangobj.stats[munit.owner].incrementStat(
                            Stat.Type.BONUSES_COLLECTED, 1);
                    }
                }
            }
        }

        return munit;
    }

    /**
     * Called following each tick to determine whether or not new bonuses
     * should be added to the board.
     *
     * @return true if a bonus was added, false if not.
     */
    protected boolean addBonus ()
    {
        // no automatic bonuses in tutorials
        if (_bconfig.tutorial) {
            return false;
        }

        Piece[] pieces = _bangobj.getPieceArray();

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
//                 log.info("Spot " + ii + " is a wash.");
                // if no one can reach it, give it a base probability
                weights[ii] = 1;

            } else if (reachers[ii].size() == 1) {
//                 log.info("Spot " + ii + " is a one man spot.");
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
//                 log.info("Spot " + ii + " is a multi-man spot: " + outlier);
                weights[ii] = outlier ? 1 : 5;
            }
        }

        // zero out weightings for any spots that already have a bonus
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Bonus) {
                int spidx = ((Bonus)pieces[ii]).spot;
                if (spidx >= 0) {
                    weights[spidx] = 0;
                }
            }
        }
        // make sure there is at least one available spot
        if (IntListUtil.sum(weights) == 0) {
            log.info("Dropping bonus. No unused spots.");
            return false;
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
        Bonus bonus = Bonus.selectBonus(_bangobj, bspot, reachers[spidx]);
        if (bonus != null) {
            bonus.spot = (short)spidx;
            bonus.assignPieceId(_bangobj);
            bonus.position(bspot.x, bspot.y);
            _bangobj.addToPieces(bonus);
            _bangobj.board.shadowPiece(bonus);

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

        // award points for the damage dealt: 1 point for each 10 units
        total /= 10;
        _bangobj.grantPoints(pidx, total);

        // finally clear out the damage index
        damage.clear();
    }

    /**
     * Computes the take-home cash for the specified player index. This is
     * based on their final rank, their purse, the number of rounds played and
     * the number of players.
     */
    protected int computeEarnings (int pidx)
    {
        int earnings = 0;
        for (int rr = 0; rr < _bconfig.getRounds(); rr++) {
            // stop if we get to a round this player didn't finish
            if (_precords[pidx].finishedRound == rr) {
                break;
            }

            // total up the players they defeated in each round
            int defeated = 0;
            for (int ii = _ranks.length-1; ii >= 0; ii--) {
                if (_ranks[ii].pidx == pidx) {
                    break;
                }
                // require that the defeated opponent finished the round
                if (_precords[_ranks[ii].pidx].finishedRound > rr) {
                    defeated++;
                }
            }
            earnings += BASE_EARNINGS[defeated];
        }

        // TODO: scale based on some stat that indicates that they actually
        // played, like damage inflicted

        // and scale earnings based on their purse
        return Math.round(_precords[pidx].purse.getPurseBonus() * earnings);
    }

    /**
     * Computes updated ratings for the specified scenario, using the supplied
     * scores and stores them in the appropriate {@link PlayerRecord}.
     */
    protected void computeRatings (String scenario, int[] scores)
    {
        // collect each player's rating for this scenario
        Rating[] ratings = new Rating[getPlayerSlots()];
        for (int pidx = 0; pidx < ratings.length; pidx++) {
            ratings[pidx] = _precords[pidx].getRating(scenario);
        }

        // now compute the adjusted ratings
        int[] nratings = new int[ratings.length];
        for (int pidx = 0; pidx < ratings.length; pidx++) {
            nratings[pidx] = Rating.computeRating(scores, ratings, pidx);
        }

        // finally store the adjusted ratings back in the ratings objects and
        // record the increased experience
        for (int pidx = 0; pidx < ratings.length; pidx++) {
            // skip this rating if we weren't able to compute a value
            if (nratings[pidx] < 0) {
                continue;
            }
            ratings[pidx].rating = nratings[pidx];
            ratings[pidx].experience++;
            _precords[pidx].nratings.put(ratings[pidx].scenario, ratings[pidx]);
        }
    }

    /**
     * Persists the supplied cash and badges and sticks them into the
     * distributed objects of the appropriate players. Also updates the
     * players' ratings if appropriate.
     */
    protected void postGamePersist (final Award[] awards)
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                for (int pidx = 0; pidx < awards.length; pidx++) {
                    Award award = awards[pidx];
                    PlayerRecord prec = _precords[award.pidx];
                    if (prec.playerId < 0) {
                        continue; // skip AIs
                    }

                    // grant them their case
                    if (award.cashEarned > 0) {
                        try {
                            BangServer.playrepo.grantScrip(
                                prec.playerId, award.cashEarned);
                        } catch (PersistenceException pe) {
                            log.log(Level.WARNING, "Failed to award scrip to " +
                                    "player [who=" + prec.playerId +
                                    ", scrip=" + award.cashEarned + "]", pe);
                        }
                    }

                    // grant them their badge
                    if (award.badge != null) {
                        try {
                            BangServer.itemrepo.insertItem(award.badge);
                        } catch (PersistenceException pe) {
                            log.log(Level.WARNING, "Failed to store badge " +
                                    award.badge, pe);
                        }
                    }

                    // update their ratings
                    if (prec.nratings.size() > 0) {
                        ArrayList<Rating> ratings =
                            new ArrayList<Rating>(prec.nratings.values());
                        try {
                            BangServer.ratingrepo.updateRatings(
                                prec.playerId, ratings);
                        } catch (PersistenceException pe) {
                            log.log(Level.WARNING,
                                    "Failed to persist ratings " +
                                    "[pid=" + prec.playerId + ", ratings=" +
                                    StringUtil.toString(ratings) + "]", pe);
                        }
                    }
                }
                return true;
            }

            public void handleResult () {
                for (int pidx = 0; pidx < awards.length; pidx++) {
                    PlayerObject player = _precords[pidx].user;
                    if (player == null || !player.isActive()) {
                        // no need to update their player distributed object if
                        // they've already logged off
                        continue;
                    }
                    if (awards[pidx].cashEarned > 0) {
                        player.setScrip(player.scrip + awards[pidx].cashEarned);
                    }
                    if (awards[pidx].badge != null) {
                        player.addToInventory(awards[pidx].badge);
                    }
                    for (Rating rating : _precords[pidx].nratings.values()) {
                        if (player.ratings.containsKey(rating.scenario)) {
                            player.updateRatings(rating);
                        } else {
                            player.addToRatings(rating);
                        }
                    }
                }
            }
        });
    }

    /**
     * Flushes any updated card items to the database and effects any removals
     * due to the last card being played from a player's inventory.
     */
    protected void notePlayedCards (final ArrayList<StartingCard> updates,
                                    final ArrayList<StartingCard> removals)
    {
        log.info("Noting played cards [updates=" + updates.size() +
                 ", removals=" + removals.size() + "].");
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                for (StartingCard scard : updates) {
                    try {
                        BangServer.itemrepo.updateItem(scard.item);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to update played card " +
                                "[item=" + scard.item + "]", pe);
                    }
                }
                for (StartingCard scard : removals) {
                    try {
                        BangServer.itemrepo.deleteItem(
                            scard.item, "played_last_card");
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to delete played card " +
                                "[item=" + scard.item + "]", pe);
                    }
                }
                return true;
            }

            public void handleResult () {
                for (StartingCard scard : updates) {
                    PlayerObject user = (PlayerObject)getPlayer(scard.pidx);
                    if (user != null) {
                        user.updateInventory(scard.item);
                    }
                }
                for (StartingCard scard : removals) {
                    PlayerObject user = (PlayerObject)getPlayer(scard.pidx);
                    if (user != null) {
                        user.removeFromInventory(scard.item.getKey());
                    }
                }
            }
        });
    }

    /**
     * Returns the number of milliseconds until the next tick. This is scaled
     * based on the number of pieces in play.
     */
    protected long getTickTime ()
    {
        if (_bconfig.tutorial) {
            // hard code ticks at four seconds for tutorials
            return 4000L;

        } else {
            // start out with a base tick of two seconds and scale it down as
            // the game progresses; cap it at ten minutes
            long delta = System.currentTimeMillis() - _startStamp;
            delta = Math.min(delta, TIME_SCALE_CAP);

            // scale from 1/1 to 1/2 over the course of ten minutes
            float factor = 1f + 1f * delta / TIME_SCALE_CAP;
            long baseTime = (long)Math.round(BASE_TICK_TIME / factor);

            // scale this base time by the average number of units in play
            long tickTime = baseTime * _bangobj.getAverageUnitCount();

            // make sure the tick is at least one second long
            return Math.max(tickTime, 1000L);
        }
    }

    /** Indicates that we're testing and to do wacky stuff. */
    protected boolean isTest ()
    {
        // if one of the AIs has a special personality code, we're testing
        int aicount = (_AIs == null) ? 0 : _AIs.length;
        for (int ii = 0; ii < aicount; ii++) {
            if (_AIs[ii] != null && _AIs[ii].personality == 1) {
                return true;
            }
        }
        return false;
    }

    /** Used to track cards from a player's inventory and whether or not they
     * are actually used during a game. */
    protected static class StartingCard
    {
        public int pidx;
        public CardItem item;
        public boolean played;

        public StartingCard (int pidx, CardItem item) {
            this.pidx = pidx;
            this.item = item;
        }
    }

    /** Contains information on the players in the game which we need to ensure
     * is around even if the player logs off in the middle of the game. */
    protected static class PlayerRecord
    {
        public int playerId;
        public Purse purse;
        public int finishedRound;

        public DSet ratings;
        public HashMap<String,Rating> nratings = new HashMap<String,Rating>();

        public PlayerObject user;

        public Rating getRating (String scenario) {
            Rating rating = nratings.get(scenario);
            if (rating == null) {
                rating = (Rating)ratings.get(scenario);
                if (rating == null) {
                    rating = new Rating();
                    rating.scenario = scenario;
                } else if (rating.experience > 0) {
                    rating = (Rating)rating.clone();
                }
            }
            return rating;
        }
    }

    /** Used to rank the players at the end of the game. */
    protected static class RankRecord implements Comparable<RankRecord>
    {
        public int pidx, points, kills;

        public RankRecord (int pidx, int points, int kills) {
            this.pidx = pidx;
            this.points = points;
            this.kills = kills;
        }

        public int compareTo (RankRecord other) {
            int delta;
            if ((delta = (other.points - points)) != 0) {
                return delta;
            }
            if ((delta = (other.kills - kills)) != 0) {
                return delta;
            }
            return 0;
        }
    }

    /** Triggers our board tick once every N seconds. */
    protected Interval _ticker = _ticker = new Interval(PresentsServer.omgr) {
        public void expired () {
            // cope if the game has been ended and destroyed since we were
            // queued up for execution
            if (!_bangobj.isActive() || _bangobj.state != BangObject.IN_PLAY) {
                return;
            }

            // reset the extra tick time and update the game's tick counter
            int nextTick = (_bangobj.tick + 1) % Short.MAX_VALUE;
            _extraTickTime = 0L;
            _bangobj.setTick((short)nextTick);

            // queue up the next tick
            long tickTime = getTickTime() + _extraTickTime;
            _ticker.schedule(tickTime);
            _nextTickTime = System.currentTimeMillis() + tickTime;
        }
    };

    /** Handles post-processing when effects are applied. */
    protected Effect.Observer _effector = new Effect.Observer() {
        public void pieceAdded (Piece piece) {
        }

        public void pieceAffected (Piece piece, String effect) {
        }

        public void pieceMoved (Piece piece) {
            // let the scenario know that the unit moved
            Effect effect = _scenario.pieceMoved(_bangobj, piece);
            if (effect != null) {
                deployEffect(piece.owner, effect);
            }
        }

        public void pieceKilled (Piece piece) {
            piece.wasKilled(_bangobj.tick);
            Effect deatheff = _scenario.pieceWasKilled(_bangobj, piece);
            if (deatheff != null) {
                deployEffect(-1, deatheff);
            }
        }

        public void pieceRemoved (Piece piece) {
        }

        public void tickDelayed (long extraTime) {
            // if we are currently processing a tick, add to the extra tick
            // time; otherwise, postpone the next tick
            long now = System.currentTimeMillis();
            if (now >= _nextTickTime) {
                _extraTickTime = Math.max(_extraTickTime, extraTime);
            } else {
                _nextTickTime += extraTime;
                _ticker.schedule(_nextTickTime - now);
            }
        }
    };

    /** A casted reference to our game configuration. */
    protected BangConfig _bconfig;

    /** A casted reference to our game object. */
    protected BangObject _bangobj;

    /** Contains info on all of the players in the game. */
    protected PlayerRecord[] _precords;

    /** Used at the end of the game to rank the players. */
    protected RankRecord[] _ranks;

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

    /** Maps card id to a {@link StartingCard} record. */
    protected HashIntMap _scards = new HashIntMap();

    /** The time for which the next tick is scheduled. */
    protected long _nextTickTime;

    /** The extra time to take for the current tick to allow extended effects
     * to complete. */
    protected long _extraTickTime;

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
        Stat.Type.POINTS_EARNED,
        Stat.Type.SHOTS_FIRED,
        Stat.Type.DISTANCE_MOVED,
    };

    /** Defines the base earnings (per-round) for each rank. */
    protected static final int[] BASE_EARNINGS = { 75, 80, 90, 100 };
}
