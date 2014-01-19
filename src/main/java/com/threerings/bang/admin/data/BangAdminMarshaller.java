//
// $Id$

package com.threerings.bang.admin.data;

import com.threerings.bang.admin.client.BangAdminService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link BangAdminService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangAdminMarshaller extends InvocationMarshaller
    implements BangAdminService
{
    /** The method id used to dispatch {@link #scheduleReboot} requests. */
    public static final int SCHEDULE_REBOOT = 1;

    // from interface BangAdminService
    public void scheduleReboot (Client arg1, int arg2)
    {
        sendRequest(arg1, SCHEDULE_REBOOT, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
