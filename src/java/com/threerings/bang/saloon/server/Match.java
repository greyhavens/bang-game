//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashSet;

import com.samskivert.util.Interval;
import com.threerings.util.Name;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.server.ServerConfig;

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
        Rating rating = player.getRating(ScenarioInfo.OVERALL_IDENT);
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
        
        // make sure we're not a foe of theirs and none of them one of ours
        for (int i = 0; i < players.length; i ++) {
            if (players[i] != null && (players[i].isFoe(player.playerId) ||
                                       player.isFoe(players[i].playerId))) {
                return false;
            }
        }
        
        // now make sure the joining player satisfies our rating range requirements: the joiner
        // must fall within our desired range of the average rating and the min and max rating must
        // fall within the joiner's criterion-specified range

        // TODO

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

        // recompute our rating info
        _avgRating = 0;
        int count = 0;
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] == null) {
                continue;
            }
            Rating rating = players[ii].getRating(ScenarioInfo.OVERALL_IDENT);
            _minRating = Math.min(_minRating, rating.rating);
            _maxRating = Math.max(_maxRating, rating.rating);
            _avgRating += rating.rating;
            count++;
        }
        _avgRating /= count;

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
        int pcount = Math.min(GameCodes.MAX_PLAYERS, getPlayerCount() + _criterion.getAllowedAIs());
        config.init(pcount, TEAM_SIZES[pcount-2]);
        config.players = new Name[pcount];

        // add our human players
        int idx = 0, humans = 0;
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                config.players[idx++] = players[ii].handle;
                humans++;
            }
        }

        // add our ais (if any)
        config.ais = new BangAI[pcount];
        HashSet<String> names = new HashSet<String>();
        for (int ii = idx; ii < config.ais.length; ii++) {
            // TODO: sort out personality and skill
            BangAI ai = BangAI.createAI(1, 50, names);
            config.ais[ii] = ai;
            config.players[ii] = ai.handle;
        }

        idx = 0;
        String[] lastScenIds = new String[humans];
        for (int ii = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                lastScenIds[idx] = players[ii].lastScenId;
                idx++;
            }
        }

        // only games versus at least one other human are rated
        config.rated = (humans > 1) ? _criterion.getDesiredRankedness() : false;

        // configure our rounds
        for (String scenId : ScenarioInfo.selectRandomIds(
                 ServerConfig.townId, _criterion.getDesiredRounds(), pcount, lastScenIds,
                 _criterion.allowPreviousTowns)) {
            config.addRound(scenId, null, null);
        }

        return config;
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

    protected Criterion _criterion;
    protected Criterion[] _playerCriterions;
    protected int _minRating, _avgRating, _maxRating;

    protected static final long BASE_WAIT = 10000L;
}
