//
// $Id$

package com.threerings.bang.game.data.scenario;

import java.util.HashMap;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.StatType;

/**
 * Contains metadata on the Practice scenario.
 */
public class PracticeInfo extends ScenarioInfo
{
    /** The string identifier for this scenario. */
    public static final String IDENT = "pr";

    public PracticeInfo ()
    {
    }

    public PracticeInfo (String townId)
    {
        _townId = townId;
    }

    @Override // from ScenarioInfo
    public String getIdent ()
    {
        return IDENT;
    }

    @Override // from ScenarioInfo
    public String getTownId ()
    {
        return _townId;
    }

    @Override // from ScenarioInfo
    public String getMusic ()
    {
        return BangCodes.FRONTIER_TOWN + "/scenario_" + TutorialInfo.IDENT;
    }

    @Override // from ScenarioInfo
    public boolean showDetailedMarquee ()
    {
        return false;
    }

    @Override // from ScenarioInfo
    public StatType[] getObjectives ()
    {
        return null;
    }

    @Override // from ScenarioInfo
    public int[] getPointsPerObjectives ()
    {
        return null;
    }

    /**
     * Get the practice board name for the specified townId.
     */
    public static String getBoardName (String townId)
    {
        String board = PRACTICE_BOARDS.get(townId);
        if (board == null) {
            board = DEFAULT_BOARD;
        }
        return board;
    }

    protected String _townId;

    protected static final String DEFAULT_BOARD = "Training Corral";
    protected static final HashMap<String, String> PRACTICE_BOARDS = 
        new HashMap<String, String>();
    static {
        PRACTICE_BOARDS.put(BangCodes.FRONTIER_TOWN, DEFAULT_BOARD);
        PRACTICE_BOARDS.put(BangCodes.INDIAN_POST, "Warrior's Trial");
    }
}
