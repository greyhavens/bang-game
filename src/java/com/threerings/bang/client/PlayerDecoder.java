//
// $Id$

package com.threerings.bang.client;

import com.threerings.bang.client.PlayerReceiver;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.InvocationDecoder;

/**
 * Dispatches calls to a {@link PlayerReceiver} instance.
 */
public class PlayerDecoder extends InvocationDecoder
{
    /** The generated hash code used to identify this receiver class. */
    public static final String RECEIVER_CODE = "bdb36c035fe36d7e951a5fb4181462d6";

    /** The method id used to dispatch {@link PlayerReceiver#receivedPardnerInvite}
     * notifications. */
    public static final int RECEIVED_PARDNER_INVITE = 1;

    /**
     * Creates a decoder that may be registered to dispatch invocation
     * service notifications to the specified receiver.
     */
    public PlayerDecoder (PlayerReceiver receiver)
    {
        this.receiver = receiver;
    }

    // documentation inherited
    public String getReceiverCode ()
    {
        return RECEIVER_CODE;
    }

    // documentation inherited
    public void dispatchNotification (int methodId, Object[] args)
    {
        switch (methodId) {
        case RECEIVED_PARDNER_INVITE:
            ((PlayerReceiver)receiver).receivedPardnerInvite(
                (Handle)args[0]
            );
            return;

        default:
            super.dispatchNotification(methodId, args);
        }
    }
}
