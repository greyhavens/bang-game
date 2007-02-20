//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.effects.particles.ParticleMesh;

import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.client.sprite.PieceSprite;

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
    protected void displayParticles (ParticleMesh particles, boolean position)
    {
        displayParticles(getLocalTranslation(), particles, position);
    }

    /**
     * Displays a particle system for this effect at a location.
     *
     * @param position whether or not to place the particle system at the
     * center of the target
     */
     protected void displayParticles (
        final Vector3f localTranslation, ParticleMesh particles, boolean position)
    {
        // we may be reusing this particle system so remove it from its
        // previous parent
        Node parent = particles.getParent();
        if (parent != null) {
            parent.detachChild(particles);
        }

        // position and fire up the particle system
        if (position) {
            Vector3f spos = localTranslation;
            particles.setLocalTranslation(
                new Vector3f(spos.x, spos.y, spos.z + TILE_SIZE/2));
        }
        _view.getPieceNode().attachChild(particles);
        particles.updateRenderState();
        particles.updateGeometricState(0f, true);
        particles.forceRespawn();
    }

    /**
     * Displays a particle effect on the specified target.
     */
    protected void displayEffect (String name)
    {
        displayEffect(name, getLocalTranslation(), (_target != null) ?
            getTargetSprite().getLocalRotation() : new Quaternion());
    }

    /**
     * Displays a particle effect on the specified location and orientation.
     */
    protected void displayEffect (String name,
        final Vector3f localTranslation, final Quaternion localRotation)
    {
        ParticlePool.getParticles(name,
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                Vector3f trans = result.getLocalTranslation();
                localRotation.multLocal(
                    trans.set(0f, 0f, TILE_SIZE/2));
                trans.addLocal(localTranslation);
            }
        });
    }

    /**
     * Removes a particle system from the view.
     */
    protected void removeParticles (ParticleMesh particles)
    {
        _view.getPieceNode().detachChild(particles);
        particles.getParticleController().setActive(false);
    }
}
