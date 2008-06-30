//
// $Id$

package com.threerings.bang.bounty.server;

import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link OfficeProvider}.
 */
public class OfficeDispatcher extends InvocationDispatcher<OfficeMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public OfficeDispatcher (OfficeProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public OfficeMarshaller createMarshaller ()
    {
        return new OfficeMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case OfficeMarshaller.TEST_BOUNTY_GAME:
            ((OfficeProvider)provider).testBountyGame(
                source,
                (BangConfig)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
