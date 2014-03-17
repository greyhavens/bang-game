//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.HashMap;
import java.util.Properties;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.Savable;
import com.jme.util.geom.BufferUtils;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.util.RenderUtil;

/**
 * A dud shot effect.
 */
public class DudShotEmission extends SpriteEmission
{
    public static class EmissionData
        implements Savable
    {
        public int frame;
        public boolean continueForward;
        public boolean stop;
        public float pause;

        public EmissionData ()
        {
        }

        // documentation inherited
        public Class<?> getClassTag ()
        {
            return getClass();
        }

        // documentation inherited
        public void read (JMEImporter im)
            throws IOException
        {
            InputCapsule capsule = im.getCapsule(this);
            frame = capsule.readInt("frame", 0);
            continueForward = capsule.readBoolean("continueForward", false);
            stop = capsule.readBoolean("stop", true);
            pause = capsule.readFloat("pause", 1f);
        }

        // documentation inherited
        public void write (JMEExporter ex)
            throws IOException
        {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(frame, "frame", 0);
            capsule.write(continueForward, "continueForward", false);
            capsule.write(stop, "stop", true);
            capsule.write(pause, "pause", 1f);
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
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        _dud = new Dud();
        if (_dudtex != null) {
            _dud.setRenderState(_dudtex);
        }
    }

    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_dudtex == null) {
            _dudtex = tprov.getTexture("/textures/effects/dud.png");
        }
        _dud.setRenderState(_dudtex);
    }

    @Override // documentation inherited
    public Controller putClone (
            Controller store, Model.CloneCreator properties)
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
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        String[] keys = capsule.readStringArray("animDataKeys", null);
        Savable[] values = capsule.readSavableArray("animDataValues", null);
        _animData = new HashMap<String, EmissionData>();
        for (int ii = 0; ii < keys.length; ii++) {
            _animData.put(keys[ii], (EmissionData)values[ii]);
        }
        _size = capsule.readFloat("size", 1f);
    }

    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_animData.keySet().toArray(
            new String[_animData.size()]), "animDataKeys", null);
        capsule.write(_animData.values().toArray(
            new EmissionData[_animData.size()]), "animDataValues", null);
        capsule.write(_size, "size", 1f);
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

        _dmesh = new TriMesh("dmesh", vbuf, null, null, tbuf, ibuf);
    }

    /** Handles the appearance and fading of the dud mesh. */
    protected class Dud extends SharedMesh
    {
        public Dud ()
        {
            super("dud", _dmesh);

            setLightCombineMode(LightState.OFF);
            setLocalScale(1.5f * _size);
            updateRenderState();
        }

        public void activate ()
        {
            _model.getEmissionNode().attachChild(this);
            updateRenderState();
        }

        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            getLocalTranslation().set(_target.getWorldTranslation());
            getLocalRotation().set(_target.getWorldRotation());
        }

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
    protected static TriMesh _dmesh;

    /** The dud texture. */
    protected static TextureState _dudtex;

    /** Dud stages. */
    protected static enum DudStage { DUD_START, DUD_PAUSE, DUD_FINISH };

    private static final long serialVersionUID = 1;
}
