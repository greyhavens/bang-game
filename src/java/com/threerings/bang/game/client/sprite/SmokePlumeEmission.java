//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Properties;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;
import com.jme.renderer.CloneCreator;
import com.jme.renderer.ColorRGBA;

import com.jmex.effects.ParticleManager;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * A plume of smoke represented by a particle system.
 */
public class SmokePlumeEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _startColor = parseColor(props.getProperty("start_color",
            "0.1, 0.1, 0.1, 0.75"));
        _endColor = parseColor(props.getProperty("end_color",
            "0.5, 0.5, 0.5, 0"));
        _startSize = Float.valueOf(props.getProperty("start_size", "1.25"));
        _endSize = Float.valueOf(props.getProperty("end_size", "5"));
        _releaseRate = Integer.valueOf(props.getProperty("release_rate",
            "12"));
        _velocity = Float.valueOf(props.getProperty("velocity", "0.005"));
        _lifetime = Float.valueOf(props.getProperty("lifetime", "4000"));
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        _smokemgr = new ParticleManager(64);
        _smokemgr.setParticlesMinimumLifeTime(_lifetime);
        _smokemgr.setInitialVelocity(_velocity);
        _smokemgr.setParticlesOrigin(new Vector3f());
        _smokemgr.setEmissionDirection(Vector3f.UNIT_Z);
        _smokemgr.setEmissionMaximumAngle(FastMath.PI / 64);
        _smokemgr.setRandomMod(0f);
        _smokemgr.setPrecision(FastMath.FLT_EPSILON);
        _smokemgr.setControlFlow(true);
        _smokemgr.setReleaseRate(0);
        _smokemgr.setReleaseVariance(0f);
        _smokemgr.setParticleSpinSpeed(0.01f);
        _smokemgr.setStartSize(_startSize);
        _smokemgr.setEndSize(_endSize);
        _smokemgr.setStartColor(_startColor);
        _smokemgr.setEndColor(_endColor);
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        _smokemgr.getParticles().setRenderState(RenderUtil.blendAlpha);
        _smokemgr.getParticles().setRenderState(RenderUtil.overlayZBuf);
        _smokemgr.getParticles().updateRenderState();
        _smokemgr.getParticles().addController(_smokemgr);
        _smokemgr.forceRespawn();
        
        model.getEmissionNode().attachChild(_smokemgr.getParticles());
        
        super.init(model);
    }
    
    @Override // documentation inherited
    public void setActive (boolean active)
    {
        super.setActive(active);
        _smokemgr.setReleaseRate(active ? _releaseRate : 0);
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_smoketex == null) {
            _smoketex = tprov.getTexture("/textures/effects/dust.png");
        }
        _smokemgr.getParticles().setRenderState(_smoketex);
    }
    
    @Override // documentation inherited
    public Controller putClone (Controller store, CloneCreator properties)
    {
        SmokePlumeEmission spstore;
        if (store == null) {
            spstore = new SmokePlumeEmission();
        } else {
            spstore = (SmokePlumeEmission)store;
        }
        super.putClone(spstore, properties);
        spstore._startColor = _startColor;
        spstore._endColor = _endColor;
        spstore._startSize = _startSize;
        spstore._endSize = _endSize;
        spstore._releaseRate = _releaseRate;
        spstore._velocity = _velocity;
        spstore._lifetime = _lifetime;
        return spstore;
    }
    
    // documentation inherited from interface Externalizable
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_startColor);
        out.writeObject(_endColor);
        out.writeFloat(_startSize);
        out.writeFloat(_endSize);
        out.writeInt(_releaseRate);
        out.writeFloat(_velocity);
        out.writeFloat(_lifetime);
    }
    
    // documentation inherited from interface Externalizable
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        _startColor = (ColorRGBA)in.readObject();
        _endColor = (ColorRGBA)in.readObject();
        _startSize = in.readFloat();
        _endSize = in.readFloat();
        _releaseRate = in.readInt();
        _velocity = in.readFloat();
        _lifetime = in.readFloat();
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive()) {
            return;
        }
        getEmitterLocation(_smokemgr.getParticlesOrigin());
    }
    
    /**
     * Parses the given string as a three or four component floating point
     * color value.
     */
    protected static ColorRGBA parseColor (String value)
    {
        float[] vals = StringUtil.parseFloatArray(value);
        if (vals == null || vals.length < 3) {
            log.warning("Invalid color value [value=" + value + "].");
            return null;
        }
        return new ColorRGBA(vals[0], vals[1], vals[2],
            (vals.length == 3) ? 1f : vals[3]);
    }
    
    /** The color of the smoke plume at its bottom and top. */
    protected ColorRGBA _startColor, _endColor;
    
    /** The width of the smoke plume at its bottom and top. */
    protected float _startSize, _endSize;
    
    /** The release rate of the smoke puffs. */
    protected int _releaseRate;
    
    /** The upward velocity of the smoke puffs. */
    protected float _velocity;
    
    /** The lifetime of the smoke puffs. */
    protected float _lifetime;
    
    /** The smoke plume particle system. */
    protected ParticleManager _smokemgr;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    private static final long serialVersionUID = 1;
}
