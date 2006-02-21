//
// $Id$

package com.threerings.bang.tests.game;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BWindow;

import com.threerings.bang.tests.TestApp;

import com.threerings.bang.game.client.RoundTimerView;

/**
 * Test harness for the round timer view.
 */
public class RoundTimerViewTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        RoundTimerViewTest test = new RoundTimerViewTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected BWindow createWindow ()
    {
        RoundTimerView view = new RoundTimerView(_ctx);
        view.setStatus(20, 79, 100);
        return view;
    }
}
