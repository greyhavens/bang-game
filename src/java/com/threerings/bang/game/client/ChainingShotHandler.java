//
// $Id$

package com.threerings.bang.game.client;

import com.samskivert.util.Interval;

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
        final int pender = notePender();
        // schedule an interval for the chain levels, firing the
        // first off immediately
        new Interval(_ctx.getClient().getRunQueue()) {
            public void expired () {
                // play the bang sounds
                playSounds(_bangSounds, true);
                
                // keep applying the next level until done
                if (!_cseffect.apply(_bangobj, ChainingShotHandler.this,
                        _level++)) {
                    cancel();
                    maybeComplete(pender);
                }
            }
            protected int _level;
            
        }.schedule(SHOT_DELAY, CHAIN_DELAY);
    }

    protected ChainingShotEffect _cseffect;
    
    /** The delay in milliseconds before the first shot. */
    protected static final long SHOT_DELAY = 750;
    
    /** The delay in milliseconds between subsequent levels. */
    protected static final long CHAIN_DELAY = 250;
}
