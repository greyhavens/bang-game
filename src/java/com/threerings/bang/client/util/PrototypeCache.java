//
// $Id$

package com.threerings.bang.client.util;

import java.util.HashMap;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;

import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * The base class of caches of prototypes used to create cloned instances.
 */
public abstract class PrototypeCache<T>
{
    public PrototypeCache (BasicContext ctx)
    {
        _ctx = ctx;
    }
    
    /**
     * Creates an instance of the identified object, loading it on the invoker
     * thread if necessary.
     */
    protected void getInstance (
        String key, Colorization[] zations, ResultListener<T> rl)
    {
        Object value = _prototypes.get(key);
        if (value == null) {
            ResultListenerList<T> rll = new ResultListenerList<T>();
            if (rl != null) {
                rll.add(new InstanceCreator(key, zations, rl));
            }
            _prototypes.put(key, rll);
            _ctx.getInvoker().postUnit(new PrototypeLoader(key));
            
        } else if (rl == null) {
            return;
            
        } else if (value instanceof ResultListenerList) {
            @SuppressWarnings("unchecked") ResultListenerList<T> rll =
                (ResultListenerList<T>)value;
            rll.add(new InstanceCreator(key, zations, rl));
            
        } else if (value instanceof Exception) {
            rl.requestFailed((Exception)value);
            
        } else {
            @SuppressWarnings("unchecked") T prototype = (T)value;
            rl.requestCompleted(createInstance(key, prototype, zations));
        }
    }
    
    /**
     * Loads the prototype corresponding to the given key on the invoker
     * thread.
     */
    protected abstract T loadPrototype (String key)
        throws Exception;
    
    /**
     * Initializes the newly loaded prototype on the main thread.
     */
    protected abstract void initPrototype (T prototype);
    
    /**
     * Creates and returns a new instance from the given prototype.
     */
    protected abstract T createInstance (
        String key, T prototype, Colorization[] zations);
    
    /**
     * Normalizes the provided relative path.
     */
    protected static String cleanPath (String path)
    {
        String npath = path.replaceAll(PATH_DOTDOT, "");
        while (!npath.equals(path)) {
            path = npath;
            npath = path.replaceAll(PATH_DOTDOT, "");
        }
        return npath;
    }
    
    /** Listens for the loading of a prototype in order to generate an
     * instance based on that prototype. */
    protected class InstanceCreator
        implements ResultListener<T>
    {
        public InstanceCreator (
            String key, Colorization[] zations, ResultListener<T> listener)
        {
            _key = key;
            _zations = zations;
            _listener = listener;
        }
        
        // documentation inherited from interface ResultListener
        public void requestCompleted (T result)
        {
            _listener.requestCompleted(createInstance(_key, result, _zations));
        }
        
        // documentation inherited from interface ResultListener
        public void requestFailed (Exception cause)
        {
            _listener.requestFailed(cause);
        }
        
        /** The object key. */
        protected String _key;
        
        /** The colorizations to apply to the instance, or <code>null</code>
         * for none. */
        protected Colorization[] _zations;
        
        /** The listener waiting for the instance. */
        protected ResultListener<T> _listener;
    }
    
    /** Loads a prototype on the invoker thread. */
    protected class PrototypeLoader extends Invoker.Unit
    {
        public PrototypeLoader (String key)
        {
            _key = key;
        }
        
        // documentation inherited
        public boolean invoke ()
        {
            try {
                _prototype = loadPrototype(_key);
            } catch (Exception cause) {
                log.warning("Failed to load prototype [key=" + _key +
                    ", cause=" + cause + "].");
                _cause = cause;
            }
            return true;
        }
        
        @Override // documentation inherited
        public void handleResult ()
        {
            @SuppressWarnings("unchecked") ResultListener<T> rl =
                (ResultListener<T>)_prototypes.get(_key);
            if (_prototype != null) {
                initPrototype(_prototype);
                _prototypes.put(_key, _prototype);
                rl.requestCompleted(_prototype);
                
            } else {
                _prototypes.put(_key, _cause);
                rl.requestFailed(_cause);
            }
        }
        
        /** The name of the prototype to load. */
        protected String _key;
        
        /** In case of success, the loaded prototype. */
        protected T _prototype;
        
        /** In case of failure, the cause of same. */
        protected Exception _cause;
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** Maps keys to prototypes for those that have been loaded, to
     * {@link ResultListenerList}s for those being loaded, or to
     * {@link Exception}s for those that failed to load. */
    protected HashMap<String, Object> _prototypes =
        new HashMap<String, Object>();
    
    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";
}
