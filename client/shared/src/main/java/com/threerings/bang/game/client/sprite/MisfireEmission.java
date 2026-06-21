//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.HashMap;
import java.util.Properties;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.RenderUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

/**
 * A misfire effect which consists of a large puff of black smoke.
 */
public class MisfireEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        if (_animations == null) {
            return;
        }
        _animShotFrame = new HashMap<String, Integer>();
        for (String anim : _animations) {
            _animShotFrame.put(anim, Integer.valueOf(
                        props.getProperty(anim + ".shot_frame", "-1")));
        }
        _size = Float.valueOf(props.getProperty("size", "1"));
    }

    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        _model = model;
        setActiveEmission(false);

        if (!BangPrefs.isHighDetail()) {
            return;
        }
        // jME3: fork ParticleMesh -> ParticleEmitter (Phase-4 tunes the emission cone / lifetime).
        _smoke = new ParticleEmitter("smoke", Type.Triangle, 16);
        _smoke.setLowLife(0.5f);
        _smoke.setHighLife(1.5f);
        _smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 0f, 0.01f));
        _smoke.getParticleInfluencer().setVelocityVariation(0.1f);
        _smoke.setStartSize(0.5f * _size);
        _smoke.setEndSize(5f * _size);
        _smoke.setStartColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
        _smoke.setEndColor(new ColorRGBA(0.15f, 0.15f, 0.15f, 0f));
        _smoke.setParticlesPerSec(0f);
    }

    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        // effect texture loaded by path (not a model texture); load from the cache.
        if (_smoketex == null && _ctx != null) {
            _smoketex = _ctx.getTextureCache().getTexture("textures/effects/dust.png");
            _smoketex.setWrap(WrapMode.Clamp);
        }
        if (_smoke != null && _smoketex != null) {
            Material mat = new Material(_ctx.getAssetManager(),
                "Common/MatDefs/Misc/Particle.j3md");
            mat.setTexture("Texture", _smoketex);
            RenderUtil.applyBlendAlpha(mat);
            RenderUtil.applyOverlayZBuf(mat);
            _smoke.setMaterial(mat);
            RenderUtil.setOverlay(_smoke);
        }
    }

    @Override // documentation inherited
    public Control putClone (
        Control store, Model.CloneCreator properties)
    {
        MisfireEmission mstore;
        if (store == null) {
            mstore = new MisfireEmission();
        } else {
            mstore = (MisfireEmission)store;
        }
        super.putClone(mstore, properties);
        mstore._animShotFrame = _animShotFrame;
        mstore._size = _size;
        return mstore;
    }

    // documentation inherited
    public void update (float time)
    {
        if (!isActive () || !isActiveEmission() || _shotFrame == -1) {
            return;
        }
        int frame = (int)((_elapsed += time) / _frameDuration);
        if (frame >= _shotFrame) {
            fireShot();
            _shotFrame = -1;
            _model.stopAnimation();
        }
    }

    @Override // documentation inherited
    protected void animationStarted (String name)
    {
        super.animationStarted(name);
        if (!isActiveEmission()) {
            return;
        }

        if (_sprite instanceof MobileSprite) {
            ((MobileSprite)_sprite).startComplexAction();
        }

        // get the grame at which the misfire happens, if any
        Integer frame = (_animShotFrame == null) ?
            null : _animShotFrame.get(name);
        _shotFrame = (frame == null) ? -1 : frame;
        if (_shotFrame == -1) {
            return;
        }

        // set initial animation state
        _frameDuration = 1f / _model.getAnimation(name).frameRate;
        _elapsed = 0f;
    }

    @Override // documenation inherited
    protected void animationStopped (String name)
    {
        if (!isActive() || !isActiveEmission()) {
            return;
        }
        if (_sprite instanceof MobileSprite) {
            ((MobileSprite)_sprite).stopComplexAction();
        }
        for (Object ctrl : _model.getControllers()) {
            if (ctrl instanceof GunshotEmission) {
                ((GunshotEmission)ctrl).setActiveEmission(true);
            } else if (ctrl instanceof MisfireEmission) {
                ((MisfireEmission)ctrl).setActiveEmission(false);
            }
        }
        super.animationStopped(name);
    }

    /**
     * Activates the misfire effect.
     */
    protected void fireShot ()
    {
        // black smoke
        if (_smoke != null) {
            if (_smoke.getParent() == null) {
                _model.getEmissionNode().attachChild(_smoke);
            }
            _smoke.setLocalTranslation(new Vector3f(_target.getWorldTranslation()));
            _smoke.emitAllParticles();
        }
        if (_sprite != null) {
            _sprite.displayParticles("frontier_town/hit_flash", true);
        }
    }

    /** For each animation, the frame at which the misfire goes off. */
    protected HashMap<String, Integer> _animShotFrame;

    /** The size of the smoke. */
    protected float _size;

    /** The frame at which the misfire goes off for the current animation. */
    protected int _shotFrame;

    /** The duration of a single frame in seconds. */
    protected float _frameDuration;

    /** The time elapsed since the start of the animation. */
    protected float _elapsed;

    /** The model to which this emission is bound. */
    protected Model _model;

    /** The misfire smoke particle system. */
    protected ParticleEmitter _smoke;

    /** The smoke texture. */
    protected static Texture _smoketex;
}
