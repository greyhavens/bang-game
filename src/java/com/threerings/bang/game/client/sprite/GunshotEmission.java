//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.HashMap;
import java.util.Properties;

import com.jme.math.FastMath;
import com.jme.math.Line;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;
import com.jmex.effects.ParticleManager;

import com.samskivert.util.StringUtil;

import com.threerings.jme.sprite.PathUtil;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.Log.*;

/**
 * A gunshot effect with muzzle flash and bullet trail.
 */
public class GunshotEmission extends SpriteEmission
{
    public GunshotEmission (String name, Properties props)
    {
        super(name);
        _shotFrames = StringUtil.parseIntArray(
            props.getProperty("shot_frames", ""));
    }
    
    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.init(ctx, view, sprite);
        if (_fmesh == null) {
            createFlareMesh();
        }
        _flare = new Flare();
        _trail = new Trail();
        
        _smokemgr = new ParticleManager(16);
        _smokemgr.setParticlesMinimumLifeTime(500f);
        _smokemgr.setInitialVelocity(0.01f);
        _smokemgr.setEmissionDirection(Vector3f.UNIT_Z);
        _smokemgr.setEmissionMaximumAngle(FastMath.PI / 16);
        _smokemgr.setRandomMod(0f);
        _smokemgr.setPrecision(FastMath.FLT_EPSILON);
        _smokemgr.setControlFlow(false);
        _smokemgr.setStartSize(0.25f);
        _smokemgr.setEndSize(2f);
        _smokemgr.setStartColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.75f));
        _smokemgr.setEndColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0f));
        _smokemgr.setRepeatType(Controller.RT_CLAMP);
        _smokemgr.setActive(false);
        
        TriMesh particles = _smokemgr.getParticles();
        particles.addController(_smokemgr);

        if (_smoketex == null) {
            _smoketex = RenderUtil.createTextureState(_ctx,
                "textures/effects/dust.png");
        }
        particles.setRenderState(_smoketex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        _view.getPieceNode().attachChild(particles);
    }
    
    @Override // documentation inherited
    public void cleanup ()
    {
        super.cleanup();
        _view.getPieceNode().detachChild(_smokemgr.getParticles());
    }
    
    @Override // documentation inherited
    public void start (Model.Animation anim, Model.Binding binding)
    {
        super.start(anim, binding);
        
        // get the animation's shot frame numbers
        _shotFrames = _animShotFrames.get(anim);
        if (_shotFrames == null) {
            _animShotFrames.put(anim, _shotFrames = StringUtil.parseIntArray(
                anim.getEmitter(_name).props.getProperty("shot_frames", "")));
        }
        
        _shotctrl.add();
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
        vbuf.put(1f).put(0f).put(1f);
        vbuf.put(1f).put(1f).put(0f);
        vbuf.put(1f).put(0f).put(-1f);
        vbuf.put(1f).put(-1f).put(0f);
        
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
    
    /**
     * Activates the shot effect.
     */
    protected void fireShot ()
    {
        getEmitterLocation(true, _eloc);
        getEmitterDirection(true, _edir);
        
        // fire off a flash of light
        _view.createLightFlash(_eloc, LIGHT_FLASH_COLOR, 0.125f);
        
        // and a muzzle flare
        _flare.activate(_eloc, _edir);
        
        // and a bullet trail
        _trail.activate(_eloc, _edir);
        
        // and a burst of smoke
        _smokemgr.setParticlesOrigin(_eloc);
        _smokemgr.forceRespawn();
    }
    
    /** Fires shots at the configured frames. */
    protected class ShotController extends Controller
    {
        public void add ()
        {
            _idx = 0;
            _elapsed = 0f;
            _frameDuration = _anim.getDuration() / _anim.frames;
            _sprite.addController(this);
        }
        
        public void update (float time)
        {
            if (_idx >= _shotFrames.length) {
                _sprite.removeController(this);
                return;
            }
            int frame = (int)((_elapsed += time) / _frameDuration);
            if (frame >= _shotFrames[_idx]) {
                fireShot();
                _idx++;
            }
        }
        
        protected int _idx;
        protected float _frameDuration, _elapsed;
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
            if (_ftex == null) {
                _ftex = RenderUtil.createTextureState(_ctx,
                    "textures/effects/flash.png");
            }
            setRenderState(_ftex);
            setLocalScale(1.5f);
            setDefaultColor(new ColorRGBA());
            updateRenderState();
        }
        
        public void activate (Vector3f eloc, Vector3f edir)
        {
            // set the flare location based on the marker position/direction
            getLocalTranslation().set(eloc);
            PathUtil.computeAxisRotation(Vector3f.UNIT_Z, edir,
                getLocalRotation());
            
            _view.getPieceNode().attachChild(this);
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            if ((_elapsed += time) >= FLARE_DURATION) {
                _view.getPieceNode().detachChild(this);
            }
            getDefaultColor().interpolate(ColorRGBA.white,
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
            
            FloatBuffer vbuf = BufferUtils.createVector3Buffer(4);
            _cbuf = BufferUtils.createFloatBuffer(4 * 4);
            IntBuffer ibuf = BufferUtils.createIntBuffer(6);
        
            vbuf.put(0f).put(0.5f).put(0f);
            vbuf.put(0f).put(-0.5f).put(0f);
            vbuf.put(1f).put(-0.5f).put(0f);
            vbuf.put(1f).put(0.5f).put(0f);

            ibuf.put(0).put(1).put(2);
            ibuf.put(0).put(2).put(3);

            reconstruct(vbuf, null, _cbuf, null, ibuf);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            updateRenderState();
        }
        
        public void activate (Vector3f eloc, Vector3f edir)
        {
            // set the transation based on the location of the source
            getLocalTranslation().set(eloc);
            
            // set the scale based on the distance to the target
            PieceSprite target = ((MobileSprite)_sprite).getTargetSprite();
            _tdist = (target == null) ? 1f :
                target.getLocalTranslation().distance(eloc);
            getLocalScale().set(0f, 0.175f, 1f);
            
            // set the orientation based on the eye vector and direction
            _ctx.getRenderer().getCamera().getLocation().subtract(eloc,
                _eye);
            _eye.cross(_edir, _yvec).normalizeLocal();
            _edir.cross(_yvec, _zvec);
            getLocalRotation().fromAxes(_edir, _yvec, _zvec);
            
            _view.getPieceNode().attachChild(this);
            _elapsed = 0f;
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            float lscale, a0, a1;
            if ((_elapsed += time) >=
                TRAIL_EXTEND_DURATION + TRAIL_FADE_DURATION) {
                _view.getPieceNode().detachChild(this);
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
    
    /** For each animation, the frames at which the shots go off. */
    protected HashMap<Model.Animation, int[]> _animShotFrames =
        new HashMap<Model.Animation, int[]>();
    
    /** The frames at which the shots go off for the current animation. */
    protected int[] _shotFrames;
    
    /** The duration of a single frame in seconds. */
    protected float _frameDuration;
    
    /** The controller that activates the shots at the appropriate frames. */
    protected ShotController _shotctrl = new ShotController();
    
    /** Result vectors to reuse. */
    protected Vector3f _eloc = new Vector3f(), _edir = new Vector3f();
    
    /** The muzzle flare handler. */
    protected Flare _flare;
    
    /** The bullet trail handler. */
    protected Trail _trail;
    
    /** The gunsmoke particle manager. */
    protected ParticleManager _smokemgr;
    
    /** The shared flare mesh. */
    protected static TriMesh _fmesh;
    
    /** The flare texture. */
    protected static TextureState _ftex;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
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
}
