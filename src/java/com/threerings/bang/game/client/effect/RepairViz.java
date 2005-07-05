//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jmex.effects.ParticleManager;

/**
 * Displays the effect when a unit is repaired.
 */
public class RepairViz extends ParticleEffectViz
{
    protected ParticleManager getParticleManager ()
    {
        ParticleManager pmgr = ParticleFactory.getGlow();
        pmgr.getParticles().setLocalScale(0.65f);
        return pmgr;
    }
}
