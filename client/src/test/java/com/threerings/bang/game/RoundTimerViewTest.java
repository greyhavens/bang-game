//
// $Id$

package com.threerings.bang.game;

import java.util.logging.Level;

import com.samskivert.util.Interval;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BWindow;

import com.threerings.bang.TestApp;

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
        final RoundTimerView view = new RoundTimerView(_ctx);
        view.setStatus(0, 79, 100);
        new Interval(_ctx.getApp()) {
            public void expired () {
                if (++_progress < 80) {
                    view.setStatus(_progress, 79, 100);
                }
            }
        }.schedule(250, true);
        return view;
    }

    protected int _progress;
}
