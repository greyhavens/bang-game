//
// $Id$

package com.threerings.bang.client.util;

import java.io.File;

import java.nio.ByteOrder;

import java.util.HashMap;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of resolved 3D models.
 */
public class ModelCache extends PrototypeCache<Model>
{
    public ModelCache (BasicContext ctx)
    {
        super(ctx);
        
        // create a texture state here in order to make sure that the
        // texture state initialization isn't called from the loader
        _ctx.getRenderer().createTextureState();
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
        String key = type + "/" + name;
        getInstance(key, zations, rl);
    }

    // documentation inherited
    protected Model loadPrototype (String key)
        throws Exception
    {
        File file = _ctx.getResourceManager().getResourceFile(
            key + "/model.dat");
        long start = PerfMonitor.getCurrentMicros();
        int size = (int)file.length();
        Model model = Model.readFromFile(file, size >= MIN_MAP_SIZE &&
            ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
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
        String key, Model prototype, Colorization[] zations)
    {
        Model instance = prototype.createInstance();
        if (zations != null) {
            // re-resolve the model's textures using the supplied colorizations
            instance.resolveTextures(new ModelTextureProvider(key, zations));
        }
        return instance;
    }
    
    /** Resolved model textures using the texture cache. */
    protected class ModelTextureProvider
        implements TextureProvider
    {
        public ModelTextureProvider (String key, Colorization[] zations)
        {
            _key = key;
            _zations = zations;
        }
        
        // documentation inherited from interface TextureProvider
        public TextureState getTexture (String name)
        {
            String path = name.startsWith("/") ?
                name.substring(1) : cleanPath(_key + "/" + name);
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
        protected String _key;
        
        /** The colorizations to apply, or <code>null</code> for none. */
        protected Colorization[] _zations;
        
        /** Maps texture paths to texture states created so far. */
        protected HashMap<String, TextureState> _tstates =
            new HashMap<String, TextureState>();
    }
    
    /** Models bigger than this have their buffers mapped into memory. */
    protected static final int MIN_MAP_SIZE = 32768;
}
