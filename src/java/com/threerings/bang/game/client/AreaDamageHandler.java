//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;

import com.threerings.openal.Sound;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays an area damage effect as a bunch of bombs dropping on the targets.
 */
public class AreaDamageHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        // we first wait for the missile sounds to resolve
        _explodeSound = _sounds.getSound("rsrc/sounds/effects/missile.ogg");
        _whistleSound = _sounds.getSound(
            "rsrc/sounds/effects/bomb_whistle.ogg");
        _whistleSound.play(new Sound.StartObserver() {
            public void soundStarted (Sound sound) {
                // then we create the visualization
                dropBomb();
            }
        }, false);

        return true;
    }

    protected void dropBomb ()
    {
        // drop the bomb at the center
        AreaDamageEffect effect = (AreaDamageEffect)_effect;
        _dest = new Vector3f((effect.x + 0.5f) * TILE_SIZE,
            (effect.y + 0.5f) * TILE_SIZE, 0f);
        _dest.z = _view.getTerrainNode().getHeightfieldHeight(_dest.x,
            _dest.y);
            
        ShotSprite ssprite = new ShotSprite(
            _ctx, "bonuses/frontier_town/missile", null);
        _view.addSprite(ssprite);
        ssprite.getLocalRotation().fromAngleNormalAxis(
            -FastMath.HALF_PI, FORWARD);
        Vector3f start = _dest.add(0f, 0f, BOMB_HEIGHT);
        ssprite.move(new LinePath(ssprite, start, _dest, BOMB_DURATION));
        final int penderId = notePender();
        ssprite.addObserver(new PathObserver() {
            public void pathCompleted (Sprite sprite, Path path) {
                _view.removeSprite(sprite);
                _explodeSound.play(true);
                apply(_effect);
                maybeComplete(penderId);
            }
            public void pathCancelled (Sprite sprite, Path path) {
                _view.removeSprite(sprite);
                apply(_effect);
                maybeComplete(penderId);
            }
        });
    }

    @Override // documentation inherited
    protected void apply (Effect effect)
    {
        ParticlePool.getParticles("frontier_town/mushroom_cloud",
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                result.setLocalTranslation(_dest);
            }
        });
        
        super.apply(effect);
    }    
    
    /** The bomb whistle. */
    protected Sound _whistleSound;

    /** The explosion sound. */
    protected Sound _explodeSound;

    /** The bomb's destination. */
    protected Vector3f _dest;

    /** The height from which the bomb falls. */
    protected static final float BOMB_HEIGHT = 200f;

    /** The duration of the bomb flight in seconds. */
    protected static final float BOMB_DURATION = 1.5f;
}
