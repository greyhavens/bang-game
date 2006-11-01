//
// $Id$

package com.threerings.bang.tests.game;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.Name;

import com.threerings.bang.game.client.FinalistView;

import com.threerings.bang.tests.TestApp;

/**
 * Test harness for the finalist view.  (Is no longer functional)
 */
public class FinalistViewTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        FinalistViewTest test = new FinalistViewTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected void createInterface (BWindow window)
    {
        window.setLayoutManager(GroupLayout.makeHoriz(GroupLayout.CENTER));
        /*
        window.add(new FinalistView(_ctx, 0, new Name("Rabid Jerrymanderer"),
                                    TEST_AVATAR, 0));
        window.add(new FinalistView(_ctx, 1, new Name("WwWwWwWwWwWwWwWwWw"),
                                    TEST_AVATAR, 1));
        window.add(new FinalistView(_ctx, 2, new Name("Wyjq WyjqWyjq Wyjq"),
                                    TEST_AVATAR, 2));
        window.add(new FinalistView(_ctx, 3, new Name("Wild Annie"),
                                    TEST_AVATAR, 3));
                                    */
    }

    protected static final int[] TEST_AVATAR = {
        198, 19, 38, 45, 71, 77, 81, 1114163, 1114167,
        671088665, 1370554375, 1370554381 };
}
