//
// $Id$

package com.threerings.bang.util;

import java.util.ArrayList;

import com.jme3.effect.ParticleEmitter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import com.threerings.jme.util.SpatialVisitor;

/**
 * Methods and classes for manipulating jME3 particle systems.
 *
 * <h3>jME3 cutover (Phase 2, cluster 6 — the particle foundation for Wave-2)</h3>
 *
 * The fork particle system ({@code com.jmex.effects.particles.ParticleGeometry} +
 * {@code ParticleController} + a list of {@code ParticleInfluence}s) is replaced by jME3's
 * {@link ParticleEmitter} (a {@code Geometry} with one {@code ParticleInfluencer}; migration map
 * §2.11). This class defines the manipulation shape the sprite/effect Wave-2 clusters reuse:
 *
 * <ul>
 *   <li><b>"All particle systems under a node"</b> means: every {@link ParticleEmitter} found by a
 *   {@link SpatialVisitor SpatialVisitor&lt;ParticleEmitter&gt;} walk (emitters are {@code Geometry}
 *   subclasses, so they are ordinary spatials).</li>
 *   <li><b>"Stop releasing"</b> = {@code emitter.setParticlesPerSec(0)} (the jME3 analogue of the
 *   fork's clamp-repeat-type-to-stop).</li>
 *   <li><b>"Inactive"</b> = no particles emitting and none still visible:
 *   {@code emitter.getParticlesPerSec() == 0 && emitter.getNumVisibleParticles() == 0}.</li>
 *   <li><b>"Force respawn"</b> = {@code emitter.emitAllParticles()}.</li>
 * </ul>
 *
 * The {@link ParticleRemover} is the fork {@code ParticleController} that detached a finished effect
 * rebuilt as a jME3 {@link AbstractControl}.
 */
public class ParticleUtil
{
    /**
     * Removes effects from their parents when all of their particle systems become inactive.
     *
     * <p>jME3: was a fork {@code Controller}; now an {@link AbstractControl} attached to the target
     * via {@code target.addControl(...)}.
     */
    public static class ParticleRemover extends AbstractControl
    {
        public ParticleRemover (Spatial target)
        {
            _target = target;
            final ArrayList<ParticleEmitter> emitters = new ArrayList<ParticleEmitter>();
            new SpatialVisitor<ParticleEmitter>(ParticleEmitter.class) {
                protected void visit (ParticleEmitter emitter) {
                    emitters.add(emitter);
                }
            }.traverse(_target);
            _emitters = emitters.toArray(new ParticleEmitter[emitters.size()]);
        }

        @Override // documentation inherited
        protected void controlUpdate (float tpf)
        {
            for (ParticleEmitter emitter : _emitters) {
                if (emitter.getParticlesPerSec() > 0f ||
                    emitter.getNumVisibleParticles() > 0) {
                    return;
                }
            }
            Node parent = _target.getParent();
            if (parent != null) {
                parent.detachChild(_target);
            }
        }

        @Override // documentation inherited
        protected void controlRender (RenderManager rm, ViewPort vp)
        {
            // nothing to render
        }

        /** The target effect. */
        protected Spatial _target;

        /** All of the particle emitters in the effect. */
        protected ParticleEmitter[] _emitters;
    }

    /**
     * Forces a respawn on all particle systems under the given node.
     */
    public static void forceRespawn (Spatial spatial)
    {
        _respawner.traverse(spatial);
    }

    /**
     * Stops all particle systems under the given node and removes it from its parent when it
     * becomes inactive (i.e., when all existing particles have died).
     */
    public static void stopAndRemove (Spatial spatial)
    {
        stopReleasing(spatial);
        spatial.addControl(new ParticleRemover(spatial));
    }

    /**
     * Sets the release rates of all particle systems under the given node to zero.
     */
    public static void stopReleasing (Spatial spatial)
    {
        _stopper.traverse(spatial);
    }

    /** Forces a respawn on all traversed particle systems. */
    protected static SpatialVisitor<ParticleEmitter> _respawner =
        new SpatialVisitor<ParticleEmitter>(ParticleEmitter.class) {
        protected void visit (ParticleEmitter emitter) {
            emitter.emitAllParticles();
        }
    };

    /** Stops all traversed particle systems releasing new particles. */
    protected static SpatialVisitor<ParticleEmitter> _stopper =
        new SpatialVisitor<ParticleEmitter>(ParticleEmitter.class) {
        protected void visit (ParticleEmitter emitter) {
            emitter.setParticlesPerSec(0f);
        }
    };
}
