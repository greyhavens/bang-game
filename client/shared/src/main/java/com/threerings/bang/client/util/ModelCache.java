//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import com.samskivert.util.Interval;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultHandler;
import com.samskivert.util.ResultListener;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.ModelTextureResolver;
import com.threerings.jme.model.TextureProvider;

import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.BasicContext;

/**
 * Maintains a cache of resolved 3D models.
 *
 * <p>jME3 cutover (Phase 2): re-pointed off the fork {@code Model.readFromFile}/{@code model.dat}
 * path onto stock jME3 {@code AssetManager.loadModel(".j3o")} wrapped in the app-side
 * {@link Model} facade. The fork-era per-instance resource bookkeeping (VBOInfo / display-list /
 * shader-attribute deletion, GL_VENDOR-gated hardware skinning) is gone: jME3 owns VBO lifecycle,
 * and skinning rides on the {@code .j3o}'s {@code SkinningControl}. Per-instance variant /
 * colorization / detail re-resolution is threaded through {@link ModelTextureResolver} via the
 * facade's {@link TextureProvider} seam.
 *
 * <p><b>Phase boundary.</b> The {@code .j3o} bake + loader registration
 * ({@code BangModelLoader} on the client {@code AssetManager}) is wired at the Phase-3 host flip;
 * until then {@link #loadPrototype} resolves against the live {@code AssetManager} the host
 * supplies. Per-owner colorization recolour is applied at instance time by
 * {@link ModelTextureProvider#getTexture}, routing colorized model-texture loads through the
 * zation-aware {@code TextureCache} (the fork's approach); the bake-time
 * {@link ModelTextureResolver#colorize} hook stays identity (goldens render without zations).
 */
public class ModelCache extends PrototypeCache<ModelCache.ModelKey, Model>
{
    public ModelCache (BasicContext ctx)
    {
        super(ctx);

        // create the interval to flush cleared prototypes
        new Interval(ctx.getApp()) {
            public void expired () {
                Reference<? extends Model> ref;
                while ((ref = _cleared.poll()) != null) {
                    PrototypeReference.class.cast(ref).flush();
                }
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
    protected void postPrototypeLoader (
        final ModelKey key, final ResultHandler<SoftReference<Model>> handler)
    {
        // variants are loaded by loading and configuring the default prototype
        if (key.variant != null) {
            getPrototype(key.getDefaultVariantKey(), new ResultListener<Model>() {
                public void requestCompleted (Model result) {
                    // if it's not a listed variant, it's the default
                    String[] variants = result.getVariantNames();
                    Model variant = result;
                    if (ListUtil.contains(variants, key.variant)) {
                        variant = result.createPrototype(key.variant);
                        variant.resolveTextures(new ModelTextureProvider(key, null));
                    }
                    handler.requestCompleted(new PrototypeReference(variant, result));
                }
                public void requestFailed (Exception cause) {
                    handler.requestFailed(cause);
                }
            });

        } else {
            super.postPrototypeLoader(key, handler);
        }
    }

    @Override // documentation inherited
    protected SoftReference<Model> createPrototypeReference (Model prototype)
    {
        return new PrototypeReference(prototype);
    }

    // documentation inherited
    protected Model loadPrototype (ModelKey key)
        throws Exception
    {
        long start = PerfMonitor.getCurrentMicros();
        // jME3 loads the baked .j3o through the AssetManager; the facade wraps the loaded
        // content + its AnimComposer/SkinningControl and re-exposes the client model API.
        Spatial content = _ctx.getAssetManager().loadModel(key.type + "/model.j3o");
        Model model = new Model(content);
        model.resolveTextures(new ModelTextureProvider(key, null));
        PerfMonitor.recordModelLoad(start, 0);
        return model;
    }

    // documentation inherited
    protected void initPrototype (Model prototype)
    {
        // jME3 owns VBO/skinning lifecycle; the only fork-era prototype tuning that still
        // applies is the animation mode hint (informational on jME3).
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
    protected class PrototypeReference extends SoftReference<Model>
    {
        public PrototypeReference (Model prototype)
        {
            this(prototype, null);
        }

        public PrototypeReference (Model prototype, Model original)
        {
            super(prototype, _cleared);
            _original = original;
        }

        /**
         * Deletes the resources held by the prototype. jME3 reclaims GPU buffers via its own
         * {@code NativeObjectManager}/GC, so the fork's manual VBO/display-list deletion is gone;
         * we only release the variant's reference to its default-variant original.
         */
        public void flush ()
        {
            _original = null;
        }

        /** A reference to the default variant (if this isn't it) to keep it from being
         * collected when this variant is in use. */
        protected Model _original;
    }

    /** Re-resolves model textures for an instance via the {@link ModelTextureResolver}. */
    protected class ModelTextureProvider
        implements TextureProvider
    {
        public ModelTextureProvider (ModelKey key, Colorization[] zations)
        {
            _key = key;
            _zations = zations;
            // the variant index (fork CloneCreator.random analogue) selects among multi-valued
            // textures; detail scales texture resolution. Threaded into the resolver per the
            // ModelCache-equivalent design (docs/jme3-model-loader.md §2).
            _resolver = new ModelTextureResolver(_ctx.getAssetManager());
        }

        // documentation inherited from interface TextureProvider
        public Texture getTexture (Geometry geom, String name)
        {
            String path = _resolver.textureAssetPath(_key.type, name);
            // Reproduce the fork ModelCache.ModelTextureProvider: with no colorizations, bind the
            // texture exactly as baked (stock AssetManager.loadTexture, identical to the bake-time
            // ModelTextureResolver path that produces the goldens). With colorizations (team/owner
            // tinting from PieceSprite.updateColorizations, colorLookup[owner+1]), route through the
            // zation-aware TextureCache so the texture's paletted pixels are recoloured per the
            // owner's zations -- the fork did exactly this:
            //   tstate.setTexture(_zations == null ?
            //       cache.getTexture(path, scale) : cache.getTexture(path, _zations, scale));
            if (_zations == null) {
                return _ctx.getAssetManager().loadTexture(path);
            }
            float scale = BangPrefs.isMediumDetail() ? 1f : 0.5f;
            return _ctx.getTextureCache().getTexture(path, _zations, scale);
        }

        /** The model key. */
        protected ModelKey _key;

        /** The colorizations to apply, or <code>null</code> for none. */
        protected Colorization[] _zations;

        /** The shared re-resolution engine. */
        protected ModelTextureResolver _resolver;
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

        @Override // documentation inherited
        public String toString ()
        {
            return type + (variant == null ? "" : (" (" + variant + ")"));
        }

        protected ModelKey (String type)
        {
            this.type = type;
        }
    }

    /** The queue of prototypes to destroy. */
    protected ReferenceQueue<Model> _cleared = new ReferenceQueue<Model>();

    /** The rate at which to check for cleared prototypes to destroy. */
    protected static final long FLUSH_INTERVAL = 5000L;
}
