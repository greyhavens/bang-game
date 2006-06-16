//
// $Id$

package com.threerings.bang.game.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Marker;

/**
 * Contains scenario-related utilities.
 */
public class ScenarioUtil
{
    /**
     * Selects a random list of scenarios for the specified town.
     */
    public static String[] selectRandom (String townId, int count)
    {
        String[] avail = getScenarios(townId);
        String[] choices = new String[count];
        for (int ii = 0; ii < choices.length; ii++) {
            choices[ii] = (String)RandomUtil.pickRandom(avail);
        }
        return choices;
    }

    /**
     * Returns the scenarios that are valid in the specified town (this
     * includes all scenarios from previous towns).
     */
    public static String[] getScenarios (String townId)
    {
        return _scenmap.get(townId);
    }

    /**
     * Called on the client to preload any sounds for this scenario.
     */
    public static void preloadSounds (String scenarioId, SoundGroup sounds)
    {
        if (scenarioId.equals(ScenarioCodes.CLAIM_JUMPING) ||
            scenarioId.equals(ScenarioCodes.GOLD_RUSH) ||
            scenarioId.equals(ScenarioCodes.TUTORIAL)) {
            sounds.preloadClip("rsrc/" + NuggetEffect.NUGGET_ADDED + ".wav");
            sounds.preloadClip("rsrc/" + NuggetEffect.NUGGET_REMOVED + ".wav");
            sounds.preloadClip(
                "rsrc/" + NuggetEffect.PICKED_UP_NUGGET + ".wav");
        }
    }

    /**
     * Returns true if this scenario involves cattle rustlin'.
     */
    public static boolean cattleRustling (String scenarioId)
    {
        return (ScenarioCodes.CATTLE_RUSTLING.equals(scenarioId));
    }

    /**
     * Returns true if this scenario involves claiming nuggets.
     */
    public static boolean nuggetClaiming (String scenarioId)
    {
        return (ScenarioCodes.CLAIM_JUMPING.equals(scenarioId) ||
                ScenarioCodes.GOLD_RUSH.equals(scenarioId));
    }

    /**
     * Returns true if this scenario involves totem building.
     */
    public static boolean totemBuilding (String scenarioId)
    {
        return (ScenarioCodes.TOTEM_BUILDING.equals(scenarioId));
    }

    /**
     * Returns true if this maker is valid for the scenario.
     */
    public static boolean isValidMarker (Marker m, String scenarioId)
    {
        if (m.getType() == Marker.CATTLE && 
                !ScenarioCodes.CATTLE_RUSTLING.equals(scenarioId)) {
            return false;
        } else if (m.getType() == Marker.LODE &&
                !ScenarioCodes.GOLD_RUSH.equals(scenarioId)) {
            return false;
        } else if (m.getType() == Marker.TOTEM &&
                !ScenarioCodes.TOTEM_BUILDING.equals(scenarioId)) {
            return false;
        }
        return true;
    }

    /** Maps town ids to a list of valid gameplay scenarios. */
    protected static HashMap<String,String[]> _scenmap =
        new HashMap<String,String[]>();
    static {
        ArrayList<String> scens = new ArrayList<String>();
        CollectionUtil.addAll(scens, ScenarioCodes.FRONTIER_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.FRONTIER_TOWN,
                     scens.toArray(new String[scens.size()]));
        CollectionUtil.addAll(scens, ScenarioCodes.INDIAN_POST_SCENARIOS);
        _scenmap.put(BangCodes.INDIAN_POST,
                     scens.toArray(new String[scens.size()]));
        CollectionUtil.addAll(scens, ScenarioCodes.BOOM_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.BOOM_TOWN,
                     scens.toArray(new String[scens.size()]));
        CollectionUtil.addAll(scens, ScenarioCodes.GHOST_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.GHOST_TOWN,
                     scens.toArray(new String[scens.size()]));
        CollectionUtil.addAll(scens, ScenarioCodes.CITY_OF_GOLD_SCENARIOS);
        _scenmap.put(BangCodes.CITY_OF_GOLD,
                     scens.toArray(new String[scens.size()]));
    }
}
