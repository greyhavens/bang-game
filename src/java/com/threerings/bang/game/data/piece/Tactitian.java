//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.Iterator;

import com.threerings.util.RandomUtil;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles the special capabilities of the Tactitian unit.
 */
public class Tactitian extends Unit
{
    /** Set to the tick on which the tactitian last fired. */
    public short lastFired = -4;

    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target)
    {
        // note our last fired time
        lastFired = bangobj.tick;
        return super.shoot(bangobj, target);
    }

    @Override // documentation inherited
    public ShotEffect deflect (
        BangObject bangobj, Piece shooter, ShotEffect effect)
    {
        // if it has been less than one one full turn since we fired, our
        // umbrella is "in use" and we don't deflect anything
        if ((bangobj.tick - lastFired) < getTicksPerMove()) {
            return effect;
        }

        // we only deflect fire from range units
        if (!(shooter instanceof Unit)) {
            return effect;
        }
        Unit ushooter = (Unit)shooter;
        if (ushooter.getConfig().mode != UnitConfig.Mode.RANGE) {
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

        log.info("Deflecting shot [ntarget=" + ntarget + ", nx=" + nx +
                 ", ny=" + ny + "].");
        if (ntarget == null) {
            effect.deflectShot(nx, ny);
        } else {
            effect.setTarget(ntarget, shooter.computeScaledDamage(ntarget));
        }

        return effect;
    }

    /** Used to deflect shots. */
    protected static short[] DX = { 0, -1, 0, 1 }, DY = { -1, 0, 1, 0 };
}
