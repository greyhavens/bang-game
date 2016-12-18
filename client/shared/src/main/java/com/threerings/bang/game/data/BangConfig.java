//
// $Id$

package com.threerings.bang.game.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;

import com.threerings.crowd.client.PlaceController;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.bang.game.client.BangController;

/**
 * Used to configure a Bang! game.
 */
public class BangConfig extends GameConfig
    implements Savable
{
    /** Indicates the type of game being played. */
    public static enum Type {
        TUTORIAL, PRACTICE, SALOON, BOUNTY
    };

    /** Used to adjust the duration of the rounds. */
    public static enum Duration {
        /** Used for practice tutorials. Super ultra short. */
        PRACTICE(0.5f, "m.dur_practice"),

        /** 70% of the duration of a normal game. */
        QUICK(0.7f, "m.dur_quick"),

        /** The standard duration. */
        NORMAL(1f, "m.dur_normal"),

        /** 130% of the duration of a normal game. */
        LONG(1.3f, "m.dur_long");

        public float getAdjustment () {
            return _adjustment;
        }

        public String key () {
            return _xlate;
        }

        Duration (float adjustment, String xlate) {
            _adjustment = adjustment;
            _xlate = xlate;
        }

        protected float _adjustment;
        protected String _xlate;
    };

    /** Used to adjust the speed of the ticks. */
    public static enum Speed {
        /** 70% of the inter-tick time of a normal game. */
        FAST(0.7f, "m.sp_fast"),

        /** The standard speed. */
        NORMAL(1f, "m.sp_normal"),

        /** 150% of the inter-tick time of a normal game. */
        SLOW(1.5f, "m.sp_slow");

        public float getAdjustment () {
            return _adjustment;
        }

        public String key () {
            return _xlate;
        }

        Speed (float adjustment, String xlate) {
            _adjustment = adjustment;
            _xlate = xlate;
        }

        protected float _adjustment;
        protected String _xlate;
    };

    /** Represents a particular player's configuration. */
    public static class Player extends SimpleStreamableObject
        implements Savable
    {
        /** The Big Shot unit to be used by this player (or null if they won't use one). */
        public String bigShot;

        /** The units be used by this player. */
        public String[] units;

        /** The cards to be used by this player. */
        public String[] cards;

        /** The index of the starting spot for this player or -1 for the default. */
        public int startSpot = -1;

        /** The team this player is on or -1 if they are on their own. */
        public int teamIdx = -1;

        /** If this player is an AI, the skill level to use for that AI. We configure this here and
         * then use it to populate {@link BangConfig#ais} when the game is being created. */
        public int skill = 50;

        // from interface Savable
        public void write (JMEExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(bigShot, "bigShot", null);
            out.write(units, "team", null); // old name to avoid breakage
            out.write(cards, "cards", null);
            out.write(startSpot, "startSpot", -1);
            out.write(teamIdx, "teamIdx", -1);
            out.write(skill, "skill", 50);
        }

        // from interface Savable
        public void read (JMEImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            bigShot = in.readString("bigShot", null);
            units = in.readStringArray("team", null); // old name to avoid breakage
            cards = in.readStringArray("cards", null);
            startSpot = in.readInt("startSpot", -1);
            teamIdx = in.readInt("teamIdx", -1);
            skill = in.readInt("skill", 50);
        }

        // from interface Savable
        public Class<?> getClassTag () {
            return getClass();
        }

        @Override // from Object
        public boolean equals (Object other)
        {
            Player oplayer = (Player)other;
            return ObjectUtil.equals(bigShot, oplayer.bigShot) &&
                Arrays.equals(units, oplayer.units) && Arrays.equals(cards, oplayer.cards);
        }
    }

    /**
     * Represents the configuration of a particular round.
     */
    public static class Round extends SimpleStreamableObject
        implements Savable
    {
        /** The scenario to be played during this round. */
        public String scenario;

        /** The name of the board to use for this round or null. */
        public String board;

        /** A serialized board to use for this round or null. */
        public byte[] bdata;

        /** Additional scenario specific configuration information (used in bouty games). */
        public String sdata;

        /** Helper for {@link #toString}. */
        public String bdataToString ()
        {
            return (bdata == null) ? "<none>" : (bdata.length + " bytes");
        }

        // from interface Savable
        public void write (JMEExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(scenario, "scenario", null);
            out.write(board, "board", null);
            out.write(sdata, "sdata", null);
            // bdata is not serialized
        }

        // from interface Savable
        public void read (JMEImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            scenario = in.readString("scenario", null);
            board = in.readString("board", null);
            sdata = in.readString("sdata", null);
            // bdata is not serialized
        }

        // from interface Savable
        public Class<?> getClassTag () {
            return getClass();
        }

        @Override // from Object
        public boolean equals (Object other)
        {
            Round oround = (Round)other;
            return ObjectUtil.equals(scenario, oround.scenario) &&
                ObjectUtil.equals(sdata, oround.sdata) && ObjectUtil.equals(board, oround.board) &&
                Arrays.equals(bdata, oround.bdata);
        }
    }

    /** The type of game being played. */
    public Type type = Type.SALOON;

    /** Whether or not to play a quick, normal or long game. */
    public Duration duration = Duration.NORMAL;

    /** Whether or not to play a fast, normal or slow game. */
    public Speed speed = Speed.NORMAL;

    /** Indicates the minimum weight of cards and bonuses that will be spawned in this game. */
    public int minWeight = 0;

    /** Whether units respawn in this game. */
    public boolean respawnUnits = true;

    /** Whether or not to grant aces to the players' gangs for this game. */
    public boolean grantAces;

    /** The configuration of each round of the game. */
    public List<Round> rounds = new ArrayList<Round>();

    /** The configuration for each player in the game. */
    public List<Player> plist = new ArrayList<Player>();

    /** Additional criteria to be met in addition to winning the game (used in bounty games). */
    public List<Criterion> criteria = new ArrayList<Criterion>();

    /**
     * Used to configure the number of players and team size for non-preconfigured games.
     */
    public void init (int players, int teamSize)
    {
        plist.clear();
        for (int ii = 0; ii < players; ii++) {
            Player player = new Player();
            player.units = new String[teamSize];
            plist.add(player);
        }
    }

    /**
     * Returns the team size for the specified player.
     */
    public int getTeamSize (int pidx)
    {
        return plist.get(pidx).units.length;
    }

    /**
     * Returns the desired number of rounds.
     */
    public int getRounds ()
    {
        return rounds.size();
    }

    /**
     * Adds a player to this game config.
     */
    public void addPlayer (String bigShot, String[] units)
    {
        addPlayer(bigShot, units, null, -1);
    }

    /**
     * Adds a player to this game config.
     */
    public void addPlayer (String bigShot, String[] units, String[] cards, int startSpot)
    {
        Player player = new Player();
        player.bigShot = bigShot;
        player.units = units;
        player.cards = cards;
        player.startSpot = startSpot;
        plist.add(player);
    }

    /**
     * Adds a round to this game config.
     */
    public void addRound (String scenario, String board, byte[] bdata)
    {
        addRound(scenario, board, bdata, null);
    }

    /**
     * Adds a round to this game config.
     */
    public void addRound (String scenario, String board, byte[] bdata, String sdata)
    {
        Round round = new Round();
        round.scenario = scenario;
        round.sdata = sdata;
        round.board = board;
        round.bdata = bdata;
        rounds.add(round);
    }

    /**
     * Returns the scenario to be used for the specified round.
     */
    public String getScenario (int ridx)
    {
        return rounds.get(ridx).scenario;
    }

    /**
     * Returns the scenario data to be used for the specified round.
     */
    public String getScenarioData (int ridx)
    {
        return rounds.get(ridx).sdata;
    }

    /**
     * Determines whether all players are AIs.
     */
    public boolean allPlayersAIs ()
    {
        return (ais != null && ListUtil.indexOfNull(ais) == -1);
    }

    // from interface Savable
    public void write (JMEExporter ex) throws IOException
    {
        OutputCapsule out = ex.getCapsule(this);
        out.write(rated, "rated", false);
        out.write(type.toString(), "type", Type.BOUNTY.toString());
        out.write(duration.toString(), "duration", Duration.NORMAL.toString());
        out.write(speed.toString(), "speed", Speed.NORMAL.toString());
        out.write(minWeight, "minWeight", 0);
        out.write(respawnUnits, "respawnUnits", true);
        out.writeSavableArrayList(rounds, "rounds", DEF_ROUNDS);
        out.writeSavableArrayList(plist, "teams", DEF_PLAYERS); // old name to avoid breakage
        out.writeSavableArrayList(criteria, "criteria", DEF_CRIT);
    }

    // from interface Savable
    public void read (JMEImporter im) throws IOException
    {
        InputCapsule in = im.getCapsule(this);
        rated = in.readBoolean("rated", false);
        type = Type.valueOf(in.readString("type", Type.BOUNTY.toString()));
        duration = Duration.valueOf(in.readString("duration", Duration.NORMAL.toString()));
        speed = Speed.valueOf(in.readString("speed", Speed.NORMAL.toString()));
        minWeight = in.readInt("minWeight", 0);
        respawnUnits = in.readBoolean("respawnUnits", true);
        rounds = in.readSavableArrayList("rounds", DEF_ROUNDS);
        plist = in.readSavableArrayList("teams", DEF_PLAYERS); // old name to avoid breakage
        criteria = in.readSavableArrayList("criteria", DEF_CRIT);
        if (criteria == DEF_CRIT) { // legacy fixy
            criteria = in.readSavableArrayList("criterion", DEF_CRIT);
        }
    }

    // from interface Savable
    public Class<?> getClassTag ()
    {
        return getClass();
    }

    @Override // from GameConfig
    public int getGameId ()
    {
        return 1; // one game to rule them all
    }

    @Override // from GameConfig
    public String getGameIdent ()
    {
        return "bang";
    }

    @Override // from GameConfig
    public GameConfigurator createConfigurator ()
    {
        return null;
    }

    @Override // from GameConfig
    public PlaceController createController ()
    {
        return new BangController();
    }

    @Override // from GameConfig
    public String getManagerClassName ()
    {
        return "com.threerings.bang.game.server.BangManager";
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }
        BangConfig oconfig = (BangConfig)other;
        return type == oconfig.type && duration == oconfig.duration && speed == oconfig.speed &&
            rounds.equals(oconfig.rounds) && plist.equals(oconfig.plist) &&
            criteria.equals(oconfig.criteria);
    }

    protected static final ArrayList<Round> DEF_ROUNDS = new ArrayList<Round>();
    protected static final ArrayList<Player> DEF_PLAYERS = new ArrayList<Player>();
    protected static final ArrayList<Criterion> DEF_CRIT = new ArrayList<Criterion>();
}
