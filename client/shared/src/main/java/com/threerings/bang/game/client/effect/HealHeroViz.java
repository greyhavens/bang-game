//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.effect.ParticleEmitter;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the effect when a unit is repaired.
 */
public class HealHeroViz extends ParticleEffectViz
{
    @Override // documentation inherited
    public void display ()
    {
        // and the swirl effect
        displayParticles(_swirls[0].particles, true);
        displayParticles(_swirls[1].particles, true);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        _swirls = new Swirl[] { new Swirl(0f), new Swirl(FastMath.PI) };
    }

    /** The swirl of sparkles effect. */
    protected class Swirl
    {
        /** The particle emitter for the swirl. */
        public ParticleEmitter particles;

        public Swirl (final float a0)
        {
            particles = ParticlePool.getSparkles();
            particles.setParticlesPerSec(512f);
            particles.setLocalTranslation(new Vector3f());

            // jME3: the per-frame fork Controller becomes an AbstractControl on the emitter.
            particles.addControl(new AbstractControl() {
                @Override protected void controlUpdate (float time) {
                    // remove swirl if its lifespan has elapsed
                    if ((_elapsed += time) > TOTAL_DURATION) {
                        particles.removeControl(this);
                        removeParticles(particles);
                        if (!_displayed) { // report completion
                            effectDisplayed();
                            _displayed = true;
                        }
                        return;

                    } else if (_elapsed > SWIRL_DURATION) {
                        particles.setParticlesPerSec(0f);
                        return;
                    }
                    float t = _elapsed / SWIRL_DURATION,
                        radius = TILE_SIZE / 3,
                        angle = a0 + t * FastMath.TWO_PI * SWIRL_REVOLUTIONS;
                    particles.setLocalTranslation(
                        radius * FastMath.cos(angle),
                        radius * FastMath.sin(angle),
                        TILE_SIZE * t - radius);
                }
                @Override protected void controlRender (RenderManager rm, ViewPort vp) {
                }
                protected float _elapsed;
            });
        }
    }

    /** The swirls of sparkles. */
    protected Swirl[] _swirls;

    /** Whether or not we have reported ourself as displayed. */
    protected boolean _displayed;

    /** The duration of the glow effect. */
    protected static final float TOTAL_DURATION = 1f;

    /** The duration of the swirl effect. */
    protected static final float SWIRL_DURATION = 0.75f;

    /** The number of revolutions for the swirl to complete. */
    protected static final float SWIRL_REVOLUTIONS = 2f;
}
