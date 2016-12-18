//
// $Id$

package com.threerings.bang.bounty.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangConfig;

/**
 * Provides the implementation of the {@link OfficeService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from OfficeService.java.")
public class OfficeMarshaller extends InvocationMarshaller<PlayerObject>
    implements OfficeService
{
    /** The method id used to dispatch {@link #testBountyGame} requests. */
    public static final int TEST_BOUNTY_GAME = 1;

    // from interface OfficeService
    public void testBountyGame (BangConfig arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(TEST_BOUNTY_GAME, new Object[] {
            arg1, listener2
        });
    }
}
