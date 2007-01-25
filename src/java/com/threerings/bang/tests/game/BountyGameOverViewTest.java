//
// $Id$

package com.threerings.bang.tests.game;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.bui.BWindow;

import com.samskivert.util.RandomUtil;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.tests.TestApp;

import com.threerings.bang.bounty.data.BountyConfig;

import com.threerings.bang.game.client.BountyGameOverView;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangObject;

/**
 * Test harness for the Bounty Game Over view.
 */
public class BountyGameOverViewTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        BountyGameOverViewTest test = new BountyGameOverViewTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected BWindow createWindow ()
    {
        PlayerObject user = new PlayerObject();
        user.handle = new Handle("Wild Annie");
        user.inventory = new DSet<Item>(new Purse[] { new Purse(-1, 1) });
        user.scrip = 125378;
        user.stats = new StatSet();

        BountyConfig config = BountyConfig.getBounty("dynamite_daltry");
        String gameId = "no_trespassing";
        BangConfig gconfig = null;
        try {
            String path = "bounties/frontier_town/most_wanted/dynamite_daltry/" + gameId + ".game";
            gconfig = (BangConfig)BinaryImporter.getInstance().load(
                _ctx.getResourceManager().getResource(path));
            gconfig.type = BangConfig.Type.BOUNTY;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(255);
        }

        BangObject bangobj = new BangObject();
        bangobj.players = new Name[] {
            user.handle,
            new Name("Elvis"),
        };
        bangobj.playerInfo = new BangObject.PlayerInfo[bangobj.players.length];
        bangobj.awards = new Award[bangobj.players.length];
        bangobj.state = BangObject.GAME_OVER;
        bangobj.critStats = new StatSet();
        bangobj.critStats.setStat(Stat.Type.CATTLE_RUSTLED, RandomUtil.getInt(10));
        bangobj.critStats.setStat(Stat.Type.UNITS_LOST, 2);
        bangobj.critStats.setStat(Stat.Type.BRAND_POINTS, 250);
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            bangobj.awards[ii] = new Award();
            bangobj.awards[ii].pidx = bangobj.awards.length-ii-1;
            if (bangobj.awards[ii].pidx == 0) {
                bangobj.awards[ii].item = Badge.Type.DISTANCE_MOVED_1.newBadge();
            }
            bangobj.awards[ii].rank = ii;
            bangobj.awards[ii].cashEarned = 100;
            bangobj.playerInfo[ii] = new BangObject.PlayerInfo();
            bangobj.playerInfo[ii].avatar = BangAI.getAvatarPrint(RandomUtil.getInt(100) > 50);
        }

        return new BountyGameOverView(_ctx, config, gameId, gconfig, bangobj, user);
    }
}
