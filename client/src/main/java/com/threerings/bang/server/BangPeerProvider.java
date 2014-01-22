//
// $Id$

package com.threerings.bang.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.client.BangPeerService;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;

/**
 * Defines the server-side of the {@link BangPeerService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangPeerService.java.")
public interface BangPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangPeerService#deliverGangInvite} request.
     */
    void deliverGangInvite (ClientObject caller, Handle arg1, Handle arg2, int arg3, Handle arg4, String arg5);

    /**
     * Handles a {@link BangPeerService#deliverItem} request.
     */
    void deliverItem (ClientObject caller, Item arg1, String arg2);

    /**
     * Handles a {@link BangPeerService#deliverPardnerInvite} request.
     */
    void deliverPardnerInvite (ClientObject caller, Handle arg1, Handle arg2, String arg3);

    /**
     * Handles a {@link BangPeerService#deliverPardnerInviteResponse} request.
     */
    void deliverPardnerInviteResponse (ClientObject caller, Handle arg1, Handle arg2, boolean arg3, boolean arg4);

    /**
     * Handles a {@link BangPeerService#deliverPardnerRemoval} request.
     */
    void deliverPardnerRemoval (ClientObject caller, Handle arg1, Handle arg2);

    /**
     * Handles a {@link BangPeerService#getGangOid} request.
     */
    void getGangOid (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
