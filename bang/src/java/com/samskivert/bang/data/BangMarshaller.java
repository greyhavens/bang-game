//
// $Id$

package com.samskivert.bang.data;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.PiecePath;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangMarshaller extends InvocationMarshaller
    implements BangService
{
    /** The method id used to dispatch {@link #setPath} requests. */
    public static final int SET_PATH = 1;

    // documentation inherited from interface
    public void setPath (Client arg1, PiecePath arg2)
    {
        sendRequest(arg1, SET_PATH, new Object[] {
            arg2
        });
    }

}
