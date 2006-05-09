//
// $Id$

package com.threerings.bang.client.util;

import java.io.File;

import java.nio.ByteOrder;

import java.util.HashMap;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;

import com.samskivert.util.Queue;
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
            _loader.queueModel(key);
            if (!_loader.isAlive()) {
                // create a texture state here in order to make sure that the
                // texture state initialization isn't called from the loader
                _ctx.getRenderer().createTextureState();
                _loader.start();
            }
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
     * Loads the specified model.  This method is called from the model loader
     * thread.
     *
     * @return the model's file size
     */
    protected int loadModel (String key)
    {
        File file = _ctx.getResourceManager().getResourceFile(
            key + "/model.dat");
        int fsize = (int)file.length();
        try {
            Model model = Model.readFromFile(file, fsize >= MIN_MAP_SIZE &&
                ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
            loadModelCompleted(key, model);
            return fsize;
            
        } catch (Exception cause) {
            loadModelFailed(key, cause);
            return 0;
        }
    }

    /**
     * Called on the model loader thread when a model has been successfully
     * loaded.
     */
    protected void loadModelCompleted (final String key, final Model model)
    {
        // resolve the model's textures
        model.resolveTextures(new ModelTextureProvider(key, null));
        
        // lock the model and report the result on the main thread
        _ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                model.lockStaticMeshes(_ctx.getRenderer(), Config.useVBOs,
                    Config.useDisplayLists);
                ResultListener<Model> rl =
                    (ResultListener<Model>)_prototypes.get(key);
                _prototypes.put(key, model);
                rl.requestCompleted(model);
            }
        });
    }
    
    /**
     * Called on the model loader thread when a model has failed to load.
     */
    protected void loadModelFailed (final String key, final Exception cause)
    {
        // log the error
        log.warning("Failed to load model [key=" + key + ", cause=" + cause +
            "].");
        
        // report the result on the main thread
        _ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                ResultListener<Model> rl =
                    (ResultListener<Model>)_prototypes.get(key);
                _prototypes.put(key, cause);
                rl.requestFailed(cause);
            }
        });
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
    
    /** Loads models in a separate thread. */    
    protected class ModelLoader extends Thread
    {
        public ModelLoader ()
        {
            super("ModelLoader");
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }
        
        /** Queues a model up for loading. */
        public void queueModel (String key)
        {
            _queue.append(key);
        }
        
        @Override // documentation inherited
        public void run ()
        {
            while (true) {
                String key = (String)_queue.get();
                if (key == null) {
                    continue;
                }
                long start = PerfMonitor.getCurrentMicros();
                PerfMonitor.recordModelLoad(start, loadModel(key));
            }
        }
        
        /** The queue of keys representing models to load. */
        protected Queue _queue = new Queue();
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
    
    /** The model loader thread. */
    protected ModelLoader _loader = new ModelLoader();
    
    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";
    
    /** Models bigger than this have their buffers mapped into memory. */
    protected static final int MIN_MAP_SIZE = 32768;
}
