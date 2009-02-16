//
// $Id$

package com.threerings.bang.web.logic;

import java.util.Comparator;

import com.samskivert.servlet.util.FriendlyException;
import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.SortableArrayList;
import com.samskivert.util.StringUtil;
import com.samskivert.velocity.InvocationContext;

import com.threerings.stats.data.Stat;
import com.threerings.user.OOOUser;

import com.threerings.bang.data.Handle;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.web.OfficeApp;

/**
 * Displays information on a particular player.
 */
public class player extends AdminLogic
{
    // documentation inherited
    public void invoke (OfficeApp app, InvocationContext ctx, OOOUser user)
        throws Exception
    {
        PlayerRecord player = null;
        OOOUser target;

        String who;
        String handle = ParameterUtil.getParameter(ctx.getRequest(), "handle", false);
        if (!StringUtil.isBlank(handle)) {
            // load up their player record by handle
            player = app.getPlayerRepository().loadByHandle(new Handle(handle));
            // and load their user record from that
            who = (player == null) ? null : player.accountName;
        } else {
            who = ParameterUtil.getParameter(ctx.getRequest(), "who", false);
        }

        if (StringUtil.isBlank(who)) {
            return;
        }
        target = (OOOUser)app.getUserManager().getRepository().loadUser(who);

        // load up their OOO user record
        if (target == null) {
            throw new FriendlyException("error.no_such_player");
        }
        ctx.put("target", target);
        ctx.put("affiliate", app.getSiteIdentifier().getSiteString(target.siteId));

        // load up their Bang! player record if we haven't already
        if (player == null) {
            player = app.getPlayerRepository().loadPlayer(who);
        }

        if (player != null) {
            ctx.put("player", player);

            // load up and sort their stats
            SortableArrayList<Stat> stats = new SortableArrayList<Stat>();
            stats.addAll(app.getStatRepository().loadStats(player.playerId));
            stats.sort(new Comparator<Stat>() {
                public int compare (Stat one, Stat two) {
                    return one.getKey().compareTo(two.getKey());
                }
            });
            ctx.put("stats", stats);

            // load up and display their inventory
            ctx.put("items", app.getItemRepository().loadItems(player.playerId));
        }
    }
}
