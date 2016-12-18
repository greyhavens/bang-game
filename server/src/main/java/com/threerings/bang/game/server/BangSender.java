//
// $Id$

package com.threerings.bang.game.server;

import com.threerings.bang.game.client.BangDecoder;
import com.threerings.bang.game.client.BangReceiver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationSender;

/**
 * Used to issue notifications to a {@link BangReceiver} instance on a
 * client.
 */
public class BangSender extends InvocationSender
{
    /**
     * Issues a notification that will result in a call to {@link
     * BangReceiver#orderInvalidated} on a client.
     */
    public static void orderInvalidated (
        ClientObject target, int arg1, String arg2)
    {
        sendNotification(
            target, BangDecoder.RECEIVER_CODE, BangDecoder.ORDER_INVALIDATED,
            new Object[] { Integer.valueOf(arg1), arg2 });
    }

}
