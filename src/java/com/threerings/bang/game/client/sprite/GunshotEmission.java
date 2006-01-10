//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.Properties;

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
        _flare = new Flare(ctx);
    }
    
    @Override // documentation inherited
    public void start (Model.Animation anim, Model.Binding binding)
    {
        super.start(anim, binding);
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
        getEmitterLocation(true, _location);
        
        // fire off a flash of light
        _view.createLightFlash(_location, LIGHT_FLASH_COLOR, 0.125f);
        
        // and a muzzle flare
        _flare.activate(_location);
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
        public Flare (BasicContext ctx)
        {
            super("flare", _fmesh);
            
            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.addAlpha);
            setRenderState(RenderUtil.overlayZBuf);
            if (_ftex == null) {
                _ftex = RenderUtil.createTextureState(ctx,
                    "textures/effects/flash.png");
            }
            setRenderState(_ftex);
            updateRenderState();
            
            setLocalScale(1.5f);
            setDefaultColor(new ColorRGBA());
        }
        
        public void activate (Vector3f eloc)
        {
            // set the location based on the marker position/direction
            getLocalTranslation().set(eloc);
            getEmitterDirection(true, _dir);
            PathUtil.computeAxisRotation(Vector3f.UNIT_Z, _dir,
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
            getDefaultColor().interpolate(ColorRGBA.white, ColorRGBA.black,
                _elapsed / FLARE_DURATION);
        }
        
        protected float _elapsed;
        protected Vector3f _dir = new Vector3f();
    }
    
    /** The frames at which the shots go off. */
    protected int[] _shotFrames;
    
    /** The duration of a single frame in seconds. */
    protected float _frameDuration;
    
    /** The controller that activates the shots at the appropriate frames. */
    protected ShotController _shotctrl = new ShotController();
    
    /** A result vector to reuse. */
    protected Vector3f _location = new Vector3f();
    
    /** The muzzle flare. */
    protected Flare _flare;
    
    /** The shared flare mesh. */
    protected static TriMesh _fmesh;
    
    /** The flare texture. */
    protected static TextureState _ftex;
    
    /** The color of the flash of light generated by the shot. */
    protected static final ColorRGBA LIGHT_FLASH_COLOR =
        new ColorRGBA(1f, 1f, 0.9f, 1f);
        
    /** The duration of the muzzle flare. */
    protected static final float FLARE_DURATION = 0.125f;
}
