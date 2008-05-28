//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Clears a piece from the board.
 */
public class ExplodeEffect extends AreaEffect
{
    /** The id of the piece to remove. */
    public Piece piece;
    
    /** The index of the player causing the damage or -1. */
    public int causer;

    /** The base damage for this effect. */
    public int baseDamage;

    /** The updated damage for the affected pieces. */
    public int[] newDamage;

    /** Death effects corresponding to each piece (<code>null</code> for pieces
     * that didn't die or didn't produce a death effect). */
    public Effect[] deathEffects;
    
    public ExplodeEffect ()
    {
    }
    
    public ExplodeEffect (Piece piece, int damage, int radius)
    {
        super(radius, piece.x, piece.y);
        this.piece = piece;
        this.causer = piece.owner;
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
    public int getBaseDamage (Piece piece)
    {
        return baseDamage / (piece.getDistance(x, y) + 1);
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
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (piece != null) {
            reportEffect(obs, piece, EXPLODE_EFFECT);
        }
        
        return super.apply(bangobj, obs);
    }    
    
    
    /** An effect reported on the primary target. */
    public static final String EXPLODE_EFFECT =
        "frontier_town/mushroom_cloud";
}
