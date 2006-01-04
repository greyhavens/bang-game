//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jmex.effects.ParticleManager;

import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * Displays the effect when a unit is repaired.
 */
public class RepairViz extends ParticleEffectViz
{
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        displayParticleManager(target, _pmgr, true);
        
        // note that the effect was displayed
        effectDisplayed();
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        _pmgr = ParticleFactory.getGlow();
        _pmgr.getParticles().setLocalScale(0.65f);
    }
    
    protected ParticleManager _pmgr;
}
