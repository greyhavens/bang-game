//
// $Id$

package com.threerings.bang.web.logic;

import java.util.ArrayList;

import com.samskivert.velocity.InvocationContext;

import com.threerings.user.OOOUser;

import com.threerings.bang.data.Stat;
import com.threerings.bang.server.persist.StatRepository;

import com.threerings.bang.web.OfficeApp;

/**
 * Displays graphs and summary information on statistics accumulated by players
 * in game.
 */
public class player_stats extends AdminLogic
{
    // documentation inherited
    public void invoke (OfficeApp app, InvocationContext ctx, OOOUser user)
        throws Exception
    {
        final ArrayList<Stat> stats = new ArrayList<Stat>();
        Stat.Type type = Stat.Type.GAMES_PLAYED;
        StatRepository.Processor proc = new StatRepository.Processor() {
            public void process (int playerId, Stat stat) {
                stats.add(stat);
            }
        };
        app.getStatRepository().processStats(proc, type);
        ctx.put("stats", stats);
    }
}
