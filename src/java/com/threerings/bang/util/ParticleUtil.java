//
// $Id$

package com.threerings.bang.util;

import java.util.ArrayList;

import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleGeometry;

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
            ArrayList<ParticleController> pctrls =
                new ArrayList<ParticleController>();
            addParticleControllers(target, pctrls);
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
            _target.getParent().detachChild(_target);
        }
        
        public void addParticleControllers (
            Spatial spatial, ArrayList<ParticleController> pctrls)
        {
            if (spatial instanceof ParticleGeometry) {
                ParticleGeometry pgeom = (ParticleGeometry)spatial;
                pctrls.add(pgeom.getParticleController());
                
            } else if (spatial instanceof Node) {
                Node node = (Node)spatial;
                for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                    addParticleControllers(node.getChild(ii), pctrls);
                }
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
        if (spatial instanceof ParticleGeometry) {
            ((ParticleGeometry)spatial).forceRespawn();
            
        } else if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                forceRespawn(node.getChild(ii));
            }
        }
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
        if (spatial instanceof ParticleGeometry) {
            ((ParticleGeometry)spatial).setRepeatType(Controller.RT_CLAMP);
            
        } else if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                stopReleasing(node.getChild(ii));
            }
        }
    }
}
