//
// $Id$

package com.threerings.bang.saloon.server;

import com.samskivert.util.Interval;
import com.threerings.util.Name;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.MatchObject;

/**
 * Contains information about a pending match-up.
 */
public class Match
{
    /** Used by {@link #checkReady}. */
    public static enum Readiness { NOT_READY, COULD_START, START_NOW };

    /** Team sizes for two, three and four player games. */
    public static final int[] TEAM_SIZES = { 4, 3, 3 };

    /** The distributed object that we use to communicate to our players. */
    public MatchObject matchobj;

    /** The players involved in this match-up. */
    public PlayerObject[] players;

    /** Used on the server to start the match after a delay. */
    public transient Interval starter;

    /** Creates a new match with the specified player. */
    public Match (PlayerObject player, Criterion criterion)
    {
        players = new PlayerObject[GameCodes.MAX_PLAYERS];
        _playerCriterions = new Criterion[GameCodes.MAX_PLAYERS];
        players[0] = player;
        Rating rating = player.getRating(ScenarioInfo.OVERALL_IDENT, null);
        _minRating = _avgRating = _maxRating = rating.rating;
        _playerCriterions[0] = criterion;
        rebuildCriterion();
    }

    /**
     * Configures this match with its distributed object (called by the saloon manager once the
     * match object is created).
     */
    public void setObject (MatchObject matchobj)
    {
        this.matchobj = matchobj;
        int[] oids = new int[players.length];
        for (int ii = 0; ii < oids.length; ii++) {
            oids[ii] = (players[ii] == null) ? 0 : players[ii].getOid();
        }
        matchobj.setCriterion(_criterion);
        matchobj.setPlayerOids(oids);
    }

    /**
     * Marks this match as starting or not in its match distributed object. If a match is set as
     * not starting, its {@link #starter} interval will be cancelled and cleared out if it exists.
     */
    public void setStarting (boolean starting)
    {
        // because we have intervals flying around all over the place, it is possible that they
        // might decide to mark a match as starting or not after it has been destroyed
        if (matchobj != null && matchobj.isActive()) {
            matchobj.setStarting(starting);
        }

        // cancel our starter interval if we're switching to non-starting state
        if (!starting && starter != null) {
            starter.cancel();
            starter = null;
        }
    }

