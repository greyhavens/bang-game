//
// $Id$

package com.threerings.bang.game.data;

import com.samskivert.util.ListUtil;

import com.threerings.crowd.client.PlaceController;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.bang.game.client.BangController;

/**
 * Used to configure a Bang! game.
 */
public class BangConfig extends GameConfig
{
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

    /** The number of people playing the game. */
    public int seats = 2;

    /** The base size of each player's team (not including their Big Shot; may
     * be modified by scenario). */
    public int teamSize = 4;

    /** Whether or not to play a quick, normal or long game. */
    public Duration duration = Duration.NORMAL;

    /** The desired scenarios for each round (implies the number of rounds). */
    public String[] scenarios;

    /** If true, the scenario will be interpreted as a tutorial identifier and
     * a tutorial game will be created. */
    public boolean tutorial = false;

    /** If true, the scenario will be interpreted as a practice sessions and
     * a practice game will be created. */
    public boolean practice = false;

    /** Specifies the exact name of the board to be used instead of choosing
     * randomly. This is used when testing. */
    public String board;

    /** Used when testing with a specific board. */
    public byte[] bdata;

    /** The previously played boards for each player. */
    public int[] lastBoardIds;

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
