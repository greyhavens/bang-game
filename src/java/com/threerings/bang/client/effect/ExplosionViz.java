//
// $Id$

package com.threerings.bang.client.effect;

import com.jmex.effects.ParticleManager;

import com.threerings.bang.client.ParticleFactory;

/**
 * Displays an explosion.
 */
public class ExplosionViz extends ParticleEffectViz
{
    public ExplosionViz (boolean small)
    {
        _small = small;
    }

    protected ParticleManager getParticleManager ()
    {
        ParticleManager pmgr = _small ? ParticleFactory.getSmallExplosion() :
            ParticleFactory.getExplosion();
        pmgr.getParticles().setLocalScale(0.65f);
        return pmgr;
    }

    protected boolean _small;
}
