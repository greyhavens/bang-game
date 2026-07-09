//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.AreaDamageHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * An effect that does damage to all units within a certain area.
 */
public class AreaDamageEffect extends AreaEffect
{
    /** The index of the player causing the damage or -1. */
    public int causer;

    /** The base damage for this effect. */
    public int baseDamage;

    /** The updated damage for the affected pieces. */
    public int[] newDamage;

    /** Death effects corresponding to each piece (<code>null</code> for pieces
     * that didn't die or didn't produce a death effect). */
    public Effect[] deathEffects;
    
    public AreaDamageEffect ()
    {
    }

    public AreaDamageEffect (int causer, int damage, int radius, int x, int y)
    {
        super(radius, x, y);
        this.causer = causer;
        this.baseDamage = damage;
    }
    
    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] apieces = pieces;
        for (Effect effect : deathEffects) {
            if (effect != null) {
                apieces = concatenate(apieces, effect.getAffectedPieces());
            }
        }
        return apieces;
    }
    
    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] wpieces = NO_PIECES;
        for (Effect effect : deathEffects) {
            if (effect != null) {
                wpieces = concatenate(wpieces, effect.getWaitPieces());
            }
        }
        return wpieces;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // determine the damage for each piece
        newDamage = new int[pieces.length];
        deathEffects = new Effect[pieces.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = bangobj.pieces.get(pieces[ii]);
            int damage = getDamage(target);
            newDamage[ii] = target.damage + damage;
            dammap.increment(target.owner, damage);
            if (newDamage[ii] == 100) {
                deathEffects[ii] = target.willDie(bangobj, -1);
                if (deathEffects[ii] != null) {
                    deathEffects[ii].prepare(bangobj, dammap);
                }
            }
        }
    }
    
    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new AreaDamageHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return baseDamage / (piece.getDistance(x, y) + 1);
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx == -1) {
            return null;
        }
        String names = getPieceNames(bangobj, pidx, pieces);
        return (names == null) ?
            null : MessageBundle.compose("m.effect_missile", names);
    }
    
    /**
     * Returns the damage done to the specified piece.
     */
    protected int getDamage (Piece piece)
    {
        int damage = getBaseDamage(piece);
        return Math.min(damage, 100-piece.damage);
    }
    
    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        if (deathEffects[pidx] != null) {
            deathEffects[pidx].apply(bangobj, obs);
        }
        damage(bangobj, obs, causer, null, piece, newDamage[pidx],
            ShotEffect.DAMAGED);
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "causer", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "baseDamage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "newDamage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "deathEffects", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "causer", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "baseDamage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "newDamage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaDamageEffect.class, "deathEffects", this, out);
    }
}
