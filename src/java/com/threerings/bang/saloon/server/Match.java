//
// $Id$

package com.threerings.bang.saloon.server;

import com.samskivert.util.Interval;
import com.threerings.util.Name;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.util.ScenarioUtil;
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
    public static final int[] TEAM_SIZES = { 5, 4, 3 };

    /** The distributed object that we use to communicate to our players. */
    public MatchObject matchobj;

    /** The players involved in this match-up. */
    public PlayerObject[] players;

    /** Used on the server to start the match after a delay. */
    public transient Interval starter;

    /** Creates a new match with the specified player. */
    public Match (PlayerObject player, Criterion criterion)
    {
        players = new PlayerObject[criterion.getDesiredPlayers()];
        players[0] = player;
        Rating rating = player.getRating(ScenarioCodes.OVERALL);
        _minRating = _avgRating = _maxRating = rating.rating;
        _criterion = criterion;
    }

    /**
     * Configures this match with its distributed object (called by the saloon
     * manager once the match object is created).
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
     * Checks to see if the specified player can be joined into this
     * pending match. If so, all the necessary internal state is updated
     * and we return true, otherwise we return false and the internal state
     * remains unchanged.
     */
    public boolean join (PlayerObject player, Criterion criterion)
    {
        // first make sure the criteron are compatible
        if (!_criterion.isCompatible(criterion)) {
            return false;
        }

        // now make sure the joining player satisfies our rating range
        // requirements: the joiner must fall within our desired range of
        // the average rating and the min and max rating must fall within
        // the joiner's criterion-specified range

        // TODO

        // we're good to go, so merge this feller on in
        _criterion.merge(criterion);

        try {
            // add the player and update the criterion in one event
            matchobj.startTransaction();

            // find them a slot
            for (int ii = 0; ii < players.length; ii++) {
                if (players[ii] == null) {
                    players[ii] = player;
                    matchobj.setPlayerOidsAt(player.getOid(), ii);
                    break;
                }
            }

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
            Rating rating = players[ii].getRating(ScenarioCodes.OVERALL);
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
     * @return true if the player was located and removed, false if they were
     * not participating in this match.
     */
    public boolean remove (int playerOid)
    {
        for (int ii = 0; ii < matchobj.playerOids.length; ii++) {
            int poid = matchobj.playerOids[ii];
            if (poid == playerOid) {
                players[ii] = null;
                matchobj.setPlayerOidsAt(0, ii);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the count of players involved in this match.
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
        if (count == _criterion.getDesiredPlayers()) {
            return Readiness.START_NOW;
        } else if (_criterion.isValidPlayerCount(count)) {
            return Readiness.COULD_START;
        } else {
            return Readiness.NOT_READY;
        }
    }

    /**
     * Creates a game configuration based on our match information.
     */
    public BangConfig createConfig ()
    {
        BangConfig config = new BangConfig();
        config.seats = getPlayerCount();
        config.players = new Name[config.seats];
        config.teamSize = TEAM_SIZES[config.seats-2];
        for (int ii = 0, idx = 0; ii < players.length; ii++) {
            if (players[ii] != null) {
                config.players[idx++] = players[ii].handle;
            }
        }
        config.rated = _criterion.getDesiredRankedness();
        config.scenarios = ScenarioUtil.selectRandom(
            ServerConfig.getTownId(), _criterion.getDesiredRounds());;
        return config;
    }

    protected Criterion _criterion;
    protected int _minRating, _avgRating, _maxRating;
}
