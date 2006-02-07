//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.PlayerDecoder;
import com.threerings.bang.client.PlayerReceiver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationSender;
import com.threerings.util.Name;

/**
 * Used to issue notifications to a {@link PlayerReceiver} instance on a
 * client.
 */
public class PlayerSender extends InvocationSender
{
    /**
     * Issues a notification that will result in a call to {@link
     * PlayerReceiver#receivedPardnerInvite} on a client.
     */
    public static void sendPardnerInvite (
        ClientObject target, Name arg1)
    {
        sendNotification(
            target, PlayerDecoder.RECEIVER_CODE, PlayerDecoder.RECEIVED_PARDNER_INVITE,
            new Object[] { arg1 });
    }

}
