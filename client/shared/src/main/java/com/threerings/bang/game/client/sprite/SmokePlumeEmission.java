//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;

import java.util.Properties;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;
import com.jme.renderer.ColorRGBA;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.Log.*;

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
        if (!BangPrefs.isHighDetail()) {
            super.init(model);
            return;
        }
        _smoke = ParticleFactory.buildParticles("smoke", 64);
        _smoke.setMinimumLifeTime(_lifetime);
        _smoke.setMaximumLifeTime(_lifetime * 1.5f);
        _smoke.setInitialVelocity(_velocity);
        _smoke.setOriginOffset(new Vector3f());
        _smoke.setEmissionDirection(Vector3f.UNIT_Z);
        _smoke.setMaximumAngle(FastMath.PI / 64);
        _smoke.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _smoke.getParticleController().setControlFlow(true);
        _smoke.setReleaseRate(0);
        _smoke.getParticleController().setReleaseVariance(0f);
        _smoke.setParticleSpinSpeed(0.01f);
        _smoke.setStartSize(_startSize);
        _smoke.setEndSize(_endSize);
        _smoke.setStartColor(_startColor);
        _smoke.setEndColor(_endColor);
        _smoke.setModelBound(new BoundingBox());
        _smoke.setIsCollidable(false);
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        if (_smoketex != null) {
            _smoke.setRenderState(_smoketex);
        }
        _smoke.setRenderState(RenderUtil.blendAlpha);
        _smoke.setRenderState(RenderUtil.overlayZBuf);
        _smoke.forceRespawn();
        
        model.getEmissionNode().attachChild(_smoke);
        _smoke.updateRenderState();
        
        super.init(model);
    }
    
    @Override // documentation inherited
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.setSpriteRefs(ctx, view, sprite);
        view.addWindInfluence(_smoke);
    }
    
    @Override // documentation inherited
    public void setActive (boolean active)
    {
        super.setActive(active);
        if (_smoke != null) {
            _smoke.setReleaseRate(active ? _releaseRate : 0);
        }
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_smoketex == null) {
            _smoketex = tprov.getTexture("/textures/effects/dust.png");
            _smoketex.getTexture().setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        }
        if (_smoke != null) {
            _smoke.setRenderState(_smoketex);
        }
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
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
    
    @Override // documentation inherited
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _startColor = (ColorRGBA)capsule.readSavable("startColor", null);
        _endColor = (ColorRGBA)capsule.readSavable("endColor", null);
        _startSize = capsule.readFloat("startSize", 0f);
        _endSize = capsule.readFloat("endSize", 0f);
        _releaseRate = capsule.readInt("releaseRate", 0);
        _velocity = capsule.readFloat("velocity", 0f);
        _lifetime = capsule.readFloat("lifetime", 0f);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_startColor, "startColor", null);
        capsule.write(_endColor, "endColor", null);
        capsule.write(_startSize, "startSize", 0f);
        capsule.write(_endSize, "endSize", 0f);
        capsule.write(_releaseRate, "releaseRate", 0);
        capsule.write(_velocity, "velocity", 0f);
        capsule.write(_lifetime, "lifetime", 0f);
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive() || _smoke == null) {
            return;
        }
        _smoke.getLocalTranslation().set(_target.getWorldTranslation());
    }
    
    /**
     * Parses the given string as a three or four component floating point
     * color value.
     */
    protected static ColorRGBA parseColor (String value)
    {
        float[] vals = StringUtil.parseFloatArray(value);
        if (vals == null || vals.length < 3) {
            log.warning("Invalid color value", "value", value);
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
    protected ParticleMesh _smoke;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    private static final long serialVersionUID = 1;
}
