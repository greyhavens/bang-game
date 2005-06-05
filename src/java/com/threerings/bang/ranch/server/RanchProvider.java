//
// $Id$

package com.threerings.bang.ranch.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.ranch.client.RanchService;

/**
 * Defines the server-side of {@link RanchService}.
 */
public interface RanchProvider extends InvocationProvider
{
    /**
     * Handles a {@link RanchService#recruitBigShot} request.
     */
    public void recruitBigShot (
        ClientObject caller, String type, RanchService.ResultListener listener)
        throws InvocationException;
}
