//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Properties;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ParticleCache;
import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.client.effect.ParticlePool;

/**
 * An emission that displays transient particle effects when the target sprite
 * reaches configured animation frames.
 */
public class TransientParticleEmission extends FrameEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _effect = props.getProperty("effect");
        _rotate = Boolean.parseBoolean(props.getProperty("rotate"));
    }
    
    @Override // documentation inherited
    public Control putClone (
        Control store, Model.CloneCreator properties)
    {
        TransientParticleEmission tstore;
        if (store == null) {
            tstore = new TransientParticleEmission();
        } else {
            tstore = (TransientParticleEmission)store;
        }
        super.putClone(tstore, properties);
        tstore._effect = _effect;
        tstore._rotate = _rotate;
        return tstore;
    }

    // documentation inherited
    protected void fireEmission ()
    {
        if (_ctx == null) {
            return;
        }
        ParticlePool.getParticles(_effect,
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                result.setLocalTranslation(new Vector3f(
                    _target.getWorldTranslation()));
                if (_rotate) {
                    result.setLocalRotation(_target.getWorldRotation().mult(
                        ParticleCache.Z_UP_ROTATION));
                }
            }
        });
    }

    /** The name of the effect to generate. */
    protected String _effect;

    /** Whether to rotate as well as translate the effect. */
    protected boolean _rotate;
}
