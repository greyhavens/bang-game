//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.BangPeerService;
import com.threerings.bang.data.Item;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BangPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangPeerMarshaller extends InvocationMarshaller
    implements BangPeerService
{
    /** The method id used to dispatch {@link #deliverItem} requests. */
    public static final int DELIVER_ITEM = 1;

    // from interface BangPeerService
    public void deliverItem (Client arg1, Item arg2, String arg3)
    {
        sendRequest(arg1, DELIVER_ITEM, new Object[] {
            arg2, arg3
        });
    }
}
