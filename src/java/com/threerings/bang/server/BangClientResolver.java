//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;

import com.threerings.util.RandomUtil;

import com.threerings.crowd.server.CrowdClientResolver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.server.persist.Player;

/**
 * Customizes the client resolver to use our {@link PlayerObject}.
 */
public class BangClientResolver extends CrowdClientResolver
{
    // documentation inherited
    public Class getClientObjectClass ()
    {
        return PlayerObject.class;
    }

    // documentation inherited
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);
        PlayerObject buser = (PlayerObject)clobj;

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
        buser.handle = new Handle(player.handle);
        buser.isMale = player.isSet(Player.IS_MALE_FLAG);
        buser.scrip = player.scrip;
        buser.coins = BangServer.coinmgr.getCoinRepository().getCoinCount(
            player.accountName);

        // load up this player's items
        ArrayList<Item> items = BangServer.itemrepo.loadItems(buser.playerId);
        buser.inventory = new DSet(items.iterator());

        // load up this player's persistent stats
        ArrayList<Stat> stats = BangServer.statrepo.loadStats(buser.playerId);
        buser.stats = new StatSet(stats.iterator());
        buser.stats.setContainer(buser);

        // load up this player's avatar looks
        buser.look = player.look;
        buser.looks = new DSet(
            BangServer.lookrepo.loadLooks(player.playerId).iterator());
    }
}
