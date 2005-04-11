//
// $Id$

package com.samskivert.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;

/**
 * An effect that does damage to all units within a certain area.
 */
public class AreaDamageEffect extends AreaEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String MISSILED = "howdy";

    public int damage;

    public AreaDamageEffect ()
    {
    }

    public AreaDamageEffect (int damage, int radius, int x, int y)
    {
        super(radius, x, y);
        this.damage = damage;
    }

    @Override // documentation inherited
    protected void noteAffected (Piece piece, IntIntMap dammap)
    {
        dammap.increment(piece.owner, Math.min(damage, 100-piece.damage));
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, Piece piece, int dist)
    {
        int pdamage = damage;
        for (int dd = 0; dd < dist; dd++) {
            pdamage /= 2;
        }
        ShotEffect.damage(bangobj, obs, piece, pdamage, MISSILED);
    }
}
