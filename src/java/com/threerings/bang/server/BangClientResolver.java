//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;

import com.threerings.crowd.server.CrowdClientResolver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Stat;
import com.threerings.bang.server.persist.Player;

/**
 * Customizes the client resolver to use our {@link BangUserObject}.
 */
public class BangClientResolver extends CrowdClientResolver
{
    // documentation inherited
    public Class getClientObjectClass ()
    {
        return BangUserObject.class;
    }

    // documentation inherited
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);
        BangUserObject buser = (BangUserObject)clobj;

        // load up our per-player bits
        String username = buser.username.toString();
        Player player = BangServer.playrepo.loadPlayer(username);
        if (player == null) {
            // it's their first time, how nice
            player = new Player(username);
            BangServer.playrepo.insertPlayer(player);
            BangServer.generalLog("first_timer " + username);
        }
        buser.playerId = player.playerId;
        buser.scrip = player.scrip;

        // load up this player's items
        ArrayList<Item> items = BangServer.itemrepo.loadItems(buser.playerId);
        buser.inventory = new DSet(items.iterator());

        // load up this player's persistent stats
        ArrayList<Stat> stats = BangServer.statrepo.loadStats(buser.playerId);
        buser.stats = new DSet(stats.iterator());
    }
}
