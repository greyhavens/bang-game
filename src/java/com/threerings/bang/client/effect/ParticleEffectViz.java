//
// $Id$

package com.threerings.bang.client.effect;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.effects.ParticleManager;

import com.threerings.bang.client.sprite.PieceSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A base class for effect visualizations that use particles.
 */
public abstract class ParticleEffectViz extends EffectViz
{
    /**
     * The effect should create the appropriate particle manager in this
     * method.
     */
    protected abstract ParticleManager getParticleManager ();

    @Override // documentation inherited
    protected void didInit ()
    {
        _pmgr = getParticleManager();
    }

    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // we may be reusing this particle manager so remove it from its
        // previous parent
        Node parent = _pmgr.getParticles().getParent();
        if (parent != null) {
            parent.detachChild(_pmgr.getParticles());
        }

        // position and fire up the particle manager
        Vector3f spos = target.getLocalTranslation();
        _pmgr.getParticles().setLocalTranslation(
            new Vector3f(spos.x, spos.y, spos.z + TILE_SIZE/2));
        _pmgr.forceRespawn();
        _view.getPieceNode().attachChild(_pmgr.getParticles());
        _pmgr.getParticles().setForceView(true);

        // finally note that the effect was displayed
        effectDisplayed();
    }

    protected ParticleManager _pmgr;
}
