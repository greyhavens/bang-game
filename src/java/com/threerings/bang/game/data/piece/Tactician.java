//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.Iterator;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.BallisticShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles the special capabilities of the Tactician unit.
 */
public class Tactician extends Unit
{
    /** Set to the tick on which the tactician last fired. */
    public short lastFired = -4;

    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        // note our last fired time
        lastFired = bangobj.tick;
        return super.shoot(bangobj, target, scale);
    }

    @Override // documentation inherited
    public ShotEffect deflect (BangObject bangobj, Piece shooter,
                               ShotEffect effect, float scale)
    {
        // we only deflect fire from range units
        if (!(shooter instanceof Unit) || !effect.isDeflectable()) {
            return effect;
        }

        // don't let a shot bounce around forever
        int lidx = effect.xcoords.length-1;
        if (lidx > 4) {
            return effect;
        }

        // randomly pick a direction in which to deflect the shot
        int ddir = RandomUtil.getInt(4);
        short nx = (short)(effect.xcoords[lidx] + DX[ddir]);
        short ny = (short)(effect.ycoords[lidx] + DY[ddir]);

        // check to see if there is a piece at these coordinates
        Piece ntarget = null;
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.x == nx && piece.y == ny) {
                ntarget = piece;
                break;
            }
        }
        // make sure the target is valid, otherwise we just fizzle out at
        // the specified coordinates
        if (ntarget != null && !shooter.validTarget(ntarget, true)) {
            ntarget = null;
        }

        log.fine("Deflecting shot [ntarget=" + ntarget + ", nx=" + nx +
                 ", ny=" + ny + "].");
        if (ntarget == null) {
            effect.deflectShot(nx, ny);
        } else {
            int damage = shooter.computeScaledDamage(bangobj, ntarget, scale);
            effect.setTarget(ntarget, damage,
                    shooter.attackInfluenceIcon(),
                    shooter.defendInfluenceIcon(ntarget));
        }

        return effect;
    }

    /** Used to deflect shots. */
    protected static short[] DX = { 0, -1, 0, 1 }, DY = { -1, 0, 1, 0 };
}
