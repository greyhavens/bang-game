//
// $Id$

package com.threerings.bang.web.logic;

import java.util.ArrayList;
import java.util.EnumSet;

import com.samskivert.servlet.util.ParameterUtil;
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
        ctx.put("types", EnumSet.allOf(Stat.Type.class));

        // if they specified a type, look it up
        Stat.Type type = Stat.getType(
            ParameterUtil.getIntParameter(
                ctx.getRequest(), "type", 0, "error.invalid_type"));
        if (type != null) {
            final ArrayList<String> stats = new ArrayList<String>();
            StatRepository.Processor proc = new StatRepository.Processor() {
                public void process (int playerId, Stat stat) {
                    stats.add(stat.valueToString());
                }
            };
            app.getStatRepository().processStats(proc, type);
            ctx.put("stats", stats);
        }
    }
}
