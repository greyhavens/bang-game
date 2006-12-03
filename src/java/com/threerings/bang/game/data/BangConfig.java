//
// $Id$

package com.threerings.bang.game.data;

import java.util.ArrayList;

import com.samskivert.util.ListUtil;

import com.threerings.crowd.client.PlaceController;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.bang.game.client.BangController;

/**
 * Used to configure a Bang! game.
 */
public class BangConfig extends GameConfig
{
    /** Represents a particular player's team configuration. */
    public static class Player extends SimpleStreamableObject
    {
        /** The Big Shot unit to be used by this player (or null if they won't use one). */
        public String bigShot;

        /** The units be used by this player. */
        public String[] team;
    }

    /** Indicates the type of game being played. */
    public static enum Type {
        TUTORIAL, PRACTICE, SALOON, BOUNTY
    };

    /** Used to adjust the duration of the rounds. */
    public static enum Duration {
        /** Used for practice tutorials. Super ultra short. */
        PRACTICE(0.5f),

        /** 70% of the duration of a normal game. */
        QUICK(0.7f),

        /** The standard duration. */
        NORMAL(1f),

        /** 130% of the duration of a normal game. */
        LONG(1.3f);

        public float getAdjustment () {
            return _adjustment;
        }

        Duration (float adjustment) {
            _adjustment = adjustment;
        }

        protected float _adjustment;
    };

    /** Used to adjust the speed of the ticks. */
    public static enum Speed {
        /** 70% of the inter-tick time of a normal game. */
        FAST(0.7f),

        /** The standard speed. */
        NORMAL(1f),

        /** 150% of the inter-tick time of a normal game. */
        SLOW(1.5f);

        public float getAdjustment () {
            return _adjustment;
        }

        Speed (float adjustment) {
            _adjustment = adjustment;
        }

        protected float _adjustment;
    };

    /** The configuration of each player's team in the game. */
    public ArrayList<Player> teams = new ArrayList<Player>();

    /** Whether or not to play a quick, normal or long game. */
    public Duration duration = Duration.NORMAL;

    /** Whether or not to play a fast, normal or slow game. */
    public Speed speed = Speed.NORMAL;

    /** The desired scenarios for each round (implies the number of rounds). */
    public String[] scenarios;

    /** The type of game being played. */
    public Type type = Type.SALOON;

    /** Specifies the exact name of the board to be used instead of choosing randomly. */
    public String board;

    /** Used when testing with a specific board. */
    public byte[] bdata;

    /** The previously played boards for each player. */
    public int[] lastBoardIds = new int[0];

    /** Any additional criterion to be met in addition to winning the game (currently used only for
     * bounty games). */
    public ArrayList<Criterion> criterion = new ArrayList<Criterion>();

    /**
     * Used to configure a number of blank seats with the specified team size for non-preconfigured
     * games.
     */
    public void init (int players, int teamSize)
    {
        teams.clear();
        for (int ii = 0; ii < players; ii++) {
            Player player = new Player();
            player.team = new String[teamSize];
            teams.add(player);
        }
    }

    /**
     * Returns the team size for the specified player.
     */
    public int getTeamSize (int pidx)
    {
        return teams.get(pidx).team.length;
    }

    /** Returns the desired number of rounds. */
    public int getRounds ()
    {
        return scenarios.length;
    }

    /** Determines whether all players are AIs. */
    public boolean allPlayersAIs ()
    {
        return (ais != null && ListUtil.indexOfNull(ais) == -1);
    }

    /** Helper for {@link #toString}. */
    public String bdataToString ()
    {
        return (bdata == null) ? "<none>" : (bdata.length + " bytes");
    }

    @Override // documentation inherited
    public String getBundleName ()
    {
        return "bang";
    }

    @Override // documentation inherited
    public GameConfigurator createConfigurator ()
    {
        return null;
    }

    @Override // documentation inherited
    public PlaceController createController ()
    {
        return new BangController();
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.game.server.BangManager";
    }
}
