//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.effects.ParticleManager;

import com.threerings.bang.game.client.sprite.PieceSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A base class for effect visualizations that use particles.
 */
public abstract class ParticleEffectViz extends EffectViz
{
    /**
     * Displays a particle manager for this effect.
     *
     * @param position whether or not to place the particle manager at the
     * center of the target
     */
    protected void displayParticleManager (PieceSprite target,
        ParticleManager pmgr, boolean position)
    {
        // we may be reusing this particle manager so remove it from its
        // previous parent
        Node parent = pmgr.getParticles().getParent();
        if (parent != null) {
            parent.detachChild(pmgr.getParticles());
        }
        
        // position and fire up the particle manager
        if (position) {
            Vector3f spos = target.getLocalTranslation();
            pmgr.getParticles().setLocalTranslation(
                new Vector3f(spos.x, spos.y, spos.z + TILE_SIZE/2));
        }
        pmgr.forceRespawn();
        _view.getPieceNode().attachChild(pmgr.getParticles());
        pmgr.getParticles().setCullMode(Node.CULL_NEVER);
    }
    
    /**
     * Removes a particle manager from the view.
     */
    protected void removeParticleManager (ParticleManager pmgr)
    {
        _view.getPieceNode().detachChild(pmgr.getParticles());
    }
}
