//
// $Id$

package com.threerings.bang.tests.server;

import com.samskivert.depot.StaticConnectionProvider;
import com.samskivert.depot.PersistenceContext;

import com.threerings.bang.data.StatType;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BangStatRepository;

/**
 * A standalone test for the stat repository dynamically assigned string code
 * system.
 */
public class StringCodeTest
{
    public static void main (String[] args)
    {
        try {
            BangStatRepository statrepo = new BangStatRepository(
                new PersistenceContext(
                    "bangdb", new StaticConnectionProvider(ServerConfig.getJDBCConfig()), null));

            // this should generate a warning
            System.out.println("Looking for missing code: " +
                               statrepo.getCodeString(StatType.UNUSED, -1));

            // the first time we'll create and insert a mapping
            String value = "test_one";
            System.out.println("Create mapping: " + value + "=" +
                               statrepo.getStringCode(StatType.UNUSED, value));

            // the second time it comes from the cache
            System.out.println("Lookup from cache: " + value + "=" +
                               statrepo.getStringCode(StatType.UNUSED, value));

            // now clear the mapping and go through the code path that would be
            // executed if someone else created a mapping before we did
            statrepo.clearMapping(StatType.UNUSED, value);
            System.out.println("Lookup with value collision: " + value + "=" +
                               statrepo.getStringCode(StatType.UNUSED, value));

            // now add a second value, clear it out and fake a code collision
            // (requires hackery in the stat repository)
            value = "test_two";
            System.out.println("Create with code collision: " + value + "=" +
                               statrepo.getStringCode(StatType.UNUSED, value));

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
