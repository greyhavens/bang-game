//
// $Id$

package com.threerings.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.surprise.Surprise;

import static com.threerings.bang.Log.log;

/**
 * Delivers a surprise to the specified player.
 */
public class GrantSurpriseEffect extends Effect
{
    public int player;

    public GrantSurpriseEffect (int player, Surprise surprise)
    {
        this.player = player;
        _surprise = surprise;
    }

    public GrantSurpriseEffect ()
    {
    }

    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // make sure our player has room for another surprise
        int have = 0;
        for (Iterator iter = bangobj.surprises.iterator(); iter.hasNext(); ) {
            Surprise s = (Surprise)iter.next();
            if (s.owner == player) {
                have++;
            }
        }
        if (have >= 3) {
            log.info("No soup four you! " + player + ".");
            return;
        }

        _surprise.init(bangobj, player);
        bangobj.addToSurprises(_surprise);
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // NOOP
    }

    protected transient Surprise _surprise;
}
