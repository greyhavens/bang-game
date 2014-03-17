//
// $Id$

package com.threerings.bang.gang.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.client.GangService;

/**
 * Defines the server-side of the {@link GangService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GangService.java.")
public interface GangProvider extends InvocationProvider
{
    /**
     * Handles a {@link GangService#getGangInfo} request.
     */
    void getGangInfo (PlayerObject caller, Handle arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link GangService#inviteMember} request.
     */
    void inviteMember (PlayerObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
