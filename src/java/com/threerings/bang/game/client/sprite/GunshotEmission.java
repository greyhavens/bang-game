//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.Properties;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.geom.BufferUtils;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.effect.ParticlePool;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A gunshot effect with muzzle flash and bullet trail.
 */
public class GunshotEmission extends FrameEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _size = Float.valueOf(props.getProperty("size", "1"));
        _trails = new Trail[Integer.valueOf(props.getProperty("trails", "1"))];
        _spread = Float.valueOf(props.getProperty("spread", "0"));
        _effect = props.getProperty("effect");
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        
        if (_fmesh == null) {
            createFlareMesh();
        }
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        _flare = new Flare();
        if (_ftex != null) {
            _flare.setRenderState(_ftex);
        }
        for (int ii = 0; ii < _trails.length; ii++) {
            _trails[ii] = new Trail();
        }
        
        if (!BangPrefs.isHighDetail()) {
            return;
        }
        _smoke = new ParticleMesh("smoke", 16);
        _smoke.addController(
            new ParticlePool.TransientParticleController(_smoke));
        _smoke.setMinimumLifeTime(500f);
        _smoke.setMaximumLifeTime(1500f);
        _smoke.setInitialVelocity(0.005f);
        _smoke.setEmissionDirection(Vector3f.UNIT_Z);
        _smoke.setMaximumAngle(FastMath.PI / 16);
        _smoke.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _smoke.getParticleController().setControlFlow(false);
        _smoke.setStartSize(0.25f * _size);
        _smoke.setEndSize(2f * _size);
        _smoke.setStartColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.75f));
        _smoke.setEndColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 0f));
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
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.setSpriteRefs(ctx, view, sprite);
        if (_effect != null && BangPrefs.isHighDetail()) {
            _ctx.loadParticles(_effect,
                new ResultAttacher<Spatial>(_model.getEmissionNode()) {
                public void requestCompleted (Spatial result) {
                    super.requestCompleted(result);
                    _particles = result;
                }
            });
        }
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_smoketex == null) {
            _smoketex = tprov.getTexture("/textures/effects/dust.png");
            _smoketex.getTexture().setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
            _ftex = tprov.getTexture("/textures/effects/flash.png");
        }
        if (_smoke != null) {
            _smoke.setRenderState(_smoketex);
        }
        _flare.setRenderState(_ftex);
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        GunshotEmission gstore;
        if (store == null) {
            gstore = new GunshotEmission();
        } else {
            gstore = (GunshotEmission)store;
        }
        super.putClone(gstore, properties);
        gstore._size = _size;
        gstore._trails = new Trail[_trails.length];
        gstore._spread = _spread;
        gstore._effect = _effect;
        return gstore;
    }
    
    @Override // documentation inherited
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _size = capsule.readFloat("size", 1f);
        _trails = new Trail[capsule.readInt("numTrails", 1)];
        _spread = capsule.readFloat("spread", 0f);
        _effect = capsule.readString("effect", null);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_size, "size", 1f);
        capsule.write(_trails.length, "numTrails", 1);
        capsule.write(_spread, "spread", 0f);
        capsule.write(_effect, "effect", null);
    }
    
    /**
     * Creates the shared flare mesh geometry.
     */
    protected void createFlareMesh ()
    {
        FloatBuffer vbuf = BufferUtils.createVector3Buffer(5),
            tbuf = BufferUtils.createVector2Buffer(5);
        IntBuffer ibuf = BufferUtils.createIntBuffer(12);
        
        vbuf.put(0f).put(0f).put(0f);
        vbuf.put(-1f).put(0f).put(1f);
        vbuf.put(0f).put(1f).put(1f);
        vbuf.put(1f).put(0f).put(1f);
        vbuf.put(0f).put(-1f).put(1f);
        
        tbuf.put(0f).put(0.5f);
        tbuf.put(1f).put(1f);
        tbuf.put(1f).put(0f);
        tbuf.put(1f).put(1f);
        tbuf.put(1f).put(0f);
        
        ibuf.put(0).put(2).put(1);
        ibuf.put(0).put(3).put(2);
        ibuf.put(0).put(1).put(4);
        ibuf.put(0).put(4).put(3);
        
        _fmesh = new TriMesh("fmesh", vbuf, null, null, tbuf, ibuf);
    }
    
    // documentation inherited
    protected void fireEmission ()
    {
        Vector3f trans = _target.getWorldTranslation();
        
        // fire off a flash of light if we're in the real view
        if (_view != null && BangPrefs.isMediumDetail()) {
            _view.createLightFlash(trans, LIGHT_FLASH_COLOR, 0.125f);
        }
        
        // and a muzzle flare
        _flare.activate();
        
        // and one or more bullet trails
        for (int ii = 0; ii < _trails.length; ii++) {
            _trails[ii].activate();
        }
        
        // and a burst of smoke
        if (_smoke != null) {
            if (!_smoke.isActive()) {
                _model.getEmissionNode().attachChild(_smoke);
                _smoke.updateRenderState();
            }
            _smoke.getLocalTranslation().set(trans);
            _smoke.updateGeometricState(0f, true);
            _smoke.forceRespawn();
        }
        
        // activate the effect, if present
        if (_particles != null) {
            _particles.getLocalTranslation().set(
                _sprite.getWorldTranslation()).addLocal(0f, 0f, TILE_SIZE / 2);
            ParticleUtil.forceRespawn(_particles);
        }
        
        // finally, the hit flash effect on the target
        if (_sprite != null) {
            PieceSprite target = ((MobileSprite)_sprite).getTargetSprite();
            if (target != null) {
                target.displayParticles("frontier_town/hit_flash", true);
            }
        }
    }
    
    /**
     * Finds a random direction at most <code>spread</code> radians away from
     * the given direction.
     */
    protected void getRandomDirection (float spread, Vector3f result)
    {
        result.set(Vector3f.UNIT_Z);
        _rot.fromAngleNormalAxis(RandomUtil.getFloat(spread),
            Vector3f.UNIT_Y).multLocal(result);
        _rot.fromAngleNormalAxis(RandomUtil.getFloat(FastMath.TWO_PI),
            Vector3f.UNIT_Z).multLocal(result);
        _target.getWorldRotation().multLocal(result);
    }
    
    /** Handles the appearance and fading of the muzzle flare. */
    protected class Flare extends SharedMesh
    {
        public Flare ()
        {
            super("flare", _fmesh);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.addAlpha);
            setLocalScale(1.5f * _size);
            updateRenderState();
        }
        
        public void activate ()
        {
            // set the flare location based on the marker position/direction
            getLocalTranslation().set(_target.getWorldTranslation());
            getLocalRotation().set(_target.getWorldRotation());
            
            _model.getEmissionNode().attachChild(this);
            updateRenderState();
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            if ((_elapsed += time) >= FLARE_DURATION) {
                _model.getEmissionNode().detachChild(this);
            }
            getBatch(0).getDefaultColor().interpolate(ColorRGBA.white,
                ColorRGBA.black, _elapsed / FLARE_DURATION);
        }
        
        protected float _elapsed;
    }
    
    /** Handles the appearance and fading of the bullet trail. */
    protected class Trail extends TriMesh
    {
        public Trail ()
        {
            super("trail");
            
            // use shared vertex and index buffers, but a unique color buffer
            if (_tvbuf == null) {
                _tvbuf = BufferUtils.createVector3Buffer(4);
                _tvbuf.put(0f).put(0.5f).put(0f);
                _tvbuf.put(0f).put(-0.5f).put(0f);
                _tvbuf.put(1f).put(-0.5f).put(0f);
                _tvbuf.put(1f).put(0.5f).put(0f);
                _tibuf = BufferUtils.createIntBuffer(6);
                _tibuf.put(0).put(1).put(2);
                _tibuf.put(0).put(2).put(3);
            }
            _cbuf = BufferUtils.createFloatBuffer(4 * 4);
            reconstruct(_tvbuf, null, _cbuf, null, _tibuf);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            updateRenderState();
        }
        
        public void activate ()
        {
            // set the transation based on the location of the source
            Vector3f trans = _target.getWorldTranslation();
            getLocalTranslation().set(trans);
            
            // set the scale based on the distance to the target (if it exists)
            _tdist = 10f;
            if (_sprite instanceof MobileSprite) {
                PieceSprite target = ((MobileSprite)_sprite).getTargetSprite();
                if (target != null) {
                    _tdist = target.getWorldTranslation().distance(trans);
                }
            }
            getLocalScale().set(0f, 0.175f * _size, 1f);
            
            // choose a direction within the spread range
            getRandomDirection(_spread, _sdir);
            
            // set the orientation based on the eye vector and direction
            Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
            renderer.getCamera().getLocation().subtract(trans, _eye);
            _eye.cross(_sdir, _yvec).normalizeLocal();
            _sdir.cross(_yvec, _zvec);
            getLocalRotation().fromAxes(_sdir, _yvec, _zvec);
            
            _model.getEmissionNode().attachChild(this);
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            float lscale, a0, a1;
            if ((_elapsed += time) >=
                TRAIL_EXTEND_DURATION + TRAIL_FADE_DURATION) {
                _model.getEmissionNode().detachChild(this);
                return;
                
            } else if (_elapsed >= TRAIL_EXTEND_DURATION) {
                lscale = a1 = 1f;
                a0 = (_elapsed - TRAIL_EXTEND_DURATION) / TRAIL_FADE_DURATION;
                
            } else {
                lscale = a1 = _elapsed / TRAIL_EXTEND_DURATION;
                a0 = 0f;
            }
            getLocalScale().x = _tdist * lscale;
            
            _color.interpolate(TRAIL_START_COLOR, TRAIL_END_COLOR, a0);
            BufferUtils.setInBuffer(_color, _cbuf, 0);
            BufferUtils.setInBuffer(_color, _cbuf, 1);
            
            _color.interpolate(TRAIL_START_COLOR, TRAIL_END_COLOR, a1);
            BufferUtils.setInBuffer(_color, _cbuf, 2);
            BufferUtils.setInBuffer(_color, _cbuf, 3);
        }
        
        protected FloatBuffer _cbuf;
        
        protected float _elapsed, _tdist;
        protected Vector3f _eye = new Vector3f(), _yvec = new Vector3f(),
            _zvec = new Vector3f();
        protected ColorRGBA _color = new ColorRGBA();
    }
    
    /** The size of the shots. */
    protected float _size;
    
    /** The trails' maximum angular distance from the firing direction. */
    protected float _spread;
    
    /** An effect to display on firing. */
    protected String _effect;
    
    /** Result variables to reuse. */
    protected Vector3f _sdir = new Vector3f(), _axis = new Vector3f();
    protected Quaternion _rot = new Quaternion();
    
    /** The muzzle flare handler. */
    protected Flare _flare;
    
    /** The bullet trail handler. */
    protected Trail[] _trails;
    
    /** The gunsmoke particle system. */
    protected ParticleMesh _smoke;
    
    /** The effect particle system. */
    protected Spatial _particles;
    
    /** The shared flare mesh. */
    protected static TriMesh _fmesh;
    
    /** The flare texture. */
    protected static TextureState _ftex;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    /** The shared vertex buffer for the bullet trails. */
    protected static FloatBuffer _tvbuf;
    
    /** The shared index buffer for the bullet trails. */
    protected static IntBuffer _tibuf;
    
    /** The color of the flash of light generated by the shot. */
    protected static final ColorRGBA LIGHT_FLASH_COLOR =
        new ColorRGBA(1f, 1f, 0.9f, 1f);
        
    /** The duration of the muzzle flare. */
    protected static final float FLARE_DURATION = 0.125f;
    
    /** The amount of time it takes for the bullet trail to extend fully. */
    protected static final float TRAIL_EXTEND_DURATION = 0.15f;
    
    /** The amount of time it takes for the bullet trail to fade away. */
    protected static final float TRAIL_FADE_DURATION = 0.05f;
    
    /** The starting color of the bullet trail. */
    protected static final ColorRGBA TRAIL_START_COLOR =
        new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f);
    
    /** The ending color of the bullet trail. */
    protected static final ColorRGBA TRAIL_END_COLOR =
        new ColorRGBA(0.75f, 0.75f, 0.75f, 0f);
        
    private static final long serialVersionUID = 1;
}
