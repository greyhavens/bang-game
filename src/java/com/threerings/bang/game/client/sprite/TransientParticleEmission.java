//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Properties;

import com.jme.scene.Controller;
import com.jme.scene.Spatial;

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
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
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
    
    // documentation inherited from interface Externalizable
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_effect);
        out.writeBoolean(_rotate);
    }
    
    // documentation inherited from interface Externalizable
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        _effect = (String)in.readObject();
        _rotate = in.readBoolean();
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
                result.getLocalTranslation().set(
                    _target.getWorldTranslation());
                if (_rotate) {
                    _target.getWorldRotation().mult(
                        ParticleCache.Z_UP_ROTATION,
                        result.getLocalRotation());
                }
            }
        });
    }
    
    /** The name of the effect to generate. */
    protected String _effect;
    
    /** Whether to rotate as well as translate the effect. */
    protected boolean _rotate;
    
    private static final long serialVersionUID = 1;
}
