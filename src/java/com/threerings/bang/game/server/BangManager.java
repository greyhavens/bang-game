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
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;
import com.threerings.util.StreamablePoint;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsServer;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.admin.data.StatusObject;
import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangBoard;
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
import com.threerings.bang.game.server.ai.AILogic;
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
    public void getBoard (
        ClientObject caller, BangService.BoardListener listener)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        if (!_bangobj.occupants.contains(user.getOid())) {
            log.warning("Rejecting request for board by non-occupant [who=" +
                user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);

        } else if (_bangobj.board == null) {
            log.warning("Rejecting request for non-existent board [who=" +
                user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        // note that roundId is currently one less than the actual round id (we
        // haven't yet called startGame()) and is thus properly zero indexed
        BoardRecord brec = _rounds[_bangobj.roundId].board;
        listener.requestProcessed(brec.getBoard(), brec.getPieces());
    }

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
        selectTeam(pidx, units, user);
    }

    // documentation inherited from interface BangProvider
    public void order (ClientObject caller, int pieceId, short x, short y,
                       int targetId, BangService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        int pidx = getPlayerIndex(user.getVisibleName());

        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            // the unit probably died or was hijacked
            throw new InvocationException(MOVER_NO_LONGER_VALID);
        }
        if (!(piece instanceof Unit)) {
            log.warning("Rejecting illegal move request [who=" + user.who() +
                        ", piece=" + piece + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        Unit unit = (Unit)piece;
        int ticksTilMove = unit.ticksUntilMovable(_bangobj.tick);
        if (ticksTilMove > 0) {
            // make sure this new order is valid
            AdvanceOrder order = new AdvanceOrder(unit, x, y, targetId);
            String cause = order.checkValid();
            if (cause != null) {
                throw new InvocationException(cause);
            }

            // clear out any previous advance order for this unit
            clearOrders(unit.pieceId);

            // queue up our new advance order
            _orders.add(order);
            listener.requestProcessed(QUEUED_ORDER);

        } else {
            // execute the order immediately
            executeOrder(unit, x, y, targetId, true);
            listener.requestProcessed(EXECUTED_ORDER);
        }
    }

    // documentation inherited from interface BangProvider
    public void cancelOrder (ClientObject caller, int pieceId)
    {
        PlayerObject user = (PlayerObject)caller;
        int pidx = getPlayerIndex(user.getVisibleName());
        Piece piece = (Piece)_bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            // the unit probably died or was hijacked
            return;
        }

        // look for any advance order for this unit and clear it
        for (Iterator<AdvanceOrder> iter = _orders.iterator();
             iter.hasNext(); ) {
            AdvanceOrder order = iter.next();
            if (order.unit.pieceId == pieceId) {
                reportInvalidOrder(order, ORDER_CLEARED);
                iter.remove();
            }
        }
    }

    // documentation inherited from interface BangProvider
    public void playCard (ClientObject caller, int cardId, short x, short y,
                          BangService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        Card card = (Card)_bangobj.cards.get(cardId);
        if (card == null ||
            card.owner != getPlayerIndex(user.getVisibleName())) {
            log.warning("Rejecting invalid card request [who=" + user.who() +
                        ", sid=" + cardId + ", card=" + card + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        log.info("Playing card: " + card);

        // create and deploy the card's effect
        Effect effect = card.activate(x, y);
        if (!deployEffect(card.owner, effect)) {
            throw new InvocationException(CARD_UNPLAYABLE);
        }

        // remove it from their list
        _bangobj.removeFromCards(cardId);

        // if this card was a starting card, note that it was consumed
        StartingCard scard = (StartingCard)_scards.get(cardId);
        if (scard != null) {
            scard.played = true;
        }

        // note that this player played a card
        _bangobj.stats[card.owner].incrementStat(Stat.Type.CARDS_PLAYED, 1);

        // let them know it worked
        listener.requestProcessed();
    }

    // documentation inherited from interface BangProvider
    public void reportPerformance (ClientObject caller, String board,
                                   String driver, int[] perfhisto)
    {
        // log this!
        PlayerObject user = (PlayerObject)caller;
        BangServer.perfLog(
            "client_perf u:" + user.username + " b:" + board + " d:" + driver +
            " h:" + StringUtil.toString(perfhisto));
    }

    // documentation inherited
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.TICK)) {
            if (_bangobj.tick >= 0) {
                tick(_bangobj.tick);
            }
        } else {
            super.attributeChanged(event);
        }
    }

    @Override // documentation inherited
    public void playerReady (ClientObject caller)
    {
        // if all players are AIs, the human observer determines when to
        // proceed
        if (_bconfig.allPlayersAIs()) {
            playersAllHere(); 
        } else {
            super.playerReady(caller);
        }

        // if we're in play, we need to note that this player is ready to go
        if (_bangobj.state == BangObject.IN_PLAY) {
            PlayerObject user = (PlayerObject)caller;
            int pidx = _bangobj.getPlayerIndex(user.handle);
            if (pidx != -1) {
                _bangobj.setPlayerStatusAt(BangObject.PLAYER_IN_PLAY, pidx);
            }
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
     * @param targetId the (optional) target to shoot after moving.
     * @param recheckOrders whether or not to recheck other advance orders
     * after executing this order.
     */
    public void executeOrder (
        Unit unit, int x, int y, int targetId, boolean recheckOrders)
        throws InvocationException
    {
        Piece munit = null, shooter = unit;
        try {
            _bangobj.startTransaction();

            // make sure the target is still around before we do anything
            Piece target = (Piece)_bangobj.pieces.get(targetId);
            if (targetId > 0 && target == null) {
                throw new InvocationException(TARGET_NO_LONGER_VALID);
            }

            // TEMP: for debugging weird shot effect problems
            int dam1 = (target == null) ? 0 : target.damage;

            // if they specified a non-NOOP move, execute it
            if (x != unit.x || y != unit.y) {
                munit = moveUnit(unit, x, y, target);
                shooter = munit;
            } else {
                // check that our target is still valid and reachable
                checkTarget(shooter, target);
            }

            // TEMP: for debugging weird shot effect problems
            int dam2 = (target == null) ? 0 : target.damage;

            // if they specified a target, shoot at it (we've already checked
            // in moveUnit() or above that our target is still valid)
            if (target != null) {
                // effect the initial shot
                log.fine("Shooting " + target.info() +
                         " with " + shooter.info());
                ShotEffect effect = shooter.shoot(_bangobj, target);
                // the initial shot updates the shooter's last acted
                effect.shooterLastActed = _bangobj.tick;

                // apply the shot effect
                if (!deployEffect(shooter.owner, effect)) {
                    log.warning("Failed to deploy shot effect " +
                                "[shooter=" + shooter.info() +
                                ", move=" + x + "/" + y +
                                ", target=" + target.info() +
                                ", dam1=" + dam1 + ", dam2=" + dam2+ "].");
                } else {
                    _bangobj.stats[shooter.owner].incrementStat(
                        Stat.Type.SHOTS_FIRED, 1);
                }

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

        // finally, validate all of our advance orders and make sure none of
        // them have become invalid
        if (recheckOrders) {
            validateOrders();
        }
    }

    /**
     * Prepares an effect and posts it to the game object, recording damage
     * done in the process.
     *
     * @return true if the effect was deployed, false if the effect was either
     * not applicable or failed to apply.
     */
    public boolean deployEffect (int effector, Effect effect)
    {
        // prepare the effect
        effect.prepare(_bangobj, _damage);

        // make sure the effect is still applicable
        if (!effect.isApplicable()) {
            _damage.clear();
            return false;
        }

        // record our damage if appropriate
        if (effector != -1) {
            recordDamage(effector, _damage);
        }

        // broadcast the effect to the client
        _bangobj.setEffect(effect);

        // on the server we apply the effect immediately
        return effect.apply(_bangobj, _effector);
    }

    /**
     * Called by the {@link Scenario} to start the specified phase of the game.
     */
    public void startPhase (int state)
    {
        switch (state) {
        case BangObject.PRE_TUTORIAL:
            resetPreparingStatus(true);
            _bangobj.setState(BangObject.PRE_TUTORIAL);
            break;

        case BangObject.SELECT_PHASE:
            resetPreparingStatus(false);
            _bangobj.setState(BangObject.SELECT_PHASE);
            break;

        case BangObject.BUYING_PHASE:
            resetPreparingStatus(false);
            _bangobj.setState(BangObject.BUYING_PHASE);
            break;

        case BangObject.IN_PLAY:
            startGame();
            break;

        default:
            log.warning("Unable to start next phase [game=" + where() +
                        ", state=" + state + "].");
            break;
        }
    }

    @Override // documentation inherited
    public void updateOccupantInfo (OccupantInfo occInfo)
    {
        super.updateOccupantInfo(occInfo);

        // if an active player disconnected, boot them from the game
        int pidx = getPlayerIndex(occInfo.username);
        if (pidx != -1 && occInfo.status == OccupantInfo.DISCONNECTED &&
            _bangobj.isInPlay() && _bangobj.isActivePlayer(pidx)) {
            log.info("Booting disconnected player [game=" + where() +
                     ", who=" + occInfo.username + "].");
            endPlayerGame(pidx);
        }
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
        log.info("Manager started up [where=" + where() + "].");

        // set up the bang object
        _bangobj = (BangObject)_gameobj;
        _bangobj.setService(
            (BangMarshaller)PresentsServer.invmgr.registerDispatcher(
                new BangDispatcher(this), false));
        _bconfig = (BangConfig)_gameconfig;

        // note this game in the status object
        StatusObject.GameInfo info = new StatusObject.GameInfo();
        info.gameOid = _bangobj.getOid();
        info.players = getPlayerCount();
        for (int ii = 0; ii < getPlayerCount(); ii++) {
            if (isAI(ii)) {
                info.players--;
            }
        }
        info.scenarios = _bconfig.scenarios;
        BangServer.statobj.addToGames(info);

        // note the time at which we started
        _startStamp = System.currentTimeMillis();

        BoardRecord[] boards = null;
        // load up the named board if one was named
        if (!StringUtil.isBlank(_bconfig.board)) {
            BoardRecord brec = BangServer.boardmgr.getBoard(
                _bconfig.players.length, _bconfig.board);
            if (brec != null) {
                boards = new BoardRecord[_bconfig.scenarios.length];
                Arrays.fill(boards, brec);
            } else {
                log.warning("Failed to locate '" + _bconfig.board + "' " +
                            "[where=" + where() + "].");
                String msg = MessageBundle.tcompose(
                    "m.no_such_board", _bconfig.board);
                SpeakProvider.sendAttention(_bangobj, GAME_MSGS, msg);
            }

        } else if (_bconfig.bdata != null) {
            try {
                BoardRecord brec = new BoardRecord();
                brec.load(new ByteArrayInputStream(_bconfig.bdata));
                boards = new BoardRecord[_bconfig.scenarios.length];
                Arrays.fill(boards, brec);
            } catch (Exception e) {
                String msg = MessageBundle.tcompose(
                    "m.board_load_failed", e.getMessage());
                SpeakProvider.sendAttention(_bangobj, GAME_MSGS, msg);
                log.log(Level.WARNING, "Failed to load board from data.", e);
            }
        }

        // if no boards were specified otherwise, pick them randomly
        if (boards == null) {
            boards = BangServer.boardmgr.selectBoards(
                _bconfig.players.length, _bconfig.scenarios);
        }

        // set up our round records
        _rounds = new RoundRecord[_bconfig.getRounds()];
        for (int ii = 0; ii < _rounds.length; ii++) {
            _rounds[ii] = new RoundRecord();
            _rounds[ii].board = boards[ii];
        }

        // configure the town associated with this server
        _bangobj.setTownId(ServerConfig.getTownId());

        // create our per-player arrays
        int slots = getPlayerSlots();
        _bangobj.points = new int[slots];
        _bangobj.perRoundPoints = new int[_bconfig.scenarios.length][slots];
        for (int ii = 0; ii < _bangobj.perRoundPoints.length; ii++) {
            Arrays.fill(_bangobj.perRoundPoints[ii], -1);
        }
        _bangobj.pdata = new BangObject.PlayerData[slots];
        for (int ii = 0; ii < slots; ii++) {
            _bangobj.pdata[ii] = new BangObject.PlayerData();
        }
        resetPreparingStatus(false);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();
        PresentsServer.invmgr.clearDispatcher(_bangobj.service);
        BangServer.statobj.removeFromGames(_bangobj.getOid());
        log.info("Manager shutdown [where=" + where() + "].");
    }

    @Override // documentation inherited
    protected void playersAllHere ()
    {
        switch (_bangobj.state) {
        case BangObject.PRE_GAME:
            // create our player records now that we know everyone's in the
            // room and ready to go
            _precords = new PlayerRecord[getPlayerSlots()];
            int[][] avatars = new int[getPlayerSlots()][];
            for (int ii = 0; ii < _precords.length; ii++) {
                PlayerRecord prec = (_precords[ii] = new PlayerRecord());
                prec.finishedTick = new int[_bconfig.getRounds()];
                if (isAI(ii)) {
                    prec.playerId = -1;
                    prec.ratings = new DSet();
                    avatars[ii] = ((BangAI)_AIs[ii]).avatar;
                } else if (isActivePlayer(ii)) {
                    prec.user = (PlayerObject)getPlayer(ii);
                    prec.playerId = prec.user.playerId;
                    prec.purse = prec.user.getPurse();
                    prec.ratings = prec.user.ratings;
                    Look look = prec.user.getLook(Look.Pose.DEFAULT);
                    if (look != null) {
                        avatars[ii] = look.getAvatar(prec.user);
                    }
                }
            }
            _bangobj.setAvatars(avatars);
            // when the players all arrive, go into the buying phase
            startRound();
            break;

        case BangObject.SELECT_PHASE:
        case BangObject.PRE_TUTORIAL:
            // start the test/tutorial
            _scenario.startNextPhase(_bangobj);
            break;

        case BangObject.IN_PLAY:
            // queue up the first board tick
            _ticker.schedule(getTickTime(), false);
            // let the players know we're ready to go with the first tick
            _bangobj.setTick((short)0);
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
                    selectStarters(ii, _aiLogic[ii].getBigShotType(),
                        _aiLogic[ii].getCardTypes());
                }
            }
            break;

        case BangObject.BUYING_PHASE:
            // make purchases for our AIs
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                if (isAI(ii)) {
                    selectTeam(ii, _aiLogic[ii].getUnitTypes(_bconfig.teamSize),
                               null);
                }
            }
            break;
        }
    }

    /** Starts the pre-game buying phase. */
    protected void startRound ()
    {
        // set the tick to -1 during the pre-round
        _bangobj.setTick((short)-1);

        // set up our stats for this round; note that roundId is not yet
        // incremented to the actual roundId
        StatSet[] stats = new StatSet[getPlayerSlots()];
        for (int ii = 0; ii < stats.length; ii++) {
            stats[ii] = new StatSet();
        }
        _rounds[_bangobj.roundId].stats = stats;
        _bangobj.stats = stats;

        // find out if the desired board has been loaded, loading it if not
        final BoardRecord brec = _rounds[_bangobj.roundId].board;
        if (brec.data != null) {
            continueStartingRound(brec);
            return;
        }
        BangServer.boardmgr.loadBoardData(
            brec, new ResultListener<BoardRecord>() {
            public void requestCompleted (BoardRecord record) {
                continueStartingRound(record);
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to load board " + brec, cause);
            }
        });
    }

    /** Continues starting the round once the board's data is loaded. */
    protected void continueStartingRound (BoardRecord brec)
    {
        // make sure we've got a board to work with
        BangBoard board = brec.getBoard();
        if (board == null) {
            log.warning("Have no board. We're hosed! " + brec + ".");
            return;
        }

        // create the appropriate scenario to handle this round
        if (_bconfig.tutorial) {
            _bangobj.setScenarioId(ScenarioCodes.TUTORIAL);
            _scenario = new Tutorial();
            // we reuse the playerIsReady() mechanism to wait for the player to
            // be ready to start the tutorial; normally they'd select their
            // bigshot, but that doesn't happen in a tutorial
            resetPlayerOids();
        } else {
            _bangobj.setScenarioId(_bconfig.scenarios[_bangobj.roundId]);
            _scenario = ScenarioFactory.createScenario(_bangobj.scenarioId);
        }
        _scenario.init(this);

        // create the logic for our ai players, if any
        int aicount = (_AIs == null) ? 0 : _AIs.length;
        _aiLogic = new AILogic[aicount];
        for (int ii = 0; ii < aicount; ii++) {
            if (_AIs[ii] != null) {
                _aiLogic[ii] = _scenario.createAILogic(_AIs[ii]);
                _aiLogic[ii].init(this, ii);
            }
        }

        // clear out the various per-player data structures
        _purchases.clear();

        // set up the board and pieces so it's visible while purchasing
        _bangobj.board =(BangBoard)board.clone();
        _bangobj.setBoardName(brec.name);
        _bangobj.setBoardHash(brec.dataHash);

        // clone the pieces we get from the board record as we may modify them
        // during the course of the game
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        Piece[] pvec = brec.getPieces();
        for (int ii = 0; ii < pvec.length; ii++) {
            Piece p = (Piece)pvec[ii].clone();
            // sanity check our pieces
            if (p.x < 0 || p.x >= _bangobj.board.getWidth() ||
                p.y < 0 || p.y >= _bangobj.board.getHeight()) {
                log.warning("Beward! Out of bounds piece " + p + ".");
            }
            pieces.add(p);
        }

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
        // store them in the bang object for initial camera positions
        _bangobj.startPositions = new StreamablePoint[_starts.size()];
        for (int ii = 0, nn = _starts.size(); ii < nn; ii++) {
            Piece start = _starts.get(ii);
            _bangobj.startPositions[ii] = new StreamablePoint(start.x,
                start.y);
        }
        _bangobj.setStartPositions(_bangobj.startPositions);

        // give the scenario a shot at its own custom markers
        _scenario.filterMarkers(_bangobj, _starts, pieces);

        // remove any remaining marker pieces and assign piece ids
        _bangobj.maxPieceId = 0;
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (p instanceof Marker) {
                iter.remove();
                continue;
            }
            p.assignPieceId(_bangobj);
        }

        // configure the game object and board with the pieces
        _bangobj.pieces = new PieceDSet(pieces.iterator());
        _bangobj.board.shadowPieces(pieces.iterator());

        // clear out the selected big shots array
        _bangobj.setBigShots(new Unit[getPlayerSlots()]);

        // configure anyone who is not in the game room as resigned for this
        // round; this is be preserved through calls to resetPreparingStatus
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            if (isAI(ii)) {
                continue;
            }
            PlayerObject user = (PlayerObject)getPlayer(ii);
            if (user == null || user.status == OccupantInfo.DISCONNECTED) {
                _bangobj.setPlayerStatusAt(BangObject.PLAYER_LEFT_GAME, ii);
            }
        }

        // transition to the pre-game selection phase
        _scenario.startNextPhase(_bangobj);
    }

    /**
     * Selects the starting configuration for an AI player.
     */
    protected void selectStarters (
        int pidx, String bigShotType, String[] cardTypes)
    {
        Card[] cards = null;
        if (cardTypes != null) {
            cards = new Card[cardTypes.length];
            for (int ii = 0; ii < cards.length; ii++) {
                cards[ii] = Card.newCard(cardTypes[ii]);
            }
        }
        selectStarters(pidx, new BigShotItem(-1, bigShotType), cards);
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
            unit.originalOwner = pidx;
            _bangobj.setBigShotsAt(unit, pidx);

            // note that they're done with this phase
            _bangobj.setPlayerStatusAt(BangObject.PLAYER_IN_PLAY, pidx);

        } finally {
            _bangobj.commitTransaction();
        }

        // if everyone has selected their starters, move to the next phase
        checkStartNextPhase();
    }

    /**
     * Configures the specified player's purchases for this round and starts
     * the game if they are the last to configure.
     */
    protected void selectTeam (int pidx, String[] types, PlayerObject user)
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
        }

        // if this is a human player, make sure they didn't request units to
        // which they don't have access
        if (user != null) {
            for (int ii = 0; ii < units.length; ii++) {
                UnitConfig config = units[ii].getConfig();
                if (config.scripCost < 0 || !config.hasAccess(user)) {
                    log.warning("Player requested to purchase illegal unit " +
                                "[who=" + user.who() +
                                ", unit=" + config.type + "].");
                    return;
                }
            }

            // TODO: make sure they didn't request more than their allowed
            // number of each unit (currently one)
        }

        // initialize and prepare the units
        for (int ii = 0; ii < units.length; ii++) {
            units[ii].assignPieceId(_bangobj);
            units[ii].init();
            units[ii].owner = pidx;
            units[ii].originalOwner = pidx;
            _purchases.add(units[ii]);
        }

        // note that this player is ready and potentially fire away
        _bangobj.setPlayerStatusAt(BangObject.PLAYER_IN_PLAY, pidx);
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
        // if all players are AIs, wait for playerReady signal before starting
        if (_bconfig.allPlayersAIs() &&
            _bangobj.state == BangObject.SELECT_PHASE) {
            return;
        }
        if (_bangobj.state == BangObject.SELECT_PHASE ||
            _bangobj.state == BangObject.BUYING_PHASE) {
            for (int ii = 0; ii < _bangobj.playerStatus.length; ii++) {
                // if anyone is still preparing, we're not ready
                if (_bangobj.isActivePlayer(ii) &&
                    _bangobj.playerStatus[ii] == BangObject.PLAYER_PREPARING) {
                    return;
                }
            }
            _scenario.startNextPhase(_bangobj);
        }
    }

    @Override // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // add the selected big shots to the purchases
        for (int ii = 0; ii < _bangobj.bigShots.length; ii++) {
            if (_bangobj.isActivePlayer(ii) && _bangobj.bigShots[ii] != null) {
                _purchases.add(_bangobj.bigShots[ii]);
            }
        }

        // now place and add the player pieces
        try {
            _bangobj.startTransaction();

            // override the player status set in super.gameWillStart()
            resetPreparingStatus(true);

            try {
                // let the scenario know that we're about to start the round
                _scenario.roundWillStart(_bangobj, _starts, _purchases);

                // configure the duration of the round
                _bangobj.setDuration(_scenario.getDuration(_bconfig));
                _bangobj.setLastTick((short)(_bangobj.duration - 1));

                // note this round's duration for later processing (roundId is
                // now the actual roundId and thus we have to subtract 1)
                _rounds[_bangobj.roundId-1].duration = _bangobj.duration;

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
                // changing their perRoundPoints from -1 to zero
                _bangobj.perRoundPoints[_bangobj.roundId-1][ii] = 0;

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
        resetPlayerOids();
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
                    log.fine("Expiring wreckage " + p.pieceId +
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

        // note that all active players completed this tick
        int ridx = _bangobj.roundId-1;
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            if (_bangobj.isActivePlayer(ii)) {
                _precords[ii].finishedTick[ridx] = _bangobj.tick;
            }
        }

        // determine whether we should end the game
        if (tick >= _bangobj.lastTick) {
            // let the scenario do any end of round business
            _scenario.roundDidEnd(_bangobj);

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

        // execute any advance orders
        int executed = 0;
        for (Iterator<AdvanceOrder> iter = _orders.iterator();
             iter.hasNext(); ) {
            AdvanceOrder order = iter.next();
            if (order.unit.ticksUntilMovable(tick) <= 0) {
                try {
                    executeOrder(order.unit, order.x, order.y,
                                 order.targetId, false);
                    executed++;
                } catch (InvocationException ie) {
                    reportInvalidOrder(order, ie.getMessage());
                }
                iter.remove();
            }
        }

        // if we executed any orders, validate the remainder
        if (executed > 0) {
            validateOrders();
        }

        // give our AI players a chance to move but not on the zeroth tick
        if (!_bconfig.tutorial && tick > 0) {
            for (int ii = 0; ii < _aiLogic.length; ii++) {
                if (_aiLogic[ii] != null) {
                    _aiLogic[ii].tick(pieces, tick);
                }
            }
        }

        try {
            _bangobj.startTransaction();
            // potentially create and add new bonuses
            if (_scenario.addBonus(_bangobj, _bangobj.getPieceArray())) {
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

        // clear out pending orders
        _orders.clear();

        // start the next round
        startRound();
    }

    @Override // documentation inherited
    protected void gameWasCancelled ()
    {
        super.gameWasCancelled();

        // record this game to the server stats log
        recordGame(null, 0);
    }

    @Override // documentation inherited
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

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

        // note the duration of the game (in minutes and seconds)
        int gameSecs = (int)(System.currentTimeMillis() - _startStamp) / 1000;

        // update ratings if appropriate
        if (_bconfig.rated &&
            !_bconfig.scenarios[0].equals(ScenarioCodes.TUTORIAL) &&
            gameSecs >= MIN_RATED_DURATION) {
            // update each player's per-scenario ratings
            for (int ii = 0; ii < _bconfig.scenarios.length; ii++) {
                String scenario = _bconfig.scenarios[ii];
                computeRatings(scenario, _bangobj.perRoundPoints[ii]);
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
            award.rank = -1;

            // note this player's rank
            if (_ranks != null) {
                for (int rr = 0; rr < _ranks.length; rr++) {
                    if (_ranks[rr].pidx == ii) {
                        award.rank = rr;
                        break;
                    }
                }
            }

            // stop here for non-humans
            if (isAI(ii)) {
                continue;
            }

            // if this player never showed up, skip the rest
            PlayerRecord prec = _precords[ii];
            if (prec.user == null) {
                continue;
            }

            // compute this player's "take home" cash
            if (prec.playerId > 0 && _scenario.shouldPayEarnings(prec.user)) {
                award.cashEarned = computeEarnings(ii);
            }

            // if this was a rated (matched) game, persist various stats and
            // potentially award a badge
            if (_bconfig.rated) {
                try {
                    recordStats(prec.user, ii, award, gameSecs/60);
                } catch (Throwable t) {
                    log.log(Level.WARNING, "Failed to record stats " +
                            "[who=" + _bangobj.players[ii] + ", idx=" + ii +
                            ", award=" + award + "].", t);
                }

            } else if (prec.user.isActive()) {
                // we only track one stat for unranked games, the number played
                prec.user.stats.incrementStat(
                    Stat.Type.UNRANKED_GAMES_PLAYED, 1);
            }
        }

        // broadcast the per-round earnings which will be displayed on one
        // stats panel
        _bangobj.setPerRoundPoints(_bangobj.perRoundPoints);

        // record this game to the server stats log (before we sort the awards)
        recordGame(awards, gameSecs);

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

        // if we haven't just lost our last human player, check to see if we
        // should start the next phase
        if (getActiveHumanCount() > 0) {
            checkStartNextPhase();
        }
        // otherwise just let the game be ended or cancelled
    }

    @Override // documentation inherited
    protected boolean shouldEndGame ()
    {
        return _bangobj.isInPlay() && (getActiveHumanCount() == 0 ||
                                       _gameobj.getActivePlayerCount() == 1);
    }

    @Override // documentation inherited
    protected void assignWinners (boolean[] winners)
    {
        // compute the final ranking of each player, resolving ties using kill
        // count, then a random ordering
        int[] points = _bangobj.getFilteredPoints();
        _ranks = new RankRecord[points.length];
        for (int ii = 0; ii < _ranks.length; ii++) {
            int kills = 0;
            for (int rr = 0; rr < _rounds.length; rr++) {
                if (_rounds[rr].stats != null) {
                    kills += _rounds[rr].stats[ii].getIntStat(
                        Stat.Type.UNITS_KILLED);
                }
            }
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
     * @return the cloned and moved piece if the piece was moved.
     */
    protected Unit moveUnit (Unit unit, int x, int y, Piece target)
        throws InvocationException
    {
        // compute the possible moves for this unit
        _moves.clear();
        _bangobj.board.computeMoves(unit, _moves, null);

        // if we have not specified an exact move, locate one now
        if (x == Short.MAX_VALUE) {
            if (target == null) {
                // the target must no longer be around, so abandon ship
                throw new InvocationException(TARGET_NO_LONGER_VALID);
            }

            Point spot = unit.computeShotLocation(
                _bangobj.board, target, _moves);
            if (spot == null) {
//                 log.info("Unable to find place from which to shoot. " +
//                          "[piece=" + unit.info() +
//                          ", target=" + target.info() +
//                          ", moves=" + _moves + "].");
                throw new InvocationException(TARGET_UNREACHABLE);
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
//             log.info("Unit no longer movable [unit=" + unit +
//                      ", alive=" + unit.isAlive() +
//                      ", mticks=" + unit.ticksUntilMovable(_bangobj.tick) +
//                      "].");
            throw new InvocationException(MOVER_NO_LONGER_VALID);
        }

        // validate that the move is still legal
        if (!_moves.contains(x, y)) {
//             log.info("Unit requested invalid move [unit=" + unit.info() +
//                      ", x=" + x + ", y=" + y + ", moves=" + _moves + "].");
                throw new InvocationException(MOVE_BLOCKED);
        }

        // clone and move the unit
        Unit munit = (Unit)unit.clone();
        munit.position(x, y);
        munit.lastActed = _bangobj.tick;

        // ensure that we don't land on any piece that prevents us from
        // overlapping
        ArrayList<Piece> lappers = _bangobj.getOverlappers(munit);
        if (lappers != null) {
            for (Piece lapper : lappers) {
                if (lapper.preventsOverlap(munit)) {
                    throw new InvocationException(MOVE_BLOCKED);
                }
            }
        }

        // make sure we can still reach and shoot any potential target before
        // we go ahead with our move
        checkTarget(munit, target);

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
     * Checks that the specified unit can reach and shoot the specified
     * target. Throws an invocation exception if that is no longer the case
     * (ie. the target moved out of range or died). Target may be null in which
     * case this method does nothing.
     */
    protected void checkTarget (Piece shooter, Piece target)
        throws InvocationException
    {
        if (target == null) {
            return;
        }

        // make sure the target is still valid
        if (!shooter.validTarget(target, false)) {
            // target already dead or something
//             log.info("Target no longer valid [shooter=" + shooter.info() +
//                      ", target=" + target.info() + "].");
            throw new InvocationException(TARGET_NO_LONGER_VALID);
        }

        // make sure the target is still reachable
        if (!shooter.targetInRange(target.x, target.y) ||
            !shooter.checkLineOfSight(
                _bangobj.board, shooter.x, shooter.y, target)) {
//             log.info("Target no longer reachable " +
//                      "[shooter=" + shooter.info() +
//                      ", target=" + target.info() + "].");
            throw new InvocationException(TARGET_UNREACHABLE);
        }
    }

    /**
     * Scans the list of advance orders and clears any that have become
     * invalid.
     */
    protected void validateOrders ()
    {
        for (Iterator<AdvanceOrder> iter = _orders.iterator();
             iter.hasNext(); ) {
            AdvanceOrder order = iter.next();
            String cause = order.checkValid();
            if (cause != null) {
                iter.remove();
                reportInvalidOrder(order, cause);
            }
        }
    }

    /**
     * Clears any advance order for the specified unit.
     */
    protected void clearOrders (int unitId)
    {
        for (int ii = 0, ll = _orders.size(); ii < ll; ii++) {
            if (_orders.get(ii).unit.pieceId == unitId) {
                _orders.remove(ii);
                return;
            }
        }
    }

    /**
     * Reports an invalidated order to the initiating player.
     */
    protected void reportInvalidOrder (AdvanceOrder order, String reason)
    {
        PlayerObject user = (PlayerObject)getPlayer(order.unit.owner);
        if (user != null) {
//             log.info("Advance order failed [order=" + order +
//                      ", who=" + user.who() + "].");
            BangSender.orderInvalidated(user, order.unit.pieceId, reason);
//         } else {
//             log.info("Advance order failed [order=" + order + "].");
        }
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
        // if we never set up our ranks, then no one gets nuthin
        if (_ranks == null) {
            return 0;
        }

        int earnings = 0;
        for (int rr = 0; rr < _bconfig.getRounds(); rr++) {
            // if the round was not played, skip it
            if (_rounds[rr].duration == 0) {
                continue;
            }

            // scale their earnings by the number of players they defeated in
            // each round
            int defeated = 0, aisDefeated = 0;
            for (int ii = _ranks.length-1; ii >= 0; ii--) {
                // stop when we get to our record
                if (_ranks[ii].pidx == pidx) {
                    break;
                }

                // require that the opponent finished at least half the round
                if (_precords[_ranks[ii].pidx].finishedTick[rr] <
                    _rounds[rr].duration/2) {
                    continue;
                }

                if (isAI(_ranks[ii].pidx)) {
                    // only the first AI counts toward earnings
                    if (++aisDefeated <= 1) {
                        defeated++;
                    }
                } else {
                    defeated++;
                }
            }

            log.fine("Noting earnings p:" + _bangobj.players[pidx] +
                     " r:" + rr + " (" + _precords[pidx].finishedTick[rr] +
                     " * " + BASE_EARNINGS[defeated] + " / " +
                     _rounds[rr].duration + ").");

            // scale the player's earnings based on the percentage of the round
            // they completed
            earnings += (_precords[pidx].finishedTick[rr] *
                         BASE_EARNINGS[defeated] / _rounds[rr].duration);
        }

        // and scale earnings based on their purse
        return Math.round(_precords[pidx].purse.getPurseBonus() * earnings);
    }

    /**
     * Computes updated ratings for the specified scenario, using the supplied
     * scores and stores them in the appropriate {@link PlayerRecord}.
     */
    protected void computeRatings (String scenario, int[] scores)
    {
        // filter AIs from the scores; the ratings computations below will
        // ignore players whose score is set to zero
        scores = (int[])scores.clone();
        for (int ii = 0; ii < scores.length; ii++) {
            if (isAI(ii)) {
                scores[ii] = 0;
            }
        }

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
     * Records game stats to the player's persistent stats and potentially
     * awards them a badge. This is only called for rated (matched) games.
     */
    protected void recordStats (
        final PlayerObject user, int pidx, Award award, int gameMins)
    {
        // if this player has logged off...
        if (!user.isActive()) {
            // ...we won't update any of their cumulative stats, but we need to
            // wipe their consecutive wins stat
            BangServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    Stat stat = Stat.Type.CONSEC_WINS.newStat();
                    stat.setModified(true);
                    BangServer.statrepo.writeModified(
                        user.playerId, new Stat[] { stat });
                    return false;
                }
            });
            return;
        }

        // send all the stat updates out in one dobj event
        user.startTransaction();

        try {
            // if the game wasn't sufficiently long, certain stats don't count
            if (gameMins >= MIN_STATS_DURATION) {
                user.stats.incrementStat(Stat.Type.GAMES_PLAYED, 1);
                user.stats.incrementStat(Stat.Type.GAME_TIME, gameMins);
                // increment consecutive wins for 1st place only
                if (award.rank == 0) {
                    user.stats.incrementStat(Stat.Type.GAMES_WON, 1);
                    user.stats.incrementStat(Stat.Type.CONSEC_WINS, 1);
                } else {
                    user.stats.setStat(Stat.Type.CONSEC_WINS, 0);
                }
                // increment consecutive losses for 4th place only
                if (award.rank == 3) {
                    user.stats.incrementStat(Stat.Type.CONSEC_LOSSES, 1);
                } else {
                    user.stats.setStat(Stat.Type.CONSEC_LOSSES, 0);
                }
            }

            // these stats count regardless of the game duration
            for (int rr = 0; rr < _rounds.length; rr++) {
                if (_rounds[rr].stats == null) {
                    continue; // skip unstarted rounds
                }

                // accumulate stats tracked during this round
                for (int ss = 0; ss < ACCUM_STATS.length; ss++) {
                    Stat.Type type = ACCUM_STATS[ss];
                    // we don't subtract accumulating stats if the player
                    // "accumulated" negative points in the game
                    int value = _rounds[rr].stats[pidx].getIntStat(type);
                    if (value > 0) {
                        user.stats.incrementStat(type, value);
                    }
                }

                // check to see if any "max" stat was exceeded in this round
                user.stats.maxStat(Stat.Type.HIGHEST_POINTS,
                                   _bangobj.perRoundPoints[rr][pidx]);
                user.stats.maxStat(Stat.Type.MOST_KILLS,
                                   _rounds[rr].stats[pidx].getIntStat(
                                       Stat.Type.UNITS_KILLED));
            }

            // note their cash earned
            user.stats.incrementStat(
                Stat.Type.CASH_EARNED, award.cashEarned);

            // allow the scenario to record statistics as well
            _scenario.recordStats(_bangobj, gameMins, pidx, user);

            // determine whether this player qualifies for a new badge
            award.badge = Badge.checkQualifies(user);

        } finally {
            user.commitTransaction();
        }
    }

    /**
     * Records the relevant state of an ended or cancelled game.
     */
    protected void recordGame (Award[] awards, int gameSecs)
    {
        try {
            StringBuffer buf = new StringBuffer(
                (awards == null) ? "game_cancelled" : "game_ended");
            buf.append(" t:").append(gameSecs);
            buf.append(" s:").append(StringUtil.join(_bconfig.scenarios, ","));
            buf.append(" ts:").append(_bconfig.teamSize);
            buf.append(" r:").append(_bconfig.rated);
            buf.append(" ");
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                if (ii > 0) {
                    buf.append(",");
                }

                // record the player in this position
                if (isAI(ii)) {
                    buf.append("(tin_can)");
                    continue;
                }
                if (_precords == null || _precords[ii] == null ||
                    _precords[ii].user == null) {
                    buf.append("(never_arrived)");
                    continue;
                }
                buf.append(_precords[ii].user.username);

                // note players that left the game early
                if (!_bangobj.isActivePlayer(ii)) {
                    PlayerObject pobj = BangServer.lookupPlayer(
                        _precords[ii].user.handle);
                    if (pobj == null) {
                        buf.append("*"); // no longer online
                    } else if (pobj.status == OccupantInfo.DISCONNECTED) {
                        buf.append("!"); // disconnected
                    } else {
                        buf.append("#"); // online and active
                    }
                }

                // record their awards if we have any
                if (awards != null) {
                    buf.append(":").append(awards[ii]);
                }
            }
            BangServer.generalLog(buf.toString());

        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to log game data.", t);
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
                for (int ii = 0; ii < awards.length; ii++) {
                    int pidx = awards[ii].pidx;
                    PlayerObject player = _precords[pidx].user;
                    if (player == null || !player.isActive()) {
                        // no need to update their player distributed object if
                        // they've already logged off
                        continue;
                    }
                    if (awards[ii].cashEarned > 0) {
                        player.setScrip(player.scrip + awards[ii].cashEarned);
                    }
                    if (awards[ii].badge != null) {
                        player.addToInventory(awards[ii].badge);
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
        log.fine("Noting played cards [updates=" + updates.size() +
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
        
        } else if (_bconfig.allPlayersAIs()) {
            // fast ticks for auto-play test games
            return 2000L;
            
        } else {
            // start out with a base tick of two seconds and scale it down as
            // the game progresses; cap it at ten minutes
            long delta = System.currentTimeMillis() - _startStamp;
            delta = Math.min(delta, TIME_SCALE_CAP);

            // scale from 1/1 to 2/3 over the course of ten minutes
            float factor = 1f + 0.5f * delta / TIME_SCALE_CAP;
            long baseTime = (long)Math.round(BASE_TICK_TIME / factor);

            // scale this base time by the average number of units in play
            long tickTime = baseTime * _bangobj.getAverageUnitCount();

            // make sure the tick is at least one second long
            return Math.max(tickTime, 1000L);
        }
    }

    /**
     * Counts the number of active humans in the game.
     */
    protected int getActiveHumanCount ()
    {
        int humanCount = 0;
        for (int ii = 0; ii < getPlayerSlots(); ii++) {
            if (isActivePlayer(ii) && !isAI(ii)) {
                humanCount++;
            }
        }
        return humanCount;
    }

    /**
     * Resets all player status to preparing. We do this element by element
     * rather than setting one array because there is the chance that
     * unprocessed element sets in the queue will overwrite what we set.
     */
    protected void resetPreparingStatus (boolean aisAreReady)
    {
        boolean dotrans = !_bangobj.inTransaction();
        if (dotrans) {
            _bangobj.startTransaction();
        }
        try {
            for (int ii = 0; ii < getPlayerSlots(); ii++) {
                int status = BangObject.PLAYER_PREPARING;
                if (isAI(ii) && aisAreReady) {
                    status = BangObject.PLAYER_IN_PLAY;
                }
                // don't override the status of players that have left the game
                if (_bangobj.playerStatus[ii] == BangObject.PLAYER_LEFT_GAME) {
                    status = BangObject.PLAYER_LEFT_GAME;
                }
                _bangobj.setPlayerStatusAt(status, ii);
            }
        } finally {
            if (dotrans) {
                _bangobj.commitTransaction();
            }
        }
    }

    /**
     * Resets the player oid of all active players so that they can report in
     * once again that they are ready and we can trigger on {@link
     * #playersAllHere} for different phases of the game.
     */
    protected void resetPlayerOids ()
    {
        for (int ii = 0; ii < _playerOids.length; ii++) {
            if (isActivePlayer(ii)) {
                _playerOids[ii] = 0;
            }
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

    /** Used to track advance orders. */
    protected class AdvanceOrder
    {
        /** The unit to be ordered. */
        public Unit unit;

        /** The coordinates to which to move the unit. */
        public short x, y;

        /** The target to attack after moving. */
        public int targetId;

        public AdvanceOrder (Unit unit, short x, short y, int targetId) {
            this.unit = unit;
            this.x = x;
            this.y = y;
            this.targetId = targetId;
        }

        public String checkValid () {
            // sanity check, though I think this bug is fixed
            Object obj = _bangobj.pieces.get(unit.pieceId);
            if (obj != null && !(obj instanceof Unit)) {
                log.warning("Our unit became a non-unit!? [where=" + where() +
                            ", unit=" + unit.info() + ", nunit=" + obj + "].");
                return INTERNAL_ERROR;
            }

            // make sure this unit is still in play
            Unit aunit = (Unit)obj;
            if (aunit == null || !aunit.isAlive()) {
                return MOVER_NO_LONGER_VALID;
            }

            // make sure our target is still around
            Piece target = null;
            if (targetId > 0) {
                target = (Piece)_bangobj.pieces.get(targetId);
                if (target == null || !target.isAlive()) {
                    return TARGET_NO_LONGER_VALID;
                }
            }

            // compute our potential move and attack set
            _moves.clear();
            _attacks.clear();
            _bangobj.board.computeMoves(unit, _moves, null);

            // if no specific location was specified, make sure we can still
            // determine a location from which to fire
            if (x == Short.MAX_VALUE) {
                if (target == null) { // sanity check
                    return TARGET_NO_LONGER_VALID;
                }
                return (unit.computeShotLocation(_bangobj.board, target,
                    _moves) == null) ? TARGET_UNREACHABLE : null;
            }

            // if a specific location was specified, make sure we can
            // still reach it
            if (!_moves.contains(x, y)) {
                return MOVE_BLOCKED;
            }

            // if we have no target, we're good to go
            if (target == null) {
                return null;
            }

            // we are doing a move and shoot, so make sure we can still hit the
            // target from our desired move location
            int tdist = target.getDistance(x, y);
            if (tdist < unit.getMinFireDistance() ||
                tdist > unit.getMaxFireDistance() ||
                !unit.checkLineOfSight(_bangobj.board, x, y, target)) {
                return TARGET_UNREACHABLE;
            }

            return null;
        }

        public String toString() {
            return unit.info() + " -> +" + x + "+" + y + " (" + targetId + ")";
        }
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
        public int[] finishedTick;

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

    /** Contains information about each round played. */
    protected static class RoundRecord
    {
        /** The duration of this round in ticks. */
        public int duration;

        /** The board we played on this round. */
        public BoardRecord board;

        /** A snapshot of the in-game stats at the end of this round. */
        public StatSet[] stats;
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
            _scenario.pieceMoved(_bangobj, piece);
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

    /** Contains info on each round that we played. */
    protected RoundRecord[] _rounds;

    /** Contains info on all of the players in the game. */
    protected PlayerRecord[] _precords;

    /** Used at the end of the game to rank the players. */
    protected RankRecord[] _ranks;

    /** Implements our gameplay scenario. */
    protected Scenario _scenario;

    /** The logic for the artificial players. */
    protected AILogic[] _aiLogic;

    /** The time at which the round started. */
    protected long _startStamp;

    /** The purchases made by players in the buying phase. */
    protected PieceSet _purchases = new PieceSet();

    /** Used to record damage done during an attack. */
    protected IntIntMap _damage = new IntIntMap();

    /** Used to compute a piece's potential moves or attacks when
     * validating a move request. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();

    /** Used to track the locations where players can start. */
    protected ArrayList<Piece> _starts = new ArrayList<Piece>();

    /** Maps card id to a {@link StartingCard} record. */
    protected HashIntMap<StartingCard> _scards = new HashIntMap<StartingCard>();

    /** The time for which the next tick is scheduled. */
    protected long _nextTickTime;

    /** The extra time to take for the current tick to allow extended effects
     * to complete. */
    protected long _extraTickTime;

    /** Tracks advance orders. */
    protected ArrayList<AdvanceOrder> _orders = new ArrayList<AdvanceOrder>();

    /** Our starting base tick time. */
    protected static final long BASE_TICK_TIME = 2000L;

    /** We stop reducing the tick time after ten minutes. */
    protected static final long TIME_SCALE_CAP = 10 * 60 * 1000L;

    /** If a game is shorter than this (in seconds) we won't rate it. */
    protected static final int MIN_RATED_DURATION = 180;

    /** If a game is shorter than this (in minutes) some stats don't count. */
    protected static final int MIN_STATS_DURATION = 2;

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
    protected static final int[] BASE_EARNINGS = { 50, 70, 85, 105 };
}
