//
// $Id$

package com.threerings.bang.client.util;

import java.util.PriorityQueue;
import java.util.logging.Level;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * A thread that runs in the background, asynchronously loading models and
 * textures.
 */
public class ModelLoader extends Thread
{
    /**
     * Creates, but does not start, a model loading thread.
     */
    public ModelLoader ()
    {
        super("ModelLoader");
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    /**
     * Queues an action to be resolved in the background.
     */
    public synchronized void queueAction (Model model, String action)
    {
        _queue.offer(new PendingActionKey(model, action));
        notify();
    }

    /**
     * Returns the number of pending loads on the queue.
     */
    public synchronized int getQueueSize ()
    {
        return _queue.size();
    }

    @Override // documentation inherited
    public void run ()
    {
        while (true) {
            synchronized (this) {
                while (_queue.peek() == null) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
            PendingActionKey akey = _queue.poll();
            try {
                akey.resolveAction();
            } catch (Throwable t) {
                log.log(Level.WARNING,
                        "Choked resolving action: " + akey + ".", t);
            }
        }
    }

    /** Used to queue up actions for loading. */
    protected static class PendingActionKey
        implements Comparable<PendingActionKey>
    {
        public PendingActionKey (Model model, String action) {
            _model = model;
            _action = action;
            if (action.equals("standing")) {
                _priority = 3;
            } else if (action.equals("normal")) {
                _priority = 2;
            } else if (action.equals("walking")) {
                _priority = 1;
            } else {
                _priority = 0;
            }
        }

        public void resolveAction () {
            _model.resolveAction(_action);
        }

        public int compareTo (PendingActionKey other) {
            return other._priority - _priority;
        }

        protected Model _model;
        protected String _action;
        protected int _priority;
    }

    protected PriorityQueue<PendingActionKey> _queue =
        new PriorityQueue<PendingActionKey>();
}
