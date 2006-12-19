//
// $Id$

package com.threerings.bang.util;

import java.util.ArrayList;

import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleGeometry;

import com.threerings.jme.util.SpatialVisitor;

/**
 * Methods and classes for manipulating JME particle systems.
 */
public class ParticleUtil
{
    /**
     * Removes effects from their parents when all of their particle systems
     * become inactive.
     */
    public static class ParticleRemover extends Controller
    {
        public ParticleRemover (Spatial target)
        {
            _target = target;
            final ArrayList<ParticleController> pctrls =
                new ArrayList<ParticleController>();
            new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
                protected void visit (ParticleGeometry geom) {
                    pctrls.add(geom.getParticleController());
                }
            }.traverse(_target);
            
            _pctrls = pctrls.toArray(new ParticleController[pctrls.size()]);
        }
        
        // documentation inherited
        public void update (float time)
        {
            for (ParticleController pctrl : _pctrls) {
                if (pctrl.isActive()) {
                    return;
                }
            }
            Node parent = _target.getParent();
            if (parent != null) {
                parent.detachChild(_target);
            }
        }
        
        /** The target effect. */
        protected Spatial _target;
        
        /** All of the particle controllers in the effect. */
        protected ParticleController[] _pctrls;
    }
    
    /**
     * Forces a respawn on all particle systems under the given node.
     */
    public static void forceRespawn (Spatial spatial)
    {
        _respawner.traverse(spatial);
    }
    
    /**
     * Stops all particle systems under the given node and removes it from its
     * parent when it becomes inactive (i.e., when all existing particles have
     * died).
     */
    public static void stopAndRemove (Spatial spatial)
    {
        stopReleasing(spatial);
        spatial.addController(new ParticleRemover(spatial));
    }
    
    /**
     * Sets the release rates of all particle systems under the given node to
     * zero.
     */
    public static void stopReleasing (Spatial spatial)
    {
        _stopper.traverse(spatial);
    }
    
    /** Forces a respawn on all traversed particle systems. */
    protected static SpatialVisitor<ParticleGeometry> _respawner =
        new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
        protected void visit (ParticleGeometry geom) {
            geom.forceRespawn();
        }
    };
    
    /** Stops all traversed particle systems. */
    protected static SpatialVisitor<ParticleGeometry> _stopper =
        new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
        protected void visit (ParticleGeometry geom) {
            geom.setRepeatType(Controller.RT_CLAMP);
        }
    };
}
