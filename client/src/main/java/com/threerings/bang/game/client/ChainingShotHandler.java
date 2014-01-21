//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

import com.samskivert.util.Interval;

import com.threerings.bang.client.BangPrefs;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.ChainingShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then animates
 * the fired shot.
 */
public class ChainingShotHandler extends ShotHandler
{
    /** The color of the light flash to show for the main bolt. */
    public static final ColorRGBA LIGHT_FLASH_COLOR =
        new ColorRGBA(0.9f, 1f, 1f, 1f);
    
    /** The duration of the light flash. */
    public static final float LIGHT_FLASH_DURATION = 0.25f;
    
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
        if (effect.equals(ChainingShotEffect.PRIMARY_EFFECT) &&
            BangPrefs.isMediumDetail()) {
            // fire off a flash of light for the main bolt
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null) {
                Vector3f trans = sprite.getWorldTranslation();
                _view.createLightFlash(
                    new Vector3f(trans.x, trans.y, trans.z + TILE_SIZE),
                    LIGHT_FLASH_COLOR, LIGHT_FLASH_DURATION);
            }
        }
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
