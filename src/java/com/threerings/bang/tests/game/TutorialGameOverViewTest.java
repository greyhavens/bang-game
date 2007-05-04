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

import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.GuestHandle;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.StatType;

import com.threerings.bang.tests.TestApp;

import com.threerings.bang.game.client.TutorialGameOverView;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;

/**
 * Test harness for the Tutorial Game Over view.
 */
public class TutorialGameOverViewTest extends TestApp
{
    public static void main (String[] args)
    {
        if (args.length > 0) {
            _tutId = args[0];
        }
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        TutorialGameOverViewTest test = new TutorialGameOverViewTest();
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
        user.handle = new GuestHandle("1234bca5316");
        user.inventory = new DSet<Item>(new Purse[] { new Purse(-1, 1) });
        user.scrip = 5378;
        user.stats = new StatSet();
        user.townId = "frontier_town";

        BangConfig gconfig = new BangConfig();
        gconfig.rated = false;
        if (_tutId.startsWith(TutorialCodes.PRACTICE_PREFIX)) {
            gconfig.addRound(_tutId.substring(TutorialCodes.PRACTICE_PREFIX.length()), null, null);
        } else {
            gconfig.type = BangConfig.Type.TUTORIAL;
        }

        BangObject bangobj = new BangObject();
        bangobj.players = new Name[] {
            user.handle,
            new Name("Elvis"),
        };
        bangobj.playerInfo = new BangObject.PlayerInfo[bangobj.players.length];
        bangobj.awards = new Award[bangobj.players.length];
        bangobj.state = BangObject.GAME_OVER;
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            bangobj.awards[ii] = new Award();
            bangobj.awards[ii].pidx = bangobj.awards.length-ii-1;
            bangobj.awards[ii].rank = ii;
            bangobj.awards[ii].item = new CardItem(0, "repair");
            bangobj.awards[ii].cashEarned = 500;
            bangobj.playerInfo[ii] = new BangObject.PlayerInfo();
            bangobj.playerInfo[ii].avatar = BangAI.getAvatar(RandomUtil.getInt(100) > 50);
        }

        return new TutorialGameOverView(_ctx, _tutId, gconfig, bangobj, user);
    }

    protected static String _tutId = TutorialCodes.NEW_TUTORIALS[0][2];
}
