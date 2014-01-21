//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.data.effect.ProximityShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Waits for all sprites involved in a shot to stop moving and then animates
 * the fired shot.
 */
public class ProximityShotHandler extends ShotHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _pseffect = (ProximityShotEffect)_effect;
        return super.execute();
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        if (_shot.targetId != piece.pieceId) {
            for (ShotEffect shot : _pseffect.proxShot) {
                if (shot.targetId == piece.pieceId) {
                    _effect = shot;
                    break;
                }
            }
        }
        super.pieceAffected(piece, effect);
        _effect = _pseffect;
    }

    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        // play the bang sounds
        playSounds(_bangSounds, true);

        apply(_effect);
        maybeComplete(-1);
    }

    protected ProximityShotEffect _pseffect;
}
