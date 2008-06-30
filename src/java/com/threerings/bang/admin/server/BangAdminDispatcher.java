//
// $Id$

package com.threerings.bang.admin.server;

import com.threerings.bang.admin.data.BangAdminMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BangAdminProvider}.
 */
public class BangAdminDispatcher extends InvocationDispatcher<BangAdminMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BangAdminDispatcher (BangAdminProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public BangAdminMarshaller createMarshaller ()
    {
        return new BangAdminMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BangAdminMarshaller.SCHEDULE_REBOOT:
            ((BangAdminProvider)provider).scheduleReboot(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
