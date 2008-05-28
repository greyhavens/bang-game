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
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.StatType;

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
        if (args.length > 2) {
            _type = args[0];
            _bountyId = args[1];
            _gameId = args[2];
        }
        if (args.length > 3) {
            _result = Enum.valueOf(Result.class, args[3].toUpperCase());
        }

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
        user.scrip = 5378;
        user.stats = new StatSet();

        BountyConfig config = BountyConfig.getBounty(_bountyId);
        BangConfig gconfig = null;
        try {
            String path = "bounties/frontier_town/" +
                _type + "/" + _bountyId + "/" + _gameId + ".game";
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
        bangobj.critStats.setStat(StatType.CATTLE_RUSTLED, 5+RandomUtil.getInt(10));
        bangobj.critStats.setStat(StatType.UNITS_KILLED, 5);
        bangobj.critStats.setStat(StatType.UNITS_LOST, 2);
        bangobj.critStats.setStat(StatType.BRAND_POINTS, 250);
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            bangobj.awards[ii] = new Award();
            bangobj.awards[ii].pidx = bangobj.awards.length-ii-1;
            if (config.reward.badge != null) {
                bangobj.awards[ii].item = config.reward.badge.newBadge();
            } else if (config.reward.articles != null) {
                bangobj.awards[ii].item = config.reward.articles[1];
            }
            bangobj.awards[ii].rank = ii;
            bangobj.awards[ii].cashEarned = config.reward.scrip;
            bangobj.playerInfo[ii] = new BangObject.PlayerInfo();
            bangobj.playerInfo[ii].avatar = BangAI.getAvatar(RandomUtil.getInt(100) > 50);
        }

        return new BountyGameOverView(_ctx, config, _gameId, gconfig, bangobj, user) {
            protected boolean bountyGameFailed () {
                return (_result == Result.LOST);
            }
            protected boolean bountyCompleted (boolean gameFailed) {
                return (_result == Result.COMPLETED);
            }
        };
    }

    protected static String _type = "most_wanted";
    protected static String _bountyId = "hard/sgt._rusty";
    protected static String _gameId = "greenhorns";
    protected static Result _result = Result.WON;

    /** Used for testing. */
    public static enum Result { LOST, WON, COMPLETED };
}
