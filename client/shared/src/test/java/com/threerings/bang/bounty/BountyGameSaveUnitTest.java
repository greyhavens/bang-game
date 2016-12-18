//
// $Id$

package com.threerings.bang.bounty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;

import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.threerings.bang.data.StatType;
import com.threerings.bang.game.data.BangConfig;

import com.threerings.bang.bounty.data.IntStatCriterion;

/**
 * Tests the saving and loading of bounty game configuration.
 */
public class BountyGameSaveUnitTest extends TestCase
{
    public BountyGameSaveUnitTest ()
    {
        super(BountyGameSaveUnitTest.class.getName());
    }

    public void runTest ()
    {
        BangConfig config = new BangConfig(), nconfig;
        config.addRound("Test Board", "wa", null);

        config.addPlayer("frontier_town/cavalry", new String[] {
            "frontier_town/artillery", "frontier_town/dirigible" });
        config.addPlayer("frontier_town/tactician", new String[] {
            "frontier_town/artillery", "frontier_town/steamgunman" });
        config.addPlayer("frontier_town/codger", new String[] {
            "frontier_town/steamgunman", "frontier_town/sharpshooter" });

        IntStatCriterion crit = new IntStatCriterion();
        crit.stat = StatType.POINTS_EARNED;
        crit.condition = IntStatCriterion.Condition.AT_LEAST;
        crit.value = 500;
        config.criteria.add(crit);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // serialize the configuration
            // System.out.println("Config: " + config);
            BinaryExporter.getInstance().save(config, out);
        } catch (IOException ioe) {
            fail("Save failed " + ioe);
        }

        try {
            // load it back in
            nconfig = (BangConfig)BinaryImporter.getInstance().load(out.toByteArray());
            // System.out.println("Config: " + nconfig);
            assertEquals("Configs are not equal", config, nconfig);
        } catch (IOException ioe) {
            fail("Load failed " + ioe);
        }
    }

    public static Test suite ()
    {
        return new BountyGameSaveUnitTest();
    }

    public static void main (String[] args)
    {
        BountyGameSaveUnitTest test = new BountyGameSaveUnitTest();
        test.runTest();
    }
}
