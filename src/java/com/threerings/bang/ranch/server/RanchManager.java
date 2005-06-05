//
// $Id$

package com.threerings.bang.ranch.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.server.BangServer;

/**
 * Provides ranch-related services.
 */
public class RanchManager
    implements RanchProvider
{
    /**
     * Prepares the ranch manager for operation.
     */
    public void init (InvocationManager invmgr)
    {
        // register ourselves with the invocation manager
        invmgr.registerDispatcher(new RanchDispatcher(this), true);
    }

    // documentation inherited from interface RanchProvider
    public void recruitBigShot (
        ClientObject caller, String type, RanchService.ResultListener listener)
        throws InvocationException
    {
        // TODO
    }
}
