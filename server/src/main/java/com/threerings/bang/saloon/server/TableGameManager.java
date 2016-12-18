//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashSet;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.media.util.MathUtil;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.saloon.data.ParlorGameConfig.Slot;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.TableGameMarshaller;
import com.threerings.bang.saloon.data.TableGameObject;

import static com.threerings.bang.Log.log;

/**
 * Handles configuring, and starting a table game.
 */
public class TableGameManager implements TableGameProvider
{
    public TableGameManager ()
    {
        _tobj = new TableGameObject();
        BangServer.omgr.registerObject(_tobj);
        _tobj.setService(BangServer.invmgr.registerProvider(this, TableGameMarshaller.class));
    }

    /**
     * Called to shutdown the manager.
     */
    public void shutdown ()
    {
        if (_tobj != null) {
            BangServer.invmgr.clearDispatcher(_tobj.service);
            BangServer.omgr.destroyObject(_tobj.getOid());
            _tobj = null;
        }
    }

    /**
     * Returns the TableGameObject.
     */
    public TableGameObject getTableGameObject ()
    {
        return _tobj;
    }

    // documentation inherited from interface TableGameProvider
    public void updateGameConfig (PlayerObject caller, ParlorGameConfig game)
    {
        // if we're already matchmaking, reject any config updates
        if (_tobj.playerOids != null) {
            return;
        }

        _tobj.setGame(game);
    }

    // documentation inherited from interface TableGameProvider
    public void startMatchMaking (PlayerObject caller, ParlorGameConfig game, byte[] bdata,
            InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }

        // if we've already started, then just turn this into a join
        if (_tobj.playerOids != null) {
            joinMatch(caller);
            listener.requestProcessed();
            return;
        }

        // sanity check the configuration
        game.slots[0] = Slot.HUMAN;
        if (game.getCount(Slot.TINCAN) == 0 && game.getCount(Slot.HUMAN) < 2) {
            game.slots[1] = Slot.HUMAN;
        }
        if (game.mode == ParlorGameConfig.Mode.TEAM_2V2) {
            for (int ii = 0; ii < game.slots.length; ii++) {
                if (game.slots[ii] == Slot.NONE) {
                    game.slots[ii] = Slot.TINCAN;
                }
            }

        }
        game.rounds = MathUtil.bound(1, game.rounds, GameCodes.MAX_ROUNDS);
        game.teamSize = MathUtil.bound(1, game.teamSize, GameCodes.MAX_TEAM_SIZE);
        if (game.scenarios == null || game.scenarios.length == 0) {
            game.scenarios = ScenarioInfo.getScenarioIds(ServerConfig.townId, false);
        }

        // update the game config with the desired config
        _tobj.setGame(game);

        // if this player is an admin, allow the board data
        if (caller.tokens.isAdmin()) {
            _bdata = bdata;
        }

        // create a playerOids array and stick the starter in slot zero
        _tobj.playerOids = new int[game.getCount(Slot.HUMAN)];
        _tobj.playerOids[0] = caller.getOid();
        _tobj.setPlayerOids(_tobj.playerOids);

