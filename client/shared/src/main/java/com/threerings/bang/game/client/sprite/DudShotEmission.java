//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.HashMap;
import java.util.Properties;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
import com.jme3.util.BufferUtils;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.util.RenderUtil;

/**
 * A dud shot effect.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1 — effects port stub)</h3>
 *
 * Fork {@code SharedMesh}/{@code TriMesh}/{@code TextureState} -> jME3 {@link Geometry}/{@link Mesh}/
 * {@link Material}; the per-frame {@code updateWorldData} repositioning rides an
 * {@link AbstractControl} on the dud geometry. The Savable {@code EmissionData} becomes a plain data
 * holder (no longer serialized into the model; the effects port reconstructs it from properties).
 */
public class DudShotEmission extends SpriteEmission
{
    public static class EmissionData
    {
        public int frame;
        public boolean continueForward;
        public boolean stop;
        public float pause;

        public EmissionData ()
        {
        }
    }

    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        if (_animations == null) {
            return;
        }
        _animData = new HashMap<String, EmissionData>();
        for (String anim : _animations) {
            EmissionData ed = new EmissionData();
            ed.frame = Integer.valueOf(
                        props.getProperty(anim + ".shot_frame", "-1"));
            ed.pause = Float.valueOf(props.getProperty(anim + ".pause", "1"));
            ed.continueForward = Boolean.valueOf(props.getProperty(
                        anim + ".continue_forward", "false"));
            ed.stop = Boolean.valueOf(props.getProperty(
                        anim + ".stop", "true"));
            _animData.put(anim, ed);
        }
        _size = Float.valueOf(props.getProperty("size", "1"));
    }

    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        _model = model;
        setActiveEmission(false);

        if (_dmesh == null) {
            createDudMesh();
        }
        _dud = new Dud();
        if (_dudtex != null) {
            _dud.setTexture(_dudtex);
        }
    }

    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        // effect texture loaded by path; load from the cache.
        if (_dudtex == null && _ctx != null) {
            _dudtex = _ctx.getTextureCache().getTexture("textures/effects/dud.png");
        }
        if (_dud != null && _dudtex != null) {
            _dud.setTexture(_dudtex);
        }
    }

    @Override // documentation inherited
    public Control putClone (
            Control store, Model.CloneCreator properties)
    {
        DudShotEmission dstore;
        if (store == null) {
            dstore = new DudShotEmission();
        } else {
            dstore = (DudShotEmission)store;
        }
        super.putClone(dstore, properties);
        dstore._animData = _animData;
        dstore._size = _size;
        return dstore;
    }

    @Override // documentation inherited
    public void update (float time)
    {
        if (!isActive() || !isActiveEmission() || _data == null) {
            return;
        }
        switch (_stage) {
          case DUD_START:
            int frame = (int)((_elapsed += time) / _frameDuration);
            if (frame >= _data.frame) {
                _dud.activate();
                if (_data.stop) {
                    _model.pauseAnimation(true);
                }
                _elapsed = 0f;
                _stage = DudStage.DUD_PAUSE;
            }
            break;

          case DUD_PAUSE:
            if ((_elapsed += time) >= _data.pause) {
                _model.getEmissionNode().detachChild(_dud);
                if (!_data.continueForward && _data.stop) {
                    _model.reverseAnimation();
                }
                if (_data.stop) {
                    _model.pauseAnimation(false);
                }
                _stage = DudStage.DUD_FINISH;
            }
            break;

        default:
            break; // nada
        }
    }

    @Override // documentation inherited
    protected void animationStarted (String name)
    {
        super.animationStarted(name);
        if (!isActiveEmission()) {
            return;
        }
        // get the frame at which the dud appears
        _data = (_animData == null) ?  null : _animData.get(name);
        if (_data == null) {
            return;
        }

        if (_sprite instanceof MobileSprite) {
            ((MobileSprite)_sprite).startComplexAction();
        }

        // set initial animation state
        _frameDuration = 1f / _model.getAnimation(name).frameRate;
        _elapsed = 0f;
        _stage = DudStage.DUD_START;
        if (_dud != null) {
            _model.getEmissionNode().detachChild(_dud);
        }

    }

    @Override // documentation inherited
    protected void animationStopped (String name)
    {
        if (!isActive() || !isActiveEmission() || _data == null) {
            return;
        }
        if (_sprite instanceof MobileSprite) {
            ((MobileSprite)_sprite).stopComplexAction();
        }
        for (Object ctrl : _model.getControllers()) {
            if (ctrl instanceof GunshotEmission) {
                ((GunshotEmission)ctrl).setActiveEmission(true);
            } else if (ctrl instanceof DudShotEmission) {
                ((DudShotEmission)ctrl).setActiveEmission(false);
            }
        }
        super.animationStopped(name);
    }

    /**
     * Creates the shared dud mesh geometry.
     */
    protected void createDudMesh ()
    {
        FloatBuffer vbuf = BufferUtils.createVector3Buffer(4),
                    tbuf = BufferUtils.createVector2Buffer(4);
        IntBuffer ibuf = BufferUtils.createIntBuffer(6);

        vbuf.put(0f).put(0f).put(0f);
        vbuf.put(0f).put(0f).put(2f);
        vbuf.put(1f).put(0f).put(2f);
        vbuf.put(1f).put(0f).put(0f);

        tbuf.put(1f).put(1f);
        tbuf.put(0f).put(1f);
        tbuf.put(0f).put(0f);
        tbuf.put(1f).put(0f);

        ibuf.put(0).put(2).put(1);
        ibuf.put(0).put(3).put(2);

        _dmesh = new Mesh();
        _dmesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        _dmesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tbuf);
        _dmesh.setBuffer(VertexBuffer.Type.Index, 3, ibuf);
        _dmesh.updateBound();
    }

    /** Handles the appearance and fading of the dud mesh. */
    protected class Dud extends Geometry
    {
        public Dud ()
        {
            super("dud", _dmesh);

            _mat = new Material(_ctx == null ? null : _ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            RenderUtil.applyBlendAlpha(_mat);
            setMaterial(_mat);
            setQueueBucket(Bucket.Transparent);
            setLocalScale(1.5f * _size);
            // track the emitter target each frame
            addControl(new AbstractControl() {
                protected void controlUpdate (float tpf) {
                    if (getParent() != null) {
                        setLocalTranslation(new Vector3f(_target.getWorldTranslation()));
                        setLocalRotation(_target.getWorldRotation().clone());
                    }
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
            _model.getEmissionNode().attachChild(this);
        }

        protected Material _mat;
        protected float _elapsed;
    }

    /** For each animation, the frames at which the shots go off. */
    protected HashMap<String, EmissionData> _animData;

    /** The size of the shots. */
    protected float _size;

    /** The emission data for the current animation. */
    protected EmissionData _data;

    /** The duration of a single frame in seconds. */
    protected float _frameDuration;

    /** The time elapsed since the start of the animation. */
    protected float _elapsed;

    /** Result variables to reuse. */
    protected Vector3f _eloc = new Vector3f();
    protected Quaternion _erot = new Quaternion();

    /** The model to which this emission is bound. */
    protected Model _model;

    /** The dud handler. */
    protected Dud _dud;

    /** The current stage. */
    protected DudStage _stage = DudStage.DUD_START;

    /** The shared dud mesh. */
    protected static Mesh _dmesh;

    /** The dud texture. */
    protected static Texture _dudtex;

    /** Dud stages. */
    protected static enum DudStage { DUD_START, DUD_PAUSE, DUD_FINISH };
}
