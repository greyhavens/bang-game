//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.Vector3f;

import com.samskivert.util.Interval;

import com.threerings.bang.client.BangPrefs;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.ChainingShotEffect;
import com.threerings.bang.game.data.effect.LightningEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Animates a chained lightning.
 */
public class LightningHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _applying = true;
        _leffect = (LightningEffect)_effect;

        apply(_effect);
        final int penderId = notePender();
        new Interval(_ctx.getClient().getRunQueue()) {
            public void expired () {
                // keep applying the next level until done
                if (!_leffect.apply(
                            _bangobj, LightningHandler.this, _level++)) {
                    cancel();
                    maybeComplete(penderId);
                }
            }
            protected byte _level;
        }.schedule(CHAIN_DELAY, true);
        _applying = false;
        return !isCompleted();
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        super.pieceAffected(piece, effect);
        if (effect.equals(ChainingShotEffect.PRIMARY_EFFECT) &&
            BangPrefs.isMediumDetail()) {
            // fire off a flash of light for the main bolt
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null) {
                Vector3f trans = sprite.getWorldTranslation();
                _view.createLightFlash(
                    new Vector3f(trans.x, trans.y, trans.z + TILE_SIZE),
                    ChainingShotHandler.LIGHT_FLASH_COLOR,
                    ChainingShotHandler.LIGHT_FLASH_DURATION);
            }
        }
    }
    
    LightningEffect _leffect;

    /** The delay in milliseconds between subsequent levels. */
    protected static final long CHAIN_DELAY = 250;
}
