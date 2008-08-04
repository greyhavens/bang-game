//
// $Id$

package com.threerings.bang.ranch.server;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.util.Name;

/**
 * Defines the server-side of the {@link RanchService}.
 */
public interface RanchProvider extends InvocationProvider
{
    /**
     * Handles a {@link RanchService#recruitBigShot} request.
     */
    void recruitBigShot (ClientObject caller, String arg1, Name arg2, InvocationService.ResultListener arg3)
        throws InvocationException;
}
