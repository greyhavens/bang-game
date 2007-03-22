//
// $Id$

package com.threerings.bang.client.util;

import java.util.HashMap;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultHandler;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;

import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * The base class of caches of prototypes used to create cloned instances.
 */
public abstract class PrototypeCache<S, T>
{
    public PrototypeCache (BasicContext ctx)
    {
        _ctx = ctx;
        _prototypes = createCache();
    }

    /**
     * Creates the cache object to store the prototype handlers.
     */
    protected SoftCache<S, ResultHandler<T>> createCache ()
    {
        return new SoftCache<S, ResultHandler<T>>();
    }

    /**
     * Creates an instance of the identified object, loading its prototype on the invoker thread if
     * necessary.
     *
     * @param rl the listener to receive the instance, or <code>null</code> to preload the
     * prototype.
     */
    protected void getInstance (
        S key, Colorization[] zations, ResultListener<T> rl)
    {
        getPrototype(key, (rl == null) ? null : new InstanceCreator(key, zations, rl));
    }

    /**
     * Fetches the prototype for the identified object, loading it on the invoker thread if
     * necessary.
     *
     * @param rl the listener to receive the instance, or <code>null</code> to preload the
     * prototype.
     */
    protected void getPrototype (S key, ResultListener<T> rl)
    {
        ResultHandler<T> handler = _prototypes.get(key);
        if (handler == null) {
            _prototypes.put(key, handler = new ResultHandler<T>());
            postPrototypeLoader(key, handler);
        }
        if (rl != null) {
            handler.getResult(rl);
        }
    }

    /**
     * Queues up a prototype loader for the identified object.
     */
    protected void postPrototypeLoader (final S key, final ResultHandler<T> handler)
    {
        _ctx.getInvoker().postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _prototype = loadPrototype(key);
                } catch (Exception cause) {
                    log.warning("Failed to load prototype [key=" + key + ", cause=" +
                        cause + "].");
                    _cause = cause;
                }
                return true;
            }
            public void handleResult () {
                if (_prototype != null) {
                    initPrototype(_prototype);
                    handler.requestCompleted(_prototype);
                } else {
                    handler.requestFailed(_cause);
                }
            }
            protected T _prototype;
            protected Exception _cause;
        });
    }

    /**
     * Loads the prototype corresponding to the given key on the invoker thread.
     */
    protected abstract T loadPrototype (S key)
        throws Exception;

    /**
     * Initializes the newly loaded prototype on the main thread.
     */
    protected abstract void initPrototype (T prototype);

    /**
     * Creates and returns a new instance from the given prototype.
     */
    protected abstract T createInstance (S key, T prototype, Colorization[] zations);

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

    /** Listens for the loading of a prototype in order to generate an instance based on that
     * prototype. */
    protected class InstanceCreator
        implements ResultListener<T>
    {
        public InstanceCreator (
            S key, Colorization[] zations, ResultListener<T> listener)
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
        protected S _key;

        /** The colorizations to apply to the instance, or <code>null</code> for none. */
        protected Colorization[] _zations;

        /** The listener waiting for the instance. */
        protected ResultListener<T> _listener;
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** Maps keys to prototype handlers. */
    protected SoftCache<S, ResultHandler<T>> _prototypes;

    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";
}
