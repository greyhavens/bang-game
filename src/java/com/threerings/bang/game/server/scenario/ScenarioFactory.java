//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.threerings.bang.data.BangCodes;

import static com.threerings.bang.Log.log;

/**
 * Enumerates all available scenarios.
 */
public class ScenarioFactory
{
    /** Lists all scenarios available in all towns up to and including the
     * specified town. */
    public String[] getScenarios (String town)
    {
        ArrayList<String> scenarios = new ArrayList<String>();
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            scenarios.addAll(_townmap.get(BangCodes.TOWN_IDS[ii]));
        }
        return scenarios.toArray(new String[scenarios.size()]);
    }

    /**
     * Creates the specified named scenario.
     */
    public static Scenario createScenario (String name)
    {
        try {
            return _scenmap.get(name).newInstance();
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to instantiate scenario " +
                    "[name=" + name + ", class=" + _scenmap.get(name) + "]", t);
            return new Shootout();
        }
    }

    /** Maps the specified scenario to the supplied identifier and
     * town. */
    protected static void map (String ident, String town,
                               Class<? extends Scenario> sclass)
    {
        // map it by identifier
        if (_scenmap.containsKey(ident)) {
            log.warning("Zounds! Scenario identifier clash [ident=" + ident +
                        ", nclass=" + sclass +
                        ", oclass=" + _scenmap.get(ident) + "].");
        }
        _scenmap.put(ident, sclass);

        // map it by town
        ArrayList<String> townids = _townmap.get(ident);
        if (townids == null) {
            _townmap.put(ident, townids = new ArrayList<String>());
        }
        townids.add(ident);
    }

    /** A mapping from scenario identifier to {@link Scenario} class. */
    protected static HashMap<String,Class<? extends Scenario>> _scenmap =
        new HashMap<String,Class<? extends Scenario>>();

    /** A mapping from town identifier to a list of scenario identifiers. */
    protected static HashMap<String,ArrayList<String>> _townmap =
        new HashMap<String,ArrayList<String>>();

    static {
        map(ClaimJumping.IDENT, BangCodes.FRONTIER_TOWN, ClaimJumping.class);
        map(CattleHerding.IDENT, BangCodes.FRONTIER_TOWN, CattleHerding.class);
    }
}