        // start a "start the game" timer if we're ready to go
        checkStart();
        listener.requestProcessed();
    }

    // documentation inherited from interface TableGameProvider
    public void joinMatch (PlayerObject caller)
    {
        // make sure the match wasn't cancelled
        if (_tobj.playerOids == null) {
            return;
        }

        // look for a spot, and make sure they're not already joined
        int idx = -1;
        for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
            if (idx == -1 && _tobj.playerOids[ii] == 0) {
                idx = ii;
            }
            if (_tobj.playerOids[ii] == caller.getOid()) {
                // abort!
                return;
            }
        }

        if (idx != -1) {
            _tobj.playerOids[idx] = caller.getOid();
            _tobj.setPlayerOids(_tobj.playerOids);
        }

        // start a "start the game" timer if we're ready to go
        checkStart();
    }

    // documentation inherited from interface TableGameProvider
    public void joinMatchSlot (PlayerObject caller, int slot)
    {
        // make sure the match wasn't cancelled
        if (_tobj.playerOids == null) {
            return;
        }

        // make sure the spot they're trying to take is still available
        if (slot < 0 || slot >= _tobj.playerOids.length || _tobj.playerOids[slot] != 0) {
            return;
        }
        for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
            if (_tobj.playerOids[ii] == caller.getOid()) {
                // abort!
                return;
            }
        }

        _tobj.playerOids[slot] = caller.getOid();
        _tobj.setPlayerOids(_tobj.playerOids);

        // start a "start the game" timer if we're ready to go
        checkStart();
    }

    /**
     * This will be override to allow subclasses to before some operations when the game is
     * about to start.
     */
    public void startingGame (PlaceObject gameobj)
    {
        // nothing doing
    }

    // documentation inherited from interface TableGameProvider
    public void leaveMatch (PlayerObject caller)
    {
        clearPlayer(caller.getOid());
    }

    /**
     * Removes a player from any pending matches.
     */
    public void clearPlayer (int playerOid)
    {
        // make sure the match wasn't already cancelled
        if (_tobj.playerOids == null) {
            return;
        }

        // clear this player out of the list
        boolean cleared = false;
        int remain = 0;
        for (int ii = 0; ii < _tobj.playerOids.length; ii++) {
            if (_tobj.playerOids[ii] == playerOid) {
                _tobj.playerOids[ii] = 0;
                cleared = true;
            } else if (_tobj.playerOids[ii] > 0) {
                remain++;
            }
        }

        // if we didn't find this player in the list, stop here
        if (!cleared) {
            return;
        }

        // either remove this player or cancel the match, depending
        if (remain == 0) {
            _tobj.setPlayerOids(null);
        } else {
            _tobj.setPlayerOids(_tobj.playerOids);
        }

        // cancel our game start timer
        if (_starter != null) {
            _tobj.setStarting(false);
            _starter.cancel();
            _starter = null;
        }
    }

    /**
     * Check if a pending match has satisfied its starting requirements.
     */
    protected void checkStart ()
    {
        if (readyToStart() && _starter == null) {
            _tobj.setStarting(true);
            _starter = new Interval(BangServer.omgr) {
                public void expired () {
                    if (_starter != this) {
                        return;
                    }
                    _starter = null;
                    if (readyToStart()) {
                        log.info("Starting " + _tobj.game + ".");
                        startMatch();
                    }
                }
            };
            _starter.schedule(SaloonManager.START_DELAY);
        }
    }

    protected boolean readyToStart ()
    {
        return (IntListUtil.indexOf(_tobj.playerOids, 0) == -1);
    }

    protected void startMatch ()
    {
        BangConfig config = new BangConfig();

        // we can use these values directly as we sanity checked them earlier
        int players = _tobj.game.getCount(Slot.HUMAN);
        int tinCans = _tobj.game.getCount(Slot.TINCAN);
        config.init(players + tinCans, _tobj.game.teamSize);
        config.players = new Handle[config.plist.size()];
        config.duration = _tobj.game.duration;
        config.speed = _tobj.game.speed;
        config.rated = false; // back parlor games are never rated

        // configure our rounds
        for (int ii = 0; ii < _tobj.game.rounds; ii++) {
            config.addRound(RandomUtil.pickRandom(_tobj.game.scenarios), null, _bdata);
        }

        // fill in the human and ai players
        config.ais = new BangAI[config.players.length];
        HashSet<String> names = new HashSet<String>();
        int pidx = 0, idx = 0;
        for (Slot slot : _tobj.game.slots) {
            switch (slot) {
            case HUMAN:
                PlayerObject user = (PlayerObject)BangServer.omgr.getObject(_tobj.playerOids[pidx]);
                if (user == null) {
                    log.warning("Zoiks! Missing player for table game", "game", _tobj.game,
                                "oid", _tobj.playerOids[pidx]);
                    // clear our now non-existant player from the match
                    clearPlayer(_tobj.playerOids[pidx]);
                    return; // abandon ship
                }
                config.players[idx] = user.handle;
                pidx++;
                idx++;
                break;
            case TINCAN:
                // TODO: sort out personality and skill
                BangAI ai = BangAI.createAI(1, 50, names);
                config.ais[idx] = ai;
                config.players[idx] = ai.handle;
                idx++;
                break;
            default:
                // nothing doing
            }
        }

        // configure teams if necessary
        if (_tobj.game.mode == ParlorGameConfig.Mode.TEAM_2V2) {
            idx = 0;
            for (BangConfig.Player player : config.plist) {
                player.teamIdx = (idx < 2 ? 0 : 1);
                idx++;
            }
        }

        try {
            BangManager mgr = (BangManager)BangServer.plreg.createPlace(config);
            startingGame(mgr.getPlaceObject());
        } catch (Exception e) {
            log.warning("Choked creating game " + config + ".", e);
        }

        // and clear out our parlor bits
        _tobj.setStarting(false);
        _tobj.setPlayerOids(null);
        _tobj.setGame(null);
        _bdata = null;
    }

    protected TableGameObject _tobj;
    protected byte[] _bdata;
    protected Interval _starter;
}
