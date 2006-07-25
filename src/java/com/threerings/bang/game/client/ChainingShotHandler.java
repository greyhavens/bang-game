//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.data.effect.ChainingShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Waits for all sprites involved in a shot to stop moving and then animates
 * the fired shot.
 */
public class ChainingShotHandler extends ShotHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _cseffect = (ChainingShotEffect)_effect;
        return super.execute();
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        if (_shot.targetId != piece.pieceId) {
            for (ShotEffect shot : _cseffect.chainShot) {
                if (shot.targetId == piece.pieceId) {
                    _effect = shot;
                    break;
                }
            }
        }
        super.pieceAffected(piece, effect);
        _effect = _cseffect;
    }
        

    protected void fireShot (int sx, int sy, int tx, int ty)
    {
        // play the bang sound
        if (_bangSound != null) {
            _bangSound.play(false);
        }

        apply(_effect);
        maybeComplete(-1);
    }

    ChainingShotEffect _cseffect;
}
