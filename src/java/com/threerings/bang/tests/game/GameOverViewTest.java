//
// $Id$

package com.threerings.bang.tests.game;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BWindow;

import com.samskivert.util.RandomUtil;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;

import com.threerings.bang.tests.TestApp;

import com.threerings.bang.game.client.GameOverView;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;

/**
 * Test harness for the game over view.
 */
public class GameOverViewTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        GameOverViewTest test = new GameOverViewTest();
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
        user.scrip = 1000;

        BangConfig bconfig = new BangConfig();
        bconfig.rated = false;
        bconfig.addRound("tb", null, null);
        bconfig.addRound("wa", null, null);
        bconfig.addRound("fg", null, null);

        BangObject bangobj = new BangObject();
        bangobj.players = new Name[] {
            new Name("Scary Jerry"),
            new Name("Monkey Butter"),
            user.handle,
            new Name("Elvis"),
        };
        bangobj.playerInfo = new BangObject.PlayerInfo[bangobj.players.length];
        bangobj.awards = new Award[bangobj.players.length];
        bangobj.roundId = 3;
        bangobj.scenario = new ForestGuardiansInfo();
        bangobj.perRoundRanks = new short[][] {
            { 0, 1, 2, 3 },
            { 0, 1, 2, 3 },
            { 155, 155, 155, 155 }
        };
        bangobj.state = BangObject.GAME_OVER;
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            bangobj.awards[ii] = new Award();
            bangobj.awards[ii].pidx = bangobj.awards.length-ii-1;
//             if (bangobj.awards[ii].pidx == 2) {
//                 bangobj.awards[ii].item = Badge.Type.DISTANCE_MOVED_1.newBadge();
//             }
            bangobj.awards[ii].rank = ii;
            bangobj.awards[ii].cashEarned = 100;
            bangobj.awards[ii].acesEarned = 5;
            bangobj.playerInfo[ii] = new BangObject.PlayerInfo();
            bangobj.playerInfo[ii].avatar = BangAI.getAvatar(RandomUtil.getInt(100) > 50);
        }

        return new GameOverView(_ctx, null, bconfig, bangobj, user, true);
    }
}
