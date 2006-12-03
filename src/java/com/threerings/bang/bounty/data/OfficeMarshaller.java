//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link OfficeService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class OfficeMarshaller extends InvocationMarshaller
    implements OfficeService
{
    /** The method id used to dispatch {@link #playBountyGame} requests. */
    public static final int PLAY_BOUNTY_GAME = 1;

    // from interface OfficeService
    public void playBountyGame (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PLAY_BOUNTY_GAME, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #testBountyGame} requests. */
    public static final int TEST_BOUNTY_GAME = 2;

    // from interface OfficeService
    public void testBountyGame (Client arg1, BangConfig arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, TEST_BOUNTY_GAME, new Object[] {
            arg2, listener3
        });
    }
}
