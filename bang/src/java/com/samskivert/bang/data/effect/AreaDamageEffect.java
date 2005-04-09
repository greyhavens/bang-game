//
// $Id$

package com.samskivert.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.threerings.media.util.MathUtil;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;

/**
 * An effect that does damage to all units within a certain area.
 */
public class AreaDamageEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String MISSILED = "howdy";

    public int damage;

    public int radius;

    public short x, y;

    public int[] pieces;

    public AreaDamageEffect ()
    {
    }

    public AreaDamageEffect (int damage, int radius, int x, int y)
    {
        this.damage = damage;
        this.radius = radius;
        this.x = (short)x;
        this.y = (short)y;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj)
    {
        ArrayIntSet affected = new ArrayIntSet();
        int r2 = radius * radius;
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (MathUtil.distanceSq(p.x, p.y, x, y) <= r2) {
                affected.add(p.pieceId);
            }
        }
        pieces = affected.toIntArray();
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = (Piece)bangobj.pieces.get(pieces[ii]);
            int dist = Math.abs(target.x - x) + Math.abs(target.y - y);
            int pdamage = damage;
            for (int dd = 0; dd < dist; dd++) {
                pdamage /= 2;
            }
            if (target == null) {
                log.warning("Missing piece for area of effect damage " +
                            "[pid=" + pieces[ii] + ", effect=" + this + "].");
            } else {
                ShotEffect.damage(bangobj, obs, target, pdamage, MISSILED);
            }
        }
    }
}
