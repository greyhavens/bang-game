//
// $Id$

package com.threerings.bang.tests;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;
import com.jmex.bui.BWindow;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.ranch.client.UnitIcon;

/**
 * A test program for displaying unit model icons.
 */
public class ModelIconTest extends TestApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        ModelIconTest test = new ModelIconTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected void createInterface (BWindow window)
    {
        for (String type : TYPES) {
            window.add(new UnitIcon(_ctx, UnitConfig.getConfig(type, true)));
        }
    }

    protected static final String[] TYPES = {
        "steamgunman", "gunslinger", "dirigible" };
}
