//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import java.io.File;

import java.nio.ByteOrder;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GL11;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.GeomBatch;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.ChainedResultListener;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultHandler;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;
import com.threerings.jme.util.BatchVisitor;

import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of resolved 3D models.
 */
public class ModelCache extends PrototypeCache<ModelCache.ModelKey, Model>
{
    public ModelCache (BasicContext ctx)
    {
        super(ctx);

        // create a texture state here in order to make sure that the
        // texture state initialization isn't called from the loader
        _ctx.getRenderer().createTextureState();

        // create the interval to flush cleared prototypes
        new Interval(ctx.getApp()) {
            public void expired () {
                Reference<? extends ResultHandler<Model>> ref;
                while ((ref = _cleared.poll()) != null) {
                    PrototypeReference.class.cast(ref).flush();
                }

                // clear the VBO cache to make sure there are no lingering references
                _ctx.getRenderer().clearVBOCache();
            }
        }.schedule(FLUSH_INTERVAL, true);
    }

    /**
     * Loads an instance of the specified model.
     *
     * @param rl the listener to notify with the resulting model
     */
    public void getModel (String type, String name, ResultListener<Model> rl)
    {
        getModel(type, name, null, rl);
    }

    /**
     * Loads an instance of the specified model.
     *
     * @param zations colorizations to apply to the model's textures, or
     * <code>null</code> for none
     * @param rl the listener to notify with the resulting model, or
     * <code>null</code> to load the model without creating an instance
     */
    public void getModel (
        String type, String name, Colorization[] zations,
        ResultListener<Model> rl)
    {
        getModel(type, name, null, zations, rl);
    }

    /**
     * Loads an instance of the specified model.
     *
     * @param variant the model variant desired, or <code>null</code>
     * for the default
     * @param zations colorizations to apply to the model's textures, or
     * <code>null</code> for none
     * @param rl the listener to notify with the resulting model, or
     * <code>null</code> to load the model without creating an instance
     */
    public void getModel (
        String type, String name, String variant, Colorization[] zations,
        ResultListener<Model> rl)
    {
        getInstance(new ModelKey(type, name, variant), zations, rl);
    }

    @Override // documentation inherited
    protected SoftCache<ModelKey, ResultHandler<Model>> createCache ()
    {
        return new SoftCache<ModelKey, ResultHandler<Model>>() {
            protected SoftReference<ResultHandler<Model>> createReference (
                ResultHandler<Model> handler) {
                return new PrototypeReference(handler);
            }
        };
    }

    @Override // documentation inherited
    protected void postPrototypeLoader (final ModelKey key, final ResultHandler<Model> handler)
    {
        // variants are loaded by loading and configuring the default prototype
        if (key.variant != null) {
            getPrototype(key.getDefaultVariantKey(), new ChainedResultListener<Model>(handler) {
                public void requestCompleted (Model result) {
                    // if it's not a listed variant, it's the default
                    String[] variants = result.getVariantNames();
                    if (ListUtil.contains(variants, key.variant)) {
                        Model prototype = result.createPrototype(key.variant);
                        prototype.resolveTextures(new ModelTextureProvider(key, null));
                        handler.requestCompleted(prototype);
                    } else {
                        handler.requestCompleted(result);
                    }
                }
            });

        } else {
            super.postPrototypeLoader(key, handler);
        }
    }

    // documentation inherited
    protected Model loadPrototype (ModelKey key)
        throws Exception
    {
        File file = _ctx.getResourceManager().getResourceFile(
            key.type + "/model.dat");
        long start = PerfMonitor.getCurrentMicros();
        int size = (int)file.length();
        Model model = Model.readFromFile(file);
        model.resolveTextures(new ModelTextureProvider(key, null));
        PerfMonitor.recordModelLoad(start, size);
        return model;
    }

    // documentation inherited
    protected void initPrototype (Model prototype)
    {
        prototype.lockStaticMeshes(_ctx.getRenderer(), Config.useVBOs,
            Config.useDisplayLists);
        if (!BangPrefs.isMediumDetail()) {
            prototype.setAnimationMode(Model.AnimationMode.MORPH);
        }
    }

    // documentation inherited
    protected Model createInstance (
        ModelKey key, Model prototype, Colorization[] zations)
    {
        Model instance = prototype.createInstance();
        if (zations != null) {
            // re-resolve the model's textures using the supplied colorizations
            instance.resolveTextures(new ModelTextureProvider(key, zations));
        }
        return instance;
    }

    /** Identifies the resources that must be released when the prototype is cleared. */
    protected class PrototypeReference extends SoftReference<ResultHandler<Model>>
    {
        public PrototypeReference (ResultHandler<Model> handler)
        {
            super(handler, _cleared);
            handler.getResult(new ResultListener<Model>() {
                public void requestCompleted (Model result) {
                    recordResources(result);
                }
                public void requestFailed (Exception cause) {
                    // this will be reported elsewhere
                }
            });
        }

