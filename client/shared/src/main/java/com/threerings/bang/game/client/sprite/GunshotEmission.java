//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.Properties;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A gunshot effect with muzzle flash and bullet trail.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1 — effects port stub)</h3>
 *
 * The fork built the flare from a {@code SharedMesh}, the bullet trails from a {@code TriMesh} with
 * a per-vertex colour buffer, and the gunsmoke from a {@code ParticleMesh}; per-frame colour came
 * off the fork {@code GeomBatch.getDefaultColor()}. Those are rebuilt here as jME3 {@link Geometry}
 * objects with an {@code Unshaded} {@link Material} (the per-frame colour rides the material's
 * {@code Color} param) and a {@link ParticleEmitter} for the smoke. Effect textures that the fork
 * pulled through the model {@code TextureProvider} by path are now loaded directly from the texture
 * cache (they are effect assets, not model textures). Frame-accurate visual fidelity (exact flare
 * UVs, smoke tuning) is finalised in the Phase-4 effects pass; this preserves the geometry shapes,
 * timing, and "muzzle flash + trail + smoke + hit flash" behaviour and compiles fork-free.
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
        _flare = new Flare();
        for (int ii = 0; ii < _trails.length; ii++) {
            _trails[ii] = new Trail();
        }

        if (!BangPrefs.isHighDetail()) {
            return;
        }
        // gunsmoke burst: fork ParticleMesh -> jME3 ParticleEmitter (Phase-4 will tune the
        // emission cone / lifetime curve to match the fork values exactly).
        _smoke = new ParticleEmitter("smoke", Type.Triangle, 16);
        _smoke.setLowLife(0.5f);
        _smoke.setHighLife(1.5f);
        _smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 0f, 0.005f));
        _smoke.getParticleInfluencer().setVelocityVariation(0.1f);
        _smoke.setStartSize(0.25f * _size);
        _smoke.setEndSize(2f * _size);
        _smoke.setStartColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.75f));
        _smoke.setEndColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 0f));
        _smoke.setParticlesPerSec(0f);
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
        // these are effect textures loaded by path, not model textures; the fork-free facade
        // TextureProvider resolves model-geometry textures, so we load these from the cache.
        if (_smoketex == null && _ctx != null) {
            _smoketex = _ctx.getTextureCache().getTexture("textures/effects/dust.png");
            _smoketex.setWrap(WrapMode.Clamp);
            _ftex = _ctx.getTextureCache().getTexture("textures/effects/flash.png");
        }
        if (_smoke != null && _smoketex != null) {
            applyParticleTexture(_smoke, _smoketex);
        }
        if (_flare != null && _ftex != null) {
            _flare.setTexture(_ftex);
        }
    }

    @Override // documentation inherited
    public Control putClone (
        Control store, Model.CloneCreator properties)
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

        _fmesh = new Mesh();
        _fmesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        _fmesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tbuf);
        _fmesh.setBuffer(VertexBuffer.Type.Index, 3, ibuf);
        _fmesh.updateBound();
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
            if (_smoke.getParent() == null) {
                _model.getEmissionNode().attachChild(_smoke);
            }
            _smoke.setLocalTranslation(new Vector3f(trans));
            _smoke.emitAllParticles();
        }

        // activate the effect, if present
        if (_particles != null) {
            _particles.setLocalTranslation(new Vector3f(
                _sprite.getWorldTranslation()).addLocal(0f, 0f, TILE_SIZE / 2));
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
     * Finds a random direction at most <code>spread</code> radians away from the given direction.
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

    /** Applies a texture to a particle emitter's material (Particle.j3md). */
    protected void applyParticleTexture (ParticleEmitter emitter, Texture tex)
    {
        Material mat = new Material(_ctx.getAssetManager(),
            "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", tex);
        RenderUtil.applyBlendAlpha(mat);
        RenderUtil.applyOverlayZBuf(mat);
        emitter.setMaterial(mat);
        RenderUtil.setOverlay(emitter);
    }

    /** Handles the appearance and fading of the muzzle flare. */
    protected class Flare extends Geometry
    {
        public Flare ()
        {
            super("flare", _fmesh);

            _mat = new Material(_ctx == null ? null : _ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            _mat.setColor("Color", new ColorRGBA(ColorRGBA.White));
            RenderUtil.applyOverlayZBuf(_mat);
            RenderUtil.applyAddAlpha(_mat);
            setMaterial(_mat);
            setQueueBucket(Bucket.Transparent);
            setLocalScale(1.5f * _size);
            // step the flare fade via a control attached to the flare itself
            addControl(new AbstractControl() {
                protected void controlUpdate (float tpf) {
                    step(tpf);
                }
                protected void controlRender (RenderManager rm, ViewPort vp) {}
            });
        }

        public void setTexture (Texture tex)
        {
            _mat.setTexture("ColorMap", tex);
        }

        public void activate ()
        {
            // set the flare location based on the marker position/direction
            setLocalTranslation(new Vector3f(_target.getWorldTranslation()));
            setLocalRotation(_target.getWorldRotation().clone());

            _model.getEmissionNode().attachChild(this);
            _elapsed = 0f;
        }

        protected void step (float time)
        {
            if (getParent() == null) {
                return;
            }
            if ((_elapsed += time) >= FLARE_DURATION) {
                _model.getEmissionNode().detachChild(this);
            }
            ColorRGBA c = new ColorRGBA();
            c.interpolateLocal(ColorRGBA.White, ColorRGBA.Black,
                Math.min(_elapsed / FLARE_DURATION, 1f));
            _mat.setColor("Color", c);
        }

        protected Material _mat;
        protected float _elapsed;
    }

    /** Handles the appearance and fading of the bullet trail. */
    protected class Trail extends Geometry
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
            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, _tvbuf);
            mesh.setBuffer(VertexBuffer.Type.Color, 4, _cbuf);
            mesh.setBuffer(VertexBuffer.Type.Index, 3, _tibuf);
            mesh.updateBound();
            setMesh(mesh);
            _tmesh = mesh;

            Material mat = new Material(_ctx == null ? null : _ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setBoolean("VertexColor", true);
            RenderUtil.applyOverlayZBuf(mat);
            RenderUtil.applyBlendAlpha(mat);
            setMaterial(mat);
            setQueueBucket(Bucket.Transparent);
            addControl(new AbstractControl() {
                protected void controlUpdate (float tpf) {
                    step(tpf);
                }
                protected void controlRender (RenderManager rm, ViewPort vp) {}
            });
        }

        public void activate ()
        {
            // set the translation based on the location of the source
            Vector3f trans = _target.getWorldTranslation();
            setLocalTranslation(new Vector3f(trans));

            // set the scale based on the distance to the target (if it exists)
            _tdist = 10f;
            if (_sprite instanceof MobileSprite) {
                PieceSprite target = ((MobileSprite)_sprite).getTargetSprite();
                if (target != null) {
                    _tdist = target.getWorldTranslation().distance(trans);
                }
            }
            setLocalScale(0f, 0.175f * _size, 1f);

            // choose a direction within the spread range
            getRandomDirection(_spread, _sdir);

            // set the orientation based on the eye vector and direction
            _ctx.getCameraHandler().getCamera().getLocation().subtract(trans, _eye);
            _eye.cross(_sdir, _yvec).normalizeLocal();
            _sdir.cross(_yvec, _zvec);
            Quaternion rot = new Quaternion();
            rot.fromAxes(_sdir, _yvec, _zvec);
            setLocalRotation(rot);

            _model.getEmissionNode().attachChild(this);
            _elapsed = 0f;
        }

        protected void step (float time)
        {
            if (getParent() == null) {
                return;
            }
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
            Vector3f scale = getLocalScale();
            setLocalScale(_tdist * lscale, scale.y, scale.z);

            _color.interpolateLocal(TRAIL_START_COLOR, TRAIL_END_COLOR, a0);
            BufferUtils.setInBuffer(_color, _cbuf, 0);
            BufferUtils.setInBuffer(_color, _cbuf, 1);

            _color.interpolateLocal(TRAIL_START_COLOR, TRAIL_END_COLOR, a1);
            BufferUtils.setInBuffer(_color, _cbuf, 2);
            BufferUtils.setInBuffer(_color, _cbuf, 3);

            _tmesh.getBuffer(VertexBuffer.Type.Color).updateData(_cbuf);
        }

        protected Mesh _tmesh;
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
    protected ParticleEmitter _smoke;

    /** The effect particle system. */
    protected Spatial _particles;

    /** The shared flare mesh. */
    protected static Mesh _fmesh;

    /** The flare texture. */
    protected static Texture _ftex;

    /** The smoke texture. */
    protected static Texture _smoketex;

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
}
