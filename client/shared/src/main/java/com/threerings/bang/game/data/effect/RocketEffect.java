//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.util.StreamablePoint;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.RocketHandler;
import com.threerings.bang.game.client.EffectHandler;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Communicates that a ballistic shot was fired from one piece to another.
 */
public class RocketEffect extends AreaEffect
{
    /** The shooter. */
    public Piece shooter;

    /** Amount of damage being applied. */
    public int baseDamage;

    /** Extra targeted areas. */
    public StreamablePoint[] affectedPoints = new StreamablePoint[0];

    /** Constructor used when unserializing. */
    public RocketEffect ()
    {
    }

    public RocketEffect (Piece shooter, int damage)
    {
        this.shooter = shooter;
        baseDamage = damage;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new RocketHandler();
    }

    /**
     * Returns the type of shot fired, which could be a model or an effect.
     */
    public String getShotType ()
    {
        return "units/frontier_town/artillery/shell";
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        ArrayIntSet affected = new ArrayIntSet();

        // shoot in a random direction
        //int dir = RandomUtil.getInt(Piece.DIRECTIONS.length);
        for (int dir : Piece.DIRECTIONS) {
            Piece piece = bangobj.getFirstAvailableTarget(shooter.x, shooter.y, dir);
            if (piece.pieceId != -1) {
                affected.add(piece.pieceId);
            } else {
                affectedPoints = ArrayUtil.append(affectedPoints,
                    new StreamablePoint(piece.x, piece.y));
            }
        }
        pieces = affected.toIntArray();
    }

    @Override // documentation inherited
    public void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        // finally do the damage
        damage(bangobj, obs, shooter.owner, shooter, piece, baseDamage, "rocket_burst");
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.RocketEffect.class, "shooter", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.RocketEffect.class, "baseDamage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.RocketEffect.class, "affectedPoints", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.RocketEffect.class, "shooter", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.RocketEffect.class, "baseDamage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.RocketEffect.class, "affectedPoints", this, out);
    }
}
