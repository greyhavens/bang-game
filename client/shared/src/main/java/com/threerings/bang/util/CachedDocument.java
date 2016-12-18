//
// $Id$

package com.threerings.bang.util;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import com.samskivert.util.ResultListener;

/**
 * Maintains a cached copy of a document loaded from a source URL. Refreshes the document contents
 * on a user specified interval.
 */
public class CachedDocument
{
    /**
     * Creates a cached document from the specified source and with the specified refresh
     * interval. The document will not be loaded until the first call to {@link #refreshDocument}.
     */
    public CachedDocument (URL source, long refreshInterval)
    {
        _source = source;
        _refreshInterval = refreshInterval;
    }

    /**
     * Requests that the document be refreshed. The supplied listener will be notified on
     * successful or failed refresh. <em>Note:</em> the listener will be notified on an unsafe
     * thread, the supplied {@link ResultListener} <em>must</em> get itself back on the normal
     * client thread before doing anything meaningful.
     *
     * @return true if a refresh was initiated (in which case the result listener will be called
     * back when the reresh is complete), false if no refresh was needed.
     */
    public boolean refreshDocument (boolean force, final ResultListener<String> rl)
    {
        // reload the news if necessary
        long now = System.currentTimeMillis();
        if (!force && _nextRefresh > now) {
            return false;
        }

        _nextRefresh = now + _refreshInterval;
        new Thread() {
            public void run () {
                try {
                    setDocument(IOUtils.toString(_source.openStream(), "UTF-8"));
                    rl.requestCompleted(getDocument());
                } catch (IOException ioe) {
                    rl.requestFailed(ioe);
                }
            }
        }.start();
        return true;
    }

    /**
     * Returns the most recently loaded version of the document, or null if it has not yet been
     * loaded.
     */
    public synchronized String getDocument ()
    {
        return _document;
    }

    protected synchronized void setDocument (String document)
    {
        _document = document;
    }

    /** The URL from which we load our document. */
    protected URL _source;

    /** Tracks the last time we refreshed the document. */
    protected long _nextRefresh;

    /** The frequency with which we reload the document. */
    protected long _refreshInterval;

    /** The most recently loaded document. */
    protected String _document;
}
