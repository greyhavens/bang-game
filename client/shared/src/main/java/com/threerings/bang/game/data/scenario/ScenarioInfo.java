//
// $Id$

package com.threerings.bang.game.data.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;
import com.threerings.io.Streamable;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.client.ScenarioHUD;
import com.threerings.bang.game.client.StatsView;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

/**
 * Contains metadata about a particular game scenario.
 */
public abstract class ScenarioInfo
    implements Streamable, Comparable<ScenarioInfo>
{
    /** The code for the "overall" scenario (only used for ratings).  */
    public static final String OVERALL_IDENT = "oa";

    /** The different team configurations. */
    public enum Teams { INDIVIDUAL, COOP, TEAM2V2, TEAM3V1 };

    /**
     * Returns the set of scenarios available in the specified town. Feel free to bend, fold or
     * mutilate the returned list.
     *
     * @param includePrior if true all scenarios up to and including the specified town will be
     * returned, if false only scenarios introduced in the specified town will be returned.
     */
    public static ArrayList<ScenarioInfo> getScenarios (
        String townId, boolean includePrior)
    {
        ArrayList<ScenarioInfo> scens = new ArrayList<ScenarioInfo>();
        int ttidx = BangUtil.getTownIndex(townId);
        for (ScenarioInfo info : _scenarios.values()) {
            if (includePrior) {
                if (info.getTownIndex() <= ttidx) {
                    scens.add(info);
                }
            } else {
                if (townId.equals(info.getTownId())) {
                    scens.add(info);
                }
            }
        }
        Collections.sort(scens);
        return scens;
    }

    /**
     * Returns true if this player has played all of the scenarios in this town at least once (in a
     * rated game).
     */
    public static boolean hasPlayedAllTownScenarios (PlayerObject user)
    {
        for (ScenarioInfo info : _scenarios.values()) {
            if (info.getTownId().equals(user.townId) &&
                user.getRating(info.getIdent(), null).experience == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns an array containing all registered scenario ids.
     */
    public static String[] getScenarioIds ()
    {
        return _scenIds.toArray(new String[_scenIds.size()]);
    }

    /**
     * Returns an array of all scenario ids available in the supplied town.
     *
     * @param includePrior if true all scenarios up to and including the specified town will be
     * returned, if false only scenarios introduced in the specified town will be returned.
     */
    public static String[] getScenarioIds (String townId, boolean includePrior)
    {
        ArrayList<ScenarioInfo> scens = getScenarios(townId, includePrior);
        String[] scids = new String[scens.size()];
        for (int ii = 0; ii < scids.length; ii++) {
            scids[ii] = scens.get(ii).getIdent();
        }
        return scids;
    }

    /**
     * Selects a random set of scenario ids matching the specified criterion.
     *
     * @param includePrior if true scenarios for towns previous to the supplied town will be
     * included in the selection.
     * @param prevScids a list of scenario ids the players have played recently.  Used to weight
     * the likelyhood of a scenario being chosen.
     * @param mode the game mode specified in Criterion
     */
    public static String[] selectRandomIds (String townId, int count,
            int players, String[] prevScids, boolean includePrior, int mode)
    {
        ArrayList<ScenarioInfo> scens = getScenarios(townId, includePrior);

        // prune out scenarios that don't support the specified player count or mode
        for (Iterator<ScenarioInfo> iter = scens.iterator(); iter.hasNext(); ) {
            ScenarioInfo info = iter.next();
            switch (info.getTeams()) {
            case INDIVIDUAL:
                if (mode == Criterion.COOP) {
                    iter.remove();
                    continue;
                }
                break;
            case COOP:
                if (mode == Criterion.COMP || mode == Criterion.TEAM_2V2) {
                    iter.remove();
                    continue;
                }
                break;
            default:
                break; // nada
            }
            if (!info.supportsPlayers(players)) {
                iter.remove();
            }
        }

        // generate the scenid weights based on the prevScids
        int[] weights = new int[scens.size()];
        String[] validIds = new String[scens.size()];
        for (int ii = 0, nn = scens.size(); ii < nn; ii++) {
            validIds[ii] = scens.get(ii).getIdent();
        }
        if (prevScids != null) {
            for (String prevId : prevScids) {
                for (int ii = 0; ii < validIds.length; ii++) {
                    if (!validIds[ii].equals(prevId)) {
                        weights[ii]++;
                    }
                }
            }
        }
        if (IntListUtil.sum(weights) == 0) {
            Arrays.fill(weights, 1);
        }

        // now select randomly from the remainder
        String[] scids = new String[count];
        for (int ii = 0; ii < count; ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            scids[ii] = validIds[idx];
            for (int jj = 0; jj < weights.length; jj++) {
                if (jj != idx) {
                    weights[jj] += players;
                }
            }
        }
        return scids;
    }

    /**
     * Looks up the scenario info for the specified id.
     */
    public static ScenarioInfo getScenarioInfo (String scenarioId)
    {
        return _scenarios.get(scenarioId);
    }

    /**
     * Returns the string identifier for this scenario.
     */
    public abstract String getIdent ();

    /**
     * Returns the id of the town in which this scenario is available.
     */
    public abstract String getTownId ();

    /**
     * Returns the name of the background music for this scenario.
     */
    public String getMusic ()
    {
        return getTownId() + "/scenario_" + getIdent();
    }

    /**
     * Returns the index of the town in which this scenario is introduced.
     */
    public int getTownIndex ()
    {
        if (_townIndex == -1) {
            _townIndex = BangUtil.getTownIndex(getTownId());
        }
        return _townIndex;
    }

    /**
     * Returns the name of the class that handles things on the server.
     */
    public String getScenarioClass ()
    {
        String name = getClass().getName();
        name = name.substring(name.lastIndexOf(".")+1);
        name = "com.threerings.bang.game.server.scenario." + name;
        return name.substring(0, name.length()-4);
    }

    /**
     * Returns a translatable string identifying this scenario.
     */
    public String getName ()
    {
        return "m.scenario_" + getIdent();
    }

    /**
     * Returns true if the scenario supports the specified player count.
     */
    public boolean supportsPlayers (int playerCount)
    {
        return true;
    }

    /**
     * Returns the number of units (aside from the big shot) that players will use, taking into
     * account the desired size in the configuration.
     */
    public int getTeamSize (BangConfig config, int pidx)
    {
        return config.getTeamSize(pidx);
    }

    /**
     * Returns true if we should show the players and round during the pre-game marquee, false
     * otherwise.
     */
    public boolean showDetailedMarquee ()
    {
        return true;
    }

    /**
     * Returns the stats associated with the scenario's primary objective.
     */
    public abstract StatType[] getObjectives ();

    /**
     * Returns for each objective the number of points earned for each time the objective is
     * reached.
     */
    public abstract int[] getPointsPerObjectives ();

    /**
     * Returns a code used in translation keys to describe the primary objective.
     */
    public String getObjectiveCode ()
    {
        return StringUtil.toUSLowerCase(getObjectives()[0].toString());
    }

    /**
     * Returns the stat associated with our secondary objective or null if this scenario has no
     * secondary objective.
     */
    public StatType getSecondaryObjective ()
    {
        return null;
    }

    /**
     * Returns true if the supplied marker is valid for this type of scenario.
     */
    public boolean isValidMarker (Marker marker)
    {
        switch (marker.getType()) {
        case Marker.START:
        case Marker.BONUS:
        case Marker.IMPASS:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true if the shooter at the supplied location should be able to shoot their target.
     */
    public boolean validShot (Unit shooter, PointSet moves, Piece target)
    {
        return true;
    }

    /**
     * Returns true if the scenario requires units to hold certain bonuses.
     */
    public boolean hasHoldableBonuses ()
    {
        return true;
    }

    /**
     * Returns true if the scenario has enemies that are of human make. Meaning either it is not
     * coop or it is coop but versus human units.
     */
    public boolean hasEnemies (UnitConfig.Make make)
    {
        return true;
    }

    /**
     * Returns the team id for the supplied owner.
     */
    public int getTeam (int owner, int assignedTeam)
    {
        // if no assigned team then just use the owner
        return (assignedTeam == -1 ? owner : assignedTeam);
    }

    /**
     * Given a piece and its set of potential moves, determines which of those moves help achieve
     * the scenario's goals.
     */
    public void getMovementGoals (BangObject bangobj, Piece mover, PointSet moves, PointSet goals)
    {
        for (Piece piece : bangobj.pieces) {
            int radius = getGoalRadius(bangobj, mover, piece);
            if (radius < 0) {
                continue;
            }
            if (moves.contains(piece.x, piece.y)) {
                goals.add(piece.x, piece.y);
            }
            if (radius < 1) {
                continue;
            }
            boolean fblock = piece.getFenceBlocksGoal();
            for (int dir : Piece.DIRECTIONS) {
                int x = piece.x + Piece.DX[dir], y = piece.y + Piece.DY[dir];
                if (moves.contains(x, y) &&
                    (!fblock || bangobj.board.canCross(x, y, piece.x, piece.y))) {
                    goals.add(x, y);
                }
            }
        }
    }

    /**
     * Determines whether the specified moving piece will help achieve the scenario's goals by
     * moving onto or next to the specified target.
     *
     * @return -1 for no relevance, 0 if the mover scores by landing on the target, or +1 if the
     * mover scores by landing next to the target
     */
    protected int getGoalRadius (BangObject bangobj, Piece mover, Piece target)
    {
        return target.getGoalRadius(bangobj, mover);
    }

    /**
     * Returns the path to sound clips that should be preloaded when playing this scenario.
     */
    public String[] getPreLoadClips ()
    {
        return PRELOAD_CLIPS;
    }

    /**
     * Returns the StatsView used by the client to display the post-game scenario stats.
     */
    public StatsView getStatsView (BasicContext ctx)
    {
        return new StatsView(ctx, false);
    }

    /**
     * Returns a HUD element used by the client to display in-game information.
     */
    public ScenarioHUD getHUD (BangContext ctx, BangObject bangobj)
    {
        return null;
    }

    /**
     * Returns true if this is an evenly matched player versus player (competitive) game rather
     * than one where players cooperate or play in some imbalanced arrangement. There are certain
     * stats that we only track for the evenly matched case.
     */
    public boolean isCompetitive ()
    {
        return getTeams() == Teams.INDIVIDUAL; // TODO: TEAM2V2?
    }

    /**
     * Returns the type of teaming in this scenario.
     */
    public Teams getTeams ()
    {
        return Teams.INDIVIDUAL;
    }

    /**
     * Returns a string representation of this instance.
     */
    public String toString ()
    {
        return getIdent();
    }

    // inherited from Comparable<ScenarioInfo>
    public int compareTo (ScenarioInfo oinfo)
    {
        int tidx = getTownIndex(), otidx = oinfo.getTownIndex();
        return (tidx != otidx) ? tidx - otidx : getIdent().compareTo(oinfo.getIdent());
    }

    /**
     * Registers a scenario info record in our table.
     */
    protected static void register (ScenarioInfo info)
    {
        _scenarios.put(info.getIdent(), info);
        _scenIds.add(info.getIdent());
    }

    /** Used to cache our town index so we don't have to look it up all the damned time. Yay for
     * premature optimization. */
    protected transient int _townIndex = -1;

    /** Maps scenario ids to scenario info instances. */
    protected static HashMap<String,ScenarioInfo> _scenarios = new HashMap<String,ScenarioInfo>();

    /** An ordered list of scneario ids. */
    protected static ArrayList<String> _scenIds = new ArrayList<String>();

    /** The default set of clips to preload: none. */
    protected static final String[] PRELOAD_CLIPS = {};

    static {
        // frontier town scenarios
        register(new LandGrabInfo());
        register(new CattleRustlingInfo());
        register(new ClaimJumpingInfo());
        register(new GoldRushInfo());

        // indian trading post scenarios
        register(new WendigoAttackInfo());
        register(new TotemBuildingInfo());
        register(new ForestGuardiansInfo());
        register(new HeroBuildingInfo());

        // boom town scenarios
        // TBD

        // ghost town scenarios
        // TBD

        // city of gold scenarios
        // TBD
    }
}
