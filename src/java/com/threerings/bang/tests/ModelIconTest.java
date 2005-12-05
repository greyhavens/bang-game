//
// $Id$

package com.threerings.bang.tests;

import java.util.logging.Level;

import com.jme.util.LoggingSystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.util.BasicContext;

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

    protected void createInterface (BDecoratedWindow window)
    {
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("steamgunman")),
                   BorderLayout.WEST);
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("gunslinger")),
                   BorderLayout.CENTER);
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("dirigible")),
                   BorderLayout.EAST);
    }
}
