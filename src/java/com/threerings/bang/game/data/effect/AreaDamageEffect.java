//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * An effect that does damage to all units within a certain area.
 */
public class AreaDamageEffect extends AreaEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String MISSILED = "howdy";

    /** The base damage for this effect. */
    public int baseDamage;

    /** The updated damage for the affected pieces. */
    public int[] newDamage;

    public AreaDamageEffect ()
    {
    }

    public AreaDamageEffect (int damage, int radius, int x, int y)
    {
        super(radius, x, y);
        this.baseDamage = damage;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // we already computed the damage for each piece in noteAffected()
        // so we can just look it up again here
        newDamage = new int[pieces.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = (Piece)bangobj.pieces.get(pieces[ii]);
            newDamage[ii] = target.damage + dammap.get(pieces[ii]);
        }
    }

    @Override // documentation inherited
    protected void noteAffected (Piece piece, IntIntMap dammap, int dist)
    {
        int damage = baseDamage / dist;
        dammap.increment(piece.owner, Math.min(damage, 100-piece.damage));
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        ShotEffect.damage(bangobj, obs, piece, newDamage[pidx], MISSILED);
    }
}