    /**
     * Called when the match is starting and the BangManager has been created.
     */
    public void startingMatch (PlaceObject gameobj)
    {
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                PlaceManager pmgr = BangServer.plreg.getPlaceManager(players[ii].getPlaceOid());
                if (pmgr instanceof ParlorManager) {
                    ((ParlorManager)pmgr).startingGame(gameobj);
                }
            }
        }
    }

    /**
     * Checks to see if the specified player can be joined into this pending match. If so, all the
     * necessary internal state is updated and we return true, otherwise we return false and the
     * internal state remains unchanged.
     */
    public boolean join (PlayerObject player, Criterion criterion)
    {
        // first make sure the criteron are compatible
        if (!_criterion.isCompatible(criterion)) {
            return false;
        }

        // make sure we can add the player without going over his maximum
        if (getPlayerCount() >= criterion.getDesiredPlayers()) {
            return false;
        }

        // make sure we're not a foe of theirs and none of them one of ours, that we're not
        // somehow already in this match and that gang teams are proper
        int mode = Math.max(criterion.mode, _criterion.mode);
        boolean gang = criterion.gang || _criterion.gang;
        for (PlayerObject oplayer : players) {
            if (oplayer != null) {
                // if we're already in this match, we can just return true at this point
                if (oplayer.playerId == player.playerId) {
                    return true;
                } else if (oplayer.isFoe(player.playerId) || player.isFoe(oplayer.playerId)) {
                    return false;
                } else if (gang) {
                    if (mode == Criterion.COOP && oplayer.gangId != player.gangId) {
                        return false;
                    } else if (mode == Criterion.COMP && oplayer.gangId != 0 && player.gangId != 0
                            && oplayer.gangId == player.gangId) {
                        return false;
                    }
                }
            }
        }

        // We'll use the forest guardians ident now for matching rank in coop games since it's
        // currently the only coop scenario.  When we add another coop scenario, we'll make an
        // overall rating for coop games.
        String ident = mode == Criterion.COOP ?
            ForestGuardiansInfo.IDENT : ScenarioInfo.OVERALL_IDENT;

        // now make sure the joining player satisfies our rating range requirements: the joiner
        // must fall within our desired range of the average rating and the min and max rating must
        // fall within the joiner's criterion-specified range
        Rating prating = player.getRating(ident, null);
        if (criterion.range < Criterion.OPEN) {
            int range = (criterion.range == Criterion.TIGHT ?
                    RuntimeConfig.server.nearRankRange : RuntimeConfig.server.looseRankRange);
            for (int ii = 0; ii < players.length; ii++) {
                if (players[ii] == null) {
                    continue;
                }
                Rating rating = players[ii].getRating(ident, null);
                if (Math.abs(rating.rating - prating.rating) > range) {
                    return false;
                }
            }
        }

        if (_criterion.range < Criterion.OPEN) {
            int range = (_criterion.range == Criterion.TIGHT ?
                    RuntimeConfig.server.nearRankRange : RuntimeConfig.server.looseRankRange);
            if (Math.abs(_avgRating - prating.rating) > range) {
                return false;
            }
        }

        try {
            // add the player and update the criterion in one event
            matchobj.startTransaction();

            // find them a slot
            int added = -1;
            for (int ii = 0; ii < players.length; ii++) {
                if (players[ii] == null) {
                    players[ii] = player;
                    _playerCriterions[ii] = criterion;
                    matchobj.setPlayerOidsAt(player.getOid(), ii);
                    added = ii;
                    break;
                }
            }

            // if we failed to find them a spot, don't let them in
            if (added == -1) {
                return false;
            }

            // we're good to go, so merge this feller on in
            _criterion.merge(criterion);

            // update the criterion now that the player oid is in
            matchobj.setCriterion(_criterion);

        } finally {
            matchobj.commitTransaction();
        }

        recomputeRating();

        return true;
    }

    /**
     * Removes the specified player from the match.
     *
     * @return true if the player was located and removed, false if they were not participating in
     * this match.
     */
    public boolean remove (int playerOid)
    {
        for (int ii = 0; ii < matchobj.playerOids.length; ii++) {
            int poid = matchobj.playerOids[ii];
            if (poid == playerOid) {
                players[ii] = null;
                _playerCriterions[ii] = null;
                try {
                    matchobj.startTransaction();
                    matchobj.setPlayerOidsAt(0, ii);
                    rebuildCriterion(); // recreate the criterion now that this player is gone
                    matchobj.setCriterion(_criterion);
                } finally {
                    matchobj.commitTransaction();
                }
                recomputeRating();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the count of human players involved in this match.
     */
    public int getPlayerCount ()
    {
        int count = 0;
        for (int ii = 0; ii < matchobj.playerOids.length; ii++) {
            if (matchobj.playerOids[ii] > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the readiness state of this match.
     */
    public Readiness checkReady ()
    {
        verifyPlayers();
        int count = getPlayerCount();
        if (matchobj == null || !matchobj.isActive()) {
            // if our match object has gone away; we will never again be ready
            return Readiness.NOT_READY;
        } else if (_criterion == null) {
            return Readiness.NOT_READY;
        } else if (count == _criterion.getDesiredPlayers()) {
            return Readiness.START_NOW;
        } else if (_criterion.couldStart(count)) {
            return Readiness.COULD_START;
        } else {
            return Readiness.NOT_READY;
        }
    }

    /**
     * Returns the duration we should wait for more opponents to arrive before we go ahead and
     * start the game with whoever is already here.
     */
    public long getWaitForOpponentsDelay ()
    {
        // if there are at least two players, just wait ten seconds; otherwise wait ten seconds for
        // every empty seat
        int humans = getPlayerCount();
        return ((humans > 1) ? 1 : _criterion.getDesiredPlayers() - humans) * BASE_WAIT;
    }

    /**
     * Creates a game configuration based on our match information.
     */
    public BangConfig createConfig ()
    {
        BangConfig config = new BangConfig();
        int pcount = Math.min(GameCodes.MAX_PLAYERS, getPlayerCount());
        config.init(pcount, TEAM_SIZES[pcount-2]);
        config.players = new Name[pcount];

        // add our human players
        int idx = 0;
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                config.players[idx++] = players[ii].handle;
            }
        }

        config.ais = new BangAI[pcount];

        idx = 0;
        String[] lastScenIds = new String[pcount];
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                lastScenIds[idx] = players[ii].lastScenId;
                idx++;
            }
        }

        // all matched games are rated
        config.rated = true;

        // configure our rounds
        String[] scenIds = ScenarioInfo.selectRandomIds(
                 ServerConfig.townId, _criterion.getDesiredRounds(), pcount, lastScenIds,
                 _criterion.allowPreviousTowns, _criterion.mode);
        for (String scenId : scenIds) {
            config.addRound(scenId, null, null);
        }

        config.grantAces = _criterion.gang || isGangCompatible(scenIds);

        return config;
    }

    /**
     * Checks that this match is a valid gang match.
     */
    protected boolean isGangCompatible (String[] scenIds)
    {
        int mode = _criterion.mode;
        // make sure all the scenarios are either competitive or cooperative
        if (mode == Criterion.ANY) {
            for (String scenId : scenIds) {
                ScenarioInfo info = ScenarioInfo.getScenarioInfo(scenId);
                if (info.getTeams() == ScenarioInfo.Teams.COOP) {
                    if (mode != Criterion.ANY && mode != Criterion.COOP) {
                        return false;
                    }
                    mode = Criterion.COOP;
                } else {
                    if (mode != Criterion.ANY && mode != Criterion.COMP) {
                        return false;
                    }
                    mode = Criterion.COMP;
                }
            }
        }

        // now check that the players are in all different gangs for competitive games or are all
        // on the same gang for cooperative games
        switch (mode) {
        case Criterion.COMP:
            for (PlayerObject player1 : players) {
                if (player1 == null || player1.gangId == 0) {
                    continue;
                }
                for (PlayerObject player2 : players) {
                    if (player2 == null || player2.gangId == 0) {
                        continue;
                    }
                    if (player1.playerId != player2.playerId && player1.gangId == player2.gangId) {
                        return false;
                    }
                }
            }
            return true;
        case Criterion.COOP:
            int gangId = -1;
            for (PlayerObject player : players) {
                if (player == null) {
                    continue;
                }
                if (gangId < 0) {
                    gangId = player.gangId;
                } else if (gangId != player.gangId) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Rebuilds the criterion based on the stored player preferences.
     */
    protected void rebuildCriterion ()
    {
        Criterion oldCriterion = _criterion;
        _criterion = null;
        for (int ii = 0; ii < _playerCriterions.length; ii++) {
            if (_playerCriterions[ii] != null) {
                if (_criterion == null) {
                    _criterion = (Criterion)_playerCriterions[ii].clone();
                } else {
                    _criterion.merge(_playerCriterions[ii]);
                }
            }
        }
        if (_criterion == null) {
            _criterion = oldCriterion;
        }
    }

    /**
     * Recomputes the average rating for this match.
     */
    protected void recomputeRating ()
    {
        // recompute our rating info
        _avgRating = 0;
        int count = 0;
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] == null) {
                continue;
            }
            Rating rating = players[ii].getRating(ScenarioInfo.OVERALL_IDENT, null);
            _minRating = Math.min(_minRating, rating.rating);
            _maxRating = Math.max(_maxRating, rating.rating);
            _avgRating += rating.rating;
            count++;
        }
        if (count > 0) {
            _avgRating /= count;
        }
    }

    /**
     * Verifies that all match players are still present, removes those that aren't.
     */
    public void verifyPlayers ()
    {
        for (int poid : matchobj.playerOids) {
            if (poid <= 0) {
                continue;
            }
            PlayerObject user = (PlayerObject)BangServer.omgr.getObject(poid);
            // make sure the player is still online and not in a game
            if (user == null ||
                    (BangServer.omgr.getObject(user.getPlaceOid()) instanceof BangObject)) {
                remove(poid);
            }
        }
    }

    protected Criterion _criterion;
    protected Criterion[] _playerCriterions;
    protected int _minRating, _avgRating, _maxRating;

    protected static final long BASE_WAIT = 10000L;
}
