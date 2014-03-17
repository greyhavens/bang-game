//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import java.awt.Point;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.ChainingShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles special custom behavior for the Storm Caller.
 */
public class StormCaller extends Unit
{
    @Override // documentation inherited
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        int damage = computeScaledDamage(bangobj, target, scale);
        ChainingShotEffect effect = new ChainingShotEffect(this, target, 
                damage, attackInfluenceIcons(), defendInfluenceIcons(target));
        
        // Get a list of all targetable pieces within a certain distance
        // so we don't need to search through the entire piece list each time
        ArrayList<Piece> targetPieces = new ArrayList<Piece>();
        for (Piece p : bangobj.pieces) {
            if (validTarget(bangobj, p, true) &&
                target.getDistance(p) <= MAX_CHAIN &&
                    p.pieceId != target.pieceId) {
                targetPieces.add(p);
            }
        }

        // The attack is chained to adjacent units
        int dist = 1, size = 1;
        LinkedList<Point> points = new LinkedList<Point>();
        points.add(new Point(target.x, target.y));
        ArrayList<ShotEffect> chainedShots = new ArrayList<ShotEffect>();
        while (!points.isEmpty()) {
            Point pt = points.poll();
            for (Iterator<Piece> iter = targetPieces.iterator(); 
                    iter.hasNext(); ) {
                Piece p = iter.next();
                if (p.getDistance(pt.x, pt.y) == 1) {
                    iter.remove();
                    if (p.getDistance(target) < MAX_CHAIN) {
                        points.add(new Point(p.x, p.y));
                    }
                    damage = computeScaledDamage(bangobj, p, 
                            scale*DAMAGE_SCALE[dist]);
                    chainedShots.add(generateShotEffect(bangobj, p, damage));
                }
            }
            if (--size == 0) {
                dist++;
                size = points.size();
            }
        }
        effect.chainShot = chainedShots.toArray(new ShotEffect[0]);
        return target.deflect(bangobj, this, effect, scale);
    }

    protected static final int MAX_CHAIN = 2;
    protected static final float[] DAMAGE_SCALE = {1f, 0.8f, 0.5f};
}
