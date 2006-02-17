//
// $Id$

package com.threerings.bang.tests.game;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BDecoratedWindow;

import com.threerings.presents.dobj.DSet;

import com.threerings.util.Name;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;

import com.threerings.bang.tests.TestApp;

import com.threerings.bang.game.client.GameOverView;
import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangObject;

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

    protected BDecoratedWindow createWindow ()
    {
        PlayerObject user = new PlayerObject();
        user.handle = new Handle("Wild Annie");
        user.inventory = new DSet(new Purse[] { new Purse(-1, 1) });
        user.scrip = 125378;

        BangObject bangobj = new BangObject();
        bangobj.players = new Name[] {
            new Name("Scary Jerry"),
            new Name("Monkey Butter"),
            user.handle,
            new Name("Elvis"),
        };
        bangobj.awards = new Award[bangobj.players.length];
        for (int ii = 0; ii < bangobj.awards.length; ii++) {
            bangobj.awards[ii] = new Award();
            bangobj.awards[ii].pidx = bangobj.awards.length-ii-1;
            bangobj.awards[ii].rank = ii;
            bangobj.awards[ii].cashEarned = 100;
            bangobj.awards[ii].badge = Badge.Type.IRON_HORSE.newBadge();
        }

        return new GameOverView(_ctx, null, bangobj, user);
    }
}
