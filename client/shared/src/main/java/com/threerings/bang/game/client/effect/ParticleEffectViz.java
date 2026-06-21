//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.effect.ParticleEmitter;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.threerings.bang.client.util.ResultAttacher;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A base class for effect visualizations that use particles.
 */
public abstract class ParticleEffectViz extends EffectViz
{
    /**
     * Displays a particle system for this effect.
     *
     * @param position whether or not to place the particle system at the
     * center of the target
     */
    protected void displayParticles (ParticleEmitter particles, boolean position)
    {
        displayParticles(getPosition(), particles, position);
    }

    /**
     * Displays a particle system for this effect at a location.
     *
     * @param position whether or not to place the particle system at the
     * center of the target
     */
     protected void displayParticles (
        final Vector3f pos, ParticleEmitter particles, boolean position)
    {
        // we may be reusing this particle system so remove it from its
        // previous parent
        Node parent = particles.getParent();
        if (parent != null) {
            parent.detachChild(particles);
        }

        // position and fire up the particle system
        if (position) {
            particles.setLocalTranslation(new Vector3f(pos.x, pos.y, pos.z + TILE_SIZE/2));
        }
        _view.getPieceNode().attachChild(particles);
        // jME3 refreshes material/light state during its update pass; the fork's explicit
        // updateRenderState() is gone.
        particles.updateGeometricState();
        particles.emitAllParticles();
    }

    /**
     * Displays a particle effect on the specified target.
     */
    protected void displayEffect (String name)
    {
        displayEffect(name, getPosition(), (_sprite != null) ?
            _sprite.getLocalRotation() : new Quaternion());
    }

    /**
     * Displays a particle effect on the specified location and orientation.
     */
    protected void displayEffect (String name,
        final Vector3f pos, final Quaternion localRotation)
    {
        ParticlePool.getParticles(name,
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                Vector3f trans = result.getLocalTranslation();
                localRotation.multLocal(trans.set(0f, 0f, TILE_SIZE/2));
                trans.addLocal(pos);
            }
        });
    }

    /**
     * Removes a particle system from the view.
     */
    protected void removeParticles (ParticleEmitter particles)
    {
        _view.getPieceNode().detachChild(particles);
        ParticlePool.setActive(particles, false);
    }
}
