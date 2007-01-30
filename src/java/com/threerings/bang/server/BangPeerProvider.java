//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.BangPeerService;
import com.threerings.bang.data.Item;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BangPeerService}.
 */
public interface BangPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangPeerService#deliverItem} request.
     */
    public void deliverItem (ClientObject caller, Item arg1, String arg2);
}
