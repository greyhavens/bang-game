//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.LandGrabLogic;

import static com.threerings.bang.Log.log;

/**
 * Implements the server side of the Land Grab gameplay scenario.
 */
public class LandGrab extends Scenario
{
    public LandGrab ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new RespawnDelegate());
        registerDelegate(_homedel = new HomesteadDelegate());
    }

    /**
     * Returns the list of homesteads on the board.
     */
    public ArrayList<Homestead> getHomesteads ()
    {
        return _homedel.getHomesteads();
    }

    @Override // from Scenario
    public AILogic createAILogic (GameAI ai)
    {
        return new LandGrabLogic(this);
    }

    @Override // from Scenario
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // increment each players' homestead related stats
        int[] steads = new int[bangobj.players.length];
        for (Homestead stead : getHomesteads()) {
            if (stead.owner >= 0) {
                steads[stead.owner]++;
            }
        }
        int loneClaimerIdx = -1;
        for (int ii = 0; ii < steads.length; ii++) {
            // check to see if one player alone holds all claimed claims
            if (steads[ii] > 0) {
                loneClaimerIdx = (loneClaimerIdx == -1) ? ii : -2;
            }
        }
        if (loneClaimerIdx >= 0) {
            bangobj.stats[loneClaimerIdx].incrementStat(Stat.Type.LONE_STEADER, 1);
        }
    }

    @Override // from Scenario
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // persist their various homestead related stats
        for (Stat.Type stat : ACCUM_STATS) {
            user.stats.incrementStat(stat, bangobj.stats[pidx].getIntStat(stat));
        }
    }

    @Override // from Scenario
    protected Point getStartSpot (int pidx)
    {
        Point spot = _homedel.getStartSpot(pidx);
        return (spot == null) ? super.getStartSpot(pidx) : spot;
    }

    /** Handles the behavior of our homesteads. */
    protected HomesteadDelegate _homedel;

    /** Stats we accumulate from the in-game versions to the player's object. */
    protected static final Stat.Type[] ACCUM_STATS = {
        Stat.Type.STEADS_CLAIMED, Stat.Type.STEADS_DESTROYED, Stat.Type.LONE_STEADER
    };
}
