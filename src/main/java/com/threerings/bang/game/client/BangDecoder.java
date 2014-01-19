//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.BangReceiver;
import com.threerings.presents.client.InvocationDecoder;

/**
 * Dispatches calls to a {@link BangReceiver} instance.
 */
public class BangDecoder extends InvocationDecoder
{
    /** The generated hash code used to identify this receiver class. */
    public static final String RECEIVER_CODE = "6bcabf165dafb8d004ff59650e7265f6";

    /** The method id used to dispatch {@link BangReceiver#orderInvalidated}
     * notifications. */
    public static final int ORDER_INVALIDATED = 1;

    /**
     * Creates a decoder that may be registered to dispatch invocation
     * service notifications to the specified receiver.
     */
    public BangDecoder (BangReceiver receiver)
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
        case ORDER_INVALIDATED:
            ((BangReceiver)receiver).orderInvalidated(
                ((Integer)args[0]).intValue(), (String)args[1]
            );
            return;

        default:
            super.dispatchNotification(methodId, args);
            return;
        }
    }
}
