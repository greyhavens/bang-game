//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import java.awt.Point;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.LightningHandler;

import com.threerings.io.SimpleStreamableObject;

import static com.threerings.bang.Log.log;

/**
 * Causes damage to units and chains through steam units.
 */
public class LightningEffect extends Effect
{
    public static class ChainDamage extends SimpleStreamableObject
    {
        public int pieceId;
        public int newDamage;
        public byte step;
        public Effect deathEffect;

        public ChainDamage (int pieceId, int newDamage, byte step)
        {
            this.pieceId = pieceId;
            this.newDamage = newDamage;
            this.step = step;
        }

        public ChainDamage ()
        {
        }
    }

    /** The pieces being damaged. */
    public ChainDamage[] chain;

    /** The initial target piece. */
    public int pieceId;

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        ArrayList<ChainDamage> damaged = new ArrayList<ChainDamage>();
        LinkedList<Point> points = new LinkedList<Point>();

        // get a list of all the units
        ArrayList<Piece> targetPieces = new ArrayList<Piece>();
        for (Piece p: bangobj.pieces) {
            if (p.isTargetable() && p.isAlive()) {
                if (p.pieceId == pieceId) {
                    damagePiece(bangobj, damaged, p, 0, dammap);
                    points.add(new Point(p.x, p.y));
                } else {
                    targetPieces.add(p);
                }
            }
        }

        // create the chain damage
        byte step = 1, size = 1;
        while (!points.isEmpty()) {
            Point pt = points.poll();
            for (Iterator<Piece> iter = targetPieces.iterator(); iter.hasNext(); ) {
                Piece p = iter.next();
                if (p.getDistance(pt.x, pt.y) == 1) {
                    damagePiece(bangobj, damaged, p, step, dammap);
                    points.add(new Point(p.x, p.y));
                    iter.remove();
                }
            }
            if (--size == 0) {
                step++;
                size = (byte)points.size();
            }
        }
        chain = damaged.toArray(new ChainDamage[damaged.size()]);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (chain.length > 0);
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] apieces = new int[chain.length];
        for (int ii = 0; ii < chain.length; ii++) {
            apieces[ii] = chain[ii].pieceId;
            if (chain[ii].deathEffect != null) {
                apieces = concatenate(apieces, chain[ii].deathEffect.getAffectedPieces());
            }
        }
        return apieces;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (bangobj.getManager().isManager(bangobj)) {
            for (byte dist = 0; apply(bangobj, obs, dist); dist++);
        }
        return true;
    }

    /**
     * Apply only the damage effects for the provided distance.
     */
    public boolean apply (BangObject bangobj, Observer obs, byte dist)
    {
        boolean remaining = false;
        for (ChainDamage cd : chain) {
            if (cd.step < dist) {
                continue;
            }
            if (cd.step > dist) {
                remaining = true;
                break;
            }
            Piece target = bangobj.pieces.get(cd.pieceId);
            if (target == null) {
                log.warning("Missing piece for lightning effect", "pieceId", cd.pieceId);
                continue;
            }
            if (cd.deathEffect != null) {
                cd.deathEffect.apply(bangobj, obs);
            }
            reportEffect(obs, target, (pieceId == cd.pieceId ?
                        ChainingShotEffect.PRIMARY_EFFECT : ChainingShotEffect.SECONDARY_EFFECT));
            damage(bangobj, obs, -1, null, target, cd.newDamage, ShotEffect.DAMAGED);
        }
        return remaining;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx == -1) {
            return null;
        }
        String names = getPieceNames(bangobj, pidx, getAffectedPieces());
        return (names == null) ?  null : MessageBundle.compose("m.effect_lightning", names);
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new LightningHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        for (ChainDamage cd : chain) {
            if (cd.pieceId == piece.pieceId) {
                return calculateDamage(cd.step);
            }
        }
        return 0;
    }

    /**
     * Helper function that returns the amount of damage for this step.
     */
    protected int calculateDamage (int step)
    {
        int damage = BASE_DAMAGE;
        for (int ii = 0; ii < step; ii++) {
            damage *= DAMAGE_SCALE;
        }
        return damage;
    }

    /**
     * Helper function that calculates the damage to a piece and handles any pesky dying issues.
     */
    protected void damagePiece (BangObject bangobj, ArrayList<ChainDamage> damaged,
            Piece p, int step, IntIntMap dammap)
    {
        int damage = calculateDamage(step);
        damage += p.damage;
        damage = Math.min(damage, 100);
        dammap.increment(p.owner, damage - p.damage);
        ChainDamage cd = new ChainDamage(p.pieceId, damage, (byte)step);
        if (damage == 100) {
            cd.deathEffect = p.willDie(bangobj, -1);
            if (cd.deathEffect != null) {
                cd.deathEffect.prepare(bangobj, dammap);
            }
        }
        damaged.add(cd);
    }

    /** Damage values for the different units. */
    protected static final int BASE_DAMAGE = 40;
    protected static final float DAMAGE_SCALE = 0.7f;
}
