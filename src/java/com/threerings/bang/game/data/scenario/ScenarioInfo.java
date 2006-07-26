//
// $Id$

package com.threerings.bang.game.data.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.RandomUtil;
import com.threerings.io.Streamable;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.util.BangUtil;

/**
 * Contains metadata about a particular game scenario.
 */
public abstract class ScenarioInfo
    implements Streamable, Comparable<ScenarioInfo>
{
    /** The code for the "overall" scenario (only used for ratings).  */
    public static final String OVERALL_IDENT = "oa";

    /**
     * Returns the set of scenarios available in the specified town. Feel free
     * to bend, fold or mutilate the returned list.
     *
     * @param includePrior if true all scenarios up to and including the
     * specified town will be returned, if false only scenarios introduced in
     * the specified town will be returned.
     */
    public static ArrayList<ScenarioInfo> getScenarios (
        String townId, boolean includePrior)
    {
        ArrayList<ScenarioInfo> scens = new ArrayList<ScenarioInfo>();
        int ttidx = BangUtil.getTownIndex(townId);
        for (ScenarioInfo info : _scenarios.values()) {
            if (includePrior) {
                if (BangUtil.getTownIndex(info.getTownId()) <= ttidx) {
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
     * Returns an array of all scenario ids available in the supplied town.
     *
     * @param includePrior if true all scenarios up to and including the
     * specified town will be returned, if false only scenarios introduced in
     * the specified town will be returned.
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
     * @param includePrior if true scenarios for towns previous to the supplied
     * town will be included in the selection.
     */
    public static String[] selectRandomIds (
        String townId, int count, int players, boolean includePrior)
    {
        ArrayList<ScenarioInfo> scens = getScenarios(townId, includePrior);

        // prune out scenarios that don't support the specified player count
        for (Iterator<ScenarioInfo> iter = scens.iterator(); iter.hasNext(); ) {
            ScenarioInfo info = iter.next();
            if (!info.supportsPlayers(players)) {
                iter.remove();
            }
        }

        // now select randomly from the remainder
        String[] scids = new String[count];
        for (int ii = 0; ii < count; ii++) {
            scids[ii] = RandomUtil.pickRandom(scens).getIdent();
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
     * Returns the stat associated with the scenario's primary objective.
     */
    public abstract Stat.Type getObjective ();

    /**
     * Returns the number of points earned for each time the objective is
     * reached.
     */
    public abstract int getPointsPerObjective ();

    /**
     * Returns the stat associated with our secondary objective or null if this
     * scenario has no secondary objective.
     */
    public Stat.Type getSecondaryObjective ()
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
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns the path to sound clips that should be preloaded when playing
     * this scenario.
     */
    public String[] getPreLoadClips ()
    {
        return PRELOAD_CLIPS;
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
        int tidx = BangUtil.getTownIndex(getTownId());
        int otidx = BangUtil.getTownIndex(oinfo.getTownId());
        return (tidx != otidx) ? tidx - otidx :
            getIdent().compareTo(oinfo.getIdent());
    }

    /**
     * Registers a scenario info record in our table.
     */
    protected static void register (ScenarioInfo info)
    {
        _scenarios.put(info.getIdent(), info);
    }

    /** Maps scenario ids to scenario info instances. */
    protected static HashMap<String,ScenarioInfo> _scenarios =
        new HashMap<String,ScenarioInfo>();

    /** The default set of clips to preload: none. */
    protected static final String[] PRELOAD_CLIPS = {};

    static {
        // frontier town scenarios
        register(new LandGrabInfo());
        register(new CattleRustlingInfo());
        register(new ClaimJumpingInfo());

        // indian trading post scenarios
        register(new WendigoAttackInfo());
        register(new TotemBuildingInfo());
        register(new ForestGuardiansInfo());

        // boom town post scenarios
//         register(new GoldRushInfo());
    }
}
