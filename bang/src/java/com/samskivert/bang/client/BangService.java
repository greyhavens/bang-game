//
// $Id$

package com.samskivert.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.samskivert.bang.data.PiecePath;

/**
 * Defines the requests that the client can make to the server.
 */
public interface BangService extends InvocationService
{
    /**
     * Requests that the specified piece be moved along the specified path.
     */
    public void setPath (Client client, PiecePath path);
}
