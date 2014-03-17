//
// $Id$

package com.threerings.bang.ranch.server;

import javax.annotation.Generated;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.ranch.client.RanchService;

/**
 * Defines the server-side of the {@link RanchService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from RanchService.java.")
public interface RanchProvider extends InvocationProvider
{
    /**
     * Handles a {@link RanchService#recruitBigShot} request.
     */
    void recruitBigShot (PlayerObject caller, String arg1, Name arg2, InvocationService.ResultListener arg3)
        throws InvocationException;
}
