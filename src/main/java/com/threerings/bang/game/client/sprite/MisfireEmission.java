//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.HashMap;
import java.util.Properties;

import java.io.IOException;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jmex.effects.particles.ParticleMesh;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.game.client.effect.ParticlePool;

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
        _smoke = new ParticleMesh("smoke", 16);
        _smoke.addController(
                new ParticlePool.TransientParticleController(_smoke));
        _smoke.setMinimumLifeTime(500f);
        _smoke.setMaximumLifeTime(1500f);
        _smoke.setInitialVelocity(0.01f);
        _smoke.setEmissionDirection(Vector3f.UNIT_Z);
        _smoke.setMaximumAngle(FastMath.PI / 16);
        _smoke.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _smoke.getParticleController().setControlFlow(false);
        _smoke.setStartSize(0.5f * _size);
        _smoke.setEndSize(5f * _size);
        _smoke.setStartColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
        _smoke.setEndColor(new ColorRGBA(0.15f, 0.15f, 0.15f, 0f));
        _smoke.getParticleController().setRepeatType(Controller.RT_CLAMP);
        _smoke.getParticleController().setActive(false);
        _smoke.setModelBound(new BoundingBox());
        _smoke.setIsCollidable(false);
        if (_smoketex != null) {
            _smoke.setRenderState(_smoketex);
        }
        _smoke.setRenderState(RenderUtil.blendAlpha);
        _smoke.setRenderState(RenderUtil.overlayZBuf);
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

    @Override // documentation inherited
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        String[] keys = capsule.readStringArray("animShotFrameKeys", null);
        int[] values = capsule.readIntArray("animShotFrameValues", null);
        _animShotFrame = new HashMap<String, Integer>();
        for (int ii = 0; ii < keys.length; ii++) {
            _animShotFrame.put(keys[ii], values[ii]);
        }
        _size = capsule.readFloat("size", 1f);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_animShotFrame.keySet().toArray(
            new String[_animShotFrame.size()]), "animShotFrameKeys", null);
        Integer[] values = _animShotFrame.values().toArray(
            new Integer[_animShotFrame.size()]);
        int[] ivals = new int[values.length];
        for (int ii = 0; ii < values.length; ii++) {
            ivals[ii] = values[ii];
        }
        capsule.write(ivals, "animShotFrameValues", null);
        capsule.write(_size, "size", 1f);
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
            if (!_smoke.isActive()) {
                _model.getEmissionNode().attachChild(_smoke);
                _smoke.updateRenderState();
            }
            _smoke.getLocalTranslation().set(_target.getWorldTranslation());
            _smoke.updateGeometricState(0f, true);
            _smoke.forceRespawn();
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
    protected ParticleMesh _smoke;

    /** The smoke texture. */
    protected static TextureState _smoketex;

    private static final long serialVersionUID = 1;
}
