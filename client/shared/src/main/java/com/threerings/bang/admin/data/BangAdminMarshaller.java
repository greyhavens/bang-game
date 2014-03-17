//
// $Id$

package com.threerings.bang.admin.data;

import javax.annotation.Generated;

import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.admin.client.BangAdminService;
import com.threerings.bang.data.PlayerObject;

/**
 * Provides the implementation of the {@link BangAdminService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangAdminService.java.")
public class BangAdminMarshaller extends InvocationMarshaller<PlayerObject>
    implements BangAdminService
{
    /** The method id used to dispatch {@link #scheduleReboot} requests. */
    public static final int SCHEDULE_REBOOT = 1;

    // from interface BangAdminService
    public void scheduleReboot (int arg1)
    {
        sendRequest(SCHEDULE_REBOOT, new Object[] {
            Integer.valueOf(arg1)
        });
    }
}
