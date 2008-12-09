//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.WeakReference;

import java.util.HashMap;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultHandler;
import com.samskivert.util.ResultListener;

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
    protected void getPrototype (S key, final ResultListener<T> rl)
    {
        ResultHandler<WeakReference<T>> handler = _prototypes.get(key);
        if (handler != null) {
            // take a peek at the result.  if it's available and non-null, we can provide the
            // prototype immediately.  if it's available and null, the prototype has been
            // collected, so we must reload it
            WeakReference<T> ref = handler.peekResult();
            if (ref != null) {
                T result = ref.get();
                if (result == null) {
                    handler = null;
                } else {
                    if (rl != null) {
                        rl.requestCompleted(result);
                    }
                    return;
                }
            }
        }
        if (handler == null) {
            _prototypes.put(key, handler = new ResultHandler<WeakReference<T>>());
            postPrototypeLoader(key, handler);
        }
        if (rl == null) {
            return;
        }
        handler.getResult(new ResultListener<WeakReference<T>>() {
            public void requestCompleted (WeakReference<T> result) {
                // this will be called just after the reference is created, so it should
                // never be null
                rl.requestCompleted(result.get());
            }
            public void requestFailed (Exception cause) {
                rl.requestFailed(cause);
            }
        });
    }

    /**
     * Queues up a prototype loader for the identified object.
     */
    protected void postPrototypeLoader (final S key, final ResultHandler<WeakReference<T>> handler)
    {
        _ctx.getInvoker().postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _prototype = loadPrototype(key);
                } catch (Exception cause) {
                    log.warning("Failed to load prototype", "key", key, "cause", cause);
                    _cause = cause;
                }
                return true;
            }
            public void handleResult () {
                if (_prototype != null) {
                    initPrototype(_prototype);
                    handler.requestCompleted(createPrototypeReference(_prototype));
                } else {
                    handler.requestFailed(_cause);
                }
            }
            public long getLongThreshold () {
                return 3000L;  // this could take a while...
            }

            protected T _prototype;
            protected Exception _cause;
        });
    }

    /**
     * Creates a soft reference to the supplied prototype.
     */
    protected WeakReference<T> createPrototypeReference (T prototype)
    {
        return new WeakReference<T>(prototype);
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
    protected HashMap<S, ResultHandler<WeakReference<T>>> _prototypes =
        new HashMap<S, ResultHandler<WeakReference<T>>>();

    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";
}