        /**
         * Deletes the resources held by the prototype.
         */
        public void flush ()
        {
            if (_lists != null) {
                for (int list : _lists) {
                    GL11.glDeleteLists(list, 1);
                }
                _lists = null;
            }
            if (_vbois == null) {
                return;
            }

            int[] buffers = null;
            for (VBOInfo vboi : _vbois) {
                buffers = maybeAdd(buffers, vboi.getVBOVertexID());
                vboi.setVBOVertexID(-1);
                vboi.setVBOVertexEnabled(false);
                buffers = maybeAdd(buffers, vboi.getVBONormalID());
                vboi.setVBONormalID(-1);
                vboi.setVBONormalEnabled(false);
                buffers = maybeAdd(buffers, vboi.getVBOIndexID());
                vboi.setVBOIndexID(-1);
                vboi.setVBOIndexEnabled(false);
                buffers = maybeAdd(buffers, vboi.getVBOColorID());
                vboi.setVBOColorID(-1);
                vboi.setVBOColorEnabled(false);
                for (int ii = 0, nn = TextureState.getNumberOfTotalUnits(); ii < nn; ii++) {
                    int buffer = vboi.getVBOTextureID(ii);
                    if (buffer > 0) {
                        buffers = IntListUtil.add(buffers, buffer);
                        vboi.setVBOTextureID(ii, -1);
                    }
                }
                vboi.setVBOTextureEnabled(false);
            }
            if (buffers != null) {
                ARBBufferObject.glDeleteBuffersARB(
                    BufferUtils.createIntBuffer(IntListUtil.compact(buffers)));
            }
            _vbois = null;
        }

        protected void recordResources (Model prototype)
        {
            final ArrayList<VBOInfo> vbois = new ArrayList<VBOInfo>();
            new BatchVisitor() {
                protected void visit (GeomBatch batch) {
                    VBOInfo vboi = batch.getVBOInfo();
                    if (vboi != null) {
                        vbois.add(vboi);
                    }
                    _lists = maybeAdd(_lists, batch.getDisplayListID());
                }
            }.traverse(prototype);

            _vbois = vbois.isEmpty() ? null : vbois.toArray(new VBOInfo[vbois.size()]);
            _lists = (_lists == null) ? null : IntListUtil.compact(_lists);

            // immediately flush if the reference has already been cleared
            if (get() == null) {
                log.warning("Prototype cleared before the model finished loading?! [model=" +
                    prototype.getName() + "].");
                flush();
            }
        }

        protected int[] maybeAdd (int[] values, int value)
        {
            return (value > 0) ? IntListUtil.add(values, value) : values;
        }

        /** The VBOs to delete. */
        protected VBOInfo[] _vbois;

        /** The display lists to delete. */
        protected int[] _lists;
    }

    /** Resolved model textures using the texture cache. */
    protected class ModelTextureProvider
        implements TextureProvider
    {
        public ModelTextureProvider (ModelKey key, Colorization[] zations)
        {
            _key = key;
            _zations = zations;
        }

        // documentation inherited from interface TextureProvider
        public TextureState getTexture (String name)
        {
            String path = name.startsWith("/") ?
                name.substring(1) : cleanPath(_key.type + "/" + name);
            TextureState tstate = _tstates.get(path);
            if (tstate == null) {
                _tstates.put(path,
                    tstate = _ctx.getRenderer().createTextureState());
                float scale = BangPrefs.isMediumDetail() ? 1f : 0.5f;
                tstate.setTexture(_zations == null ?
                    _ctx.getTextureCache().getTexture(path, scale) :
                    _ctx.getTextureCache().getTexture(path, _zations, scale));
            }
            return tstate;
        }

        /** The model key. */
        protected ModelKey _key;

        /** The colorizations to apply, or <code>null</code> for none. */
        protected Colorization[] _zations;

        /** Maps texture paths to texture states created so far. */
        protected HashMap<String, TextureState> _tstates =
            new HashMap<String, TextureState>();
    }

    /** Identifies a model type/variant. */
    protected static class ModelKey
    {
        public String type, variant;

        public ModelKey (String type, String name, String variant)
        {
            this.type = type + "/" + name;
            this.variant = variant;
        }

        /**
         * Returns the key of the default variant of this model.
         */
        public ModelKey getDefaultVariantKey ()
        {
            return new ModelKey(type);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            ModelKey okey = (ModelKey)other;
            return type.equals(okey.type) &&
                ObjectUtil.equals(variant, okey.variant);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return type.hashCode() +
                (variant == null ? 0 : variant.hashCode());
        }

        protected ModelKey (String type)
        {
            this.type = type;
        }
    }

    /** The queue of prototypes to destroy. */
    protected ReferenceQueue<ResultHandler<Model>> _cleared =
        new ReferenceQueue<ResultHandler<Model>>();

    /** The rate at which to check for cleared prototypes to destroy. */
    protected static final long FLUSH_INTERVAL = 5000L;
}
