//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.Sound;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays an area damage effect as a bunch of bombs dropping on the targets.
 */
public class AreaDamageHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        AreaDamageEffect effect = (AreaDamageEffect)_effect;
        
        // load and start the whistle sound
        _whistleSound = _sounds.getSound(
            "rsrc/sounds/effects/bomb_whistle.wav");
        _whistleSound.play(false);
        
        // create a missile for each piece, scaled according to distance from
        // the center
        for (int ii = 0; ii < effect.pieces.length; ii++) {
            ShotSprite ssprite = new ShotSprite(_ctx);
            Piece target = (Piece)_bangobj.pieces.get(
                Integer.valueOf(effect.pieces[ii]));
            if (target == null) {
                log.warning("Missing piece for damage target [pieceId=" +
                    effect.pieces[ii] + "].");
                continue;
            }
            _view.addSprite(ssprite);
            ssprite.setLocalScale(FastMath.pow(0.5f,
                target.getDistance(effect.x, effect.y)));
            ssprite.getLocalRotation().fromAngleNormalAxis(-FastMath.HALF_PI,
                FORWARD);
            Vector3f end = _view.getPieceSprite(target).getLocalTranslation(),
                start = end.add(0f, 0f, BOMB_HEIGHT);
            ssprite.move(new LinePath(ssprite, start, end, BOMB_DURATION));
            final int penderId = notePender();
            ssprite.addObserver(new PathObserver() {
                public void pathCompleted (Sprite sprite, Path path) {
                    _view.removeSprite(sprite);
                    maybeComplete(penderId);
                }
                public void pathCancelled (Sprite sprite, Path path) {
                    _view.removeSprite(sprite);
                    maybeComplete(penderId);
                }
            });        
        }
        return true;
    }
    
    @Override // documentation inherited
    protected void maybeComplete (int penderId)
    {
        // when the last bomb finishes its path, start the explosions
        if (_penders.size() == 1 && !_applied) {
            _applied = true;
            _effect.apply(_bangobj, this);
        }
        super.maybeComplete(penderId);
    }
    
    /** The bomb whistle. */
    protected Sound _whistleSound;
    
    /** Whether or not the effect has been applied. */
    protected boolean _applied;
    
    /** The height from which the bombs fall. */
    protected static final float BOMB_HEIGHT = 200f;
    
    /** The duration of the bomb flight in seconds. */
    protected static final float BOMB_DURATION = 1.5f;
}
