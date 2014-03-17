//
// $Id$

package com.threerings.bang.ranch.data;

import javax.annotation.Generated;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.ranch.client.RanchService;

/**
 * Provides the implementation of the {@link RanchService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from RanchService.java.")
public class RanchMarshaller extends InvocationMarshaller<PlayerObject>
    implements RanchService
{
    /** The method id used to dispatch {@link #recruitBigShot} requests. */
    public static final int RECRUIT_BIG_SHOT = 1;

    // from interface RanchService
    public void recruitBigShot (String arg1, Name arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(RECRUIT_BIG_SHOT, new Object[] {
            arg1, arg2, listener3
        });
    }
}
