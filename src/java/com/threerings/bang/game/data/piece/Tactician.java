//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles the special capabilities of the Tactician unit.
 */
public class Tactician extends Unit
{
    @Override // documentation inherited
    public boolean willDeflect (BangObject bangobj, Piece shooter)
    {
        ShotEffect effect = shooter.generateShotEffect(bangobj, this, 1);
        return willDeflectEffect(shooter, effect);
    }

    @Override // documentation inherited
    public ShotEffect deflect (BangObject bangobj, Piece shooter,
                               ShotEffect effect, float scale)
    {
        // we only deflect fire from range units
        if (!willDeflectEffect(shooter, effect)) {
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
        for (Piece piece : bangobj.pieces) {
            if (piece.x == nx && piece.y == ny) {
                ntarget = piece;
                break;
            }
        }
        // make sure the target is valid, otherwise we just fizzle out at
        // the specified coordinates
        if (ntarget != null && !shooter.validTarget(bangobj, ntarget, true)) {
            ntarget = null;
        }

        log.debug("Deflecting shot", "ntarget", ntarget, "nx", nx, "ny", ny);
        if (ntarget == null) {
            effect.deflectShot(nx, ny);
        } else {
            int damage = shooter.computeScaledDamage(bangobj, ntarget, scale);
            effect.setTarget(ntarget, damage,
                    shooter.attackInfluenceIcons(),
                    shooter.defendInfluenceIcons(ntarget));
        }

        return effect;
    }

    /**
     * Returns true if the shot will be deflected.
     */
    protected boolean willDeflectEffect (Piece shooter, ShotEffect effect)
    {
        return ((shooter instanceof Unit) && effect.isDeflectable());
    }

    /** Used to deflect shots. */
    protected static short[] DX = { 0, -1, 0, 1 }, DY = { -1, 0, 1, 0 };
}
