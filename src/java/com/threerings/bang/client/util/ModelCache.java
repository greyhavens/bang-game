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

import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of resolved 3D models.
 */
public class ModelCache
{
    public ModelCache (BasicContext ctx)
    {
        _ctx = ctx;
        
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
        Object value = _prototypes.get(key);
        if (value instanceof Model && rl != null) {
            rl.requestCompleted(createInstance(key, (Model)value, zations));
            
        } else if (value instanceof Exception && rl != null) {
            rl.requestFailed((Exception)value);
            
        } else if (value instanceof ResultListenerList && rl != null) {
            ((ResultListenerList<Model>)value).add(
                new InstanceCreator(key, zations, rl));
            
        } else if (value == null) {
            ResultListenerList<Model> rll = new ResultListenerList<Model>();
            if (rl != null) {
                rll.add(new InstanceCreator(key, zations, rl));
            }
            _prototypes.put(key, rll);
            _ctx.getInvoker().postUnit(new ModelLoader(key));
        }
    }

    /**
     * Creates and returns an instance of the specified model.
     *
     * @param zations the colorizations to apply to the model's textures, or
     * <code>null</code> for none
     */
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
    
    /**
     * Normalizes the provided relative path.
     */
    protected String cleanPath (String path)
    {
        String npath = path.replaceAll(PATH_DOTDOT, "");
        while (!npath.equals(path)) {
            path = npath;
            npath = path.replaceAll(PATH_DOTDOT, "");
        }
        return npath;
    }
    
    /** Loads a model on the invoker thread. */
    protected class ModelLoader extends Invoker.Unit
    {
        public ModelLoader (String key)
        {
            _key = key;
        }
        
        // documentation inherited
        public boolean invoke ()
        {
            File file = _ctx.getResourceManager().getResourceFile(
                _key + "/model.dat");
            long start = PerfMonitor.getCurrentMicros();
            int size = (int)file.length();
            try {
                _model = Model.readFromFile(file, size >= MIN_MAP_SIZE &&
                    ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
                _model.resolveTextures(new ModelTextureProvider(_key, null));
                PerfMonitor.recordModelLoad(start, size);
                
            } catch (Exception cause) {
                log.warning("Failed to load model [key=" + _key + ", cause=" +
                    cause + "].");
                _cause = cause;
            }
            return true;
        }
        
        @Override // documentation inherited
        public void handleResult ()
        {
            ResultListener<Model> rl =
                (ResultListener<Model>)_prototypes.get(_key);
            if (_model != null) {
                _model.lockStaticMeshes(_ctx.getRenderer(), Config.useVBOs,
                    Config.useDisplayLists);
                _prototypes.put(_key, _model);
                rl.requestCompleted(_model);
                
            } else {
                _prototypes.put(_key, _cause);
                rl.requestFailed(_cause);
            }
        }
        
        /** The name of the model to load. */
        protected String _key;
        
        /** In case of success, the loaded model. */
        protected Model _model;
        
        /** In case of failure, the cause of same. */
        protected Exception _cause;
    }
    
    /** Listens for the loading of a prototype in order to generate an
     * instance based on that prototype. */
    protected class InstanceCreator
        implements ResultListener<Model>
    {
        public InstanceCreator (
            String key, Colorization[] zations, ResultListener<Model> listener)
        {
            _key = key;
            _zations = zations;
            _listener = listener;
        }
        
        // documentation inherited from interface ResultListener
        public void requestCompleted (Model model)
        {
            _listener.requestCompleted(createInstance(_key, model, _zations));
        }
        
        // documentation inherited from interface ResultListener
        public void requestFailed (Exception cause)
        {
            _listener.requestFailed(cause);
        }
        
        /** The model key. */
        protected String _key;
        
        /** The colorizations to apply to the instance, or <code>null</code>
         * for none. */
        protected Colorization[] _zations;
        
        /** The listener waiting for the instance. */
        protected ResultListener<Model> _listener;
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
                tstate.setTexture(_zations == null ?
                    _ctx.getTextureCache().getTexture(path) :
                    _ctx.getTextureCache().getTexture(path, _zations));
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
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** Maps model keys to prototypes for models that have been loaded, to
     * {@link ResultListenerList}s for models being loaded, or to
     * {@link Exception}s for models that failed to load. */
    protected HashMap<String, Object> _prototypes =
        new HashMap<String, Object>();
    
    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";
    
    /** Models bigger than this have their buffers mapped into memory. */
    protected static final int MIN_MAP_SIZE = 32768;
}
