//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Properties;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

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
        // jME3: fork ParticleFactory/ParticleMesh -> ParticleEmitter; fork lifetimes are ms,
        // jME3 LowLife/HighLife are seconds (Phase-4 tunes the exact curve / spin / spread).
        _smoke = new ParticleEmitter("smoke", Type.Triangle, 64);
        _smoke.setLowLife(_lifetime / 1000f);
        _smoke.setHighLife(_lifetime * 1.5f / 1000f);
        _smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 0f, _velocity));
        _smoke.getParticleInfluencer().setVelocityVariation(0.05f);
        _smoke.setParticlesPerSec(0f);
        _smoke.setStartSize(_startSize);
        _smoke.setEndSize(_endSize);
        _smoke.setStartColor(_startColor);
        _smoke.setEndColor(_endColor);
        applySmokeMaterial();
        _smoke.emitAllParticles();

        model.getEmissionNode().attachChild(_smoke);

        super.init(model);
    }

    /** Applies the smoke particle material (textured if resolved). */
    protected void applySmokeMaterial ()
    {
        if (_smoke == null || _ctx == null) {
            return;
        }
        Material mat = new Material(_ctx.getAssetManager(),
            "Common/MatDefs/Misc/Particle.j3md");
        if (_smoketex != null) {
            mat.setTexture("Texture", _smoketex);
        }
        RenderUtil.applyBlendAlpha(mat);
        RenderUtil.applyOverlayZBuf(mat);
        _smoke.setMaterial(mat);
        RenderUtil.setOverlay(_smoke);
    }
    
    @Override // documentation inherited
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.setSpriteRefs(ctx, view, sprite);
        if (_smoke != null) {
            view.addWindInfluence(_smoke);
            // _ctx is now set, so (re)build the material so it is textured.
            applySmokeMaterial();
        }
    }

    @Override // documentation inherited
    public void setActive (boolean active)
    {
        super.setActive(active);
        if (_smoke != null) {
            _smoke.setParticlesPerSec(active ? _releaseRate : 0f);
        }
    }

    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        // effect texture loaded by path; load from the cache.
        if (_smoketex == null && _ctx != null) {
            _smoketex = _ctx.getTextureCache().getTexture("textures/effects/dust.png");
            _smoketex.setWrap(WrapMode.Clamp);
        }
        applySmokeMaterial();
    }

    @Override // documentation inherited
    public Control putClone (
        Control store, Model.CloneCreator properties)
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

    // documentation inherited
    public void update (float time)
    {
        if (!isActive() || _smoke == null) {
            return;
        }
        _smoke.setLocalTranslation(new Vector3f(_target.getWorldTranslation()));
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
    protected ParticleEmitter _smoke;

    /** The smoke texture. */
    protected static Texture _smoketex;
}
