//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link ParlorService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ParlorMarshaller extends InvocationMarshaller
    implements ParlorService
{
    /** The method id used to dispatch {@link #bootPlayer} requests. */
    public static final int BOOT_PLAYER = 1;

    // from interface ParlorService
    public void bootPlayer (Client arg1, int arg2)
    {
        sendRequest(arg1, BOOT_PLAYER, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #findSaloonMatch} requests. */
    public static final int FIND_SALOON_MATCH = 2;

    // from interface ParlorService
    public void findSaloonMatch (Client arg1, Criterion arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FIND_SALOON_MATCH, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #leaveSaloonMatch} requests. */
    public static final int LEAVE_SALOON_MATCH = 3;

    // from interface ParlorService
    public void leaveSaloonMatch (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_SALOON_MATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #updateParlorConfig} requests. */
    public static final int UPDATE_PARLOR_CONFIG = 4;

    // from interface ParlorService
    public void updateParlorConfig (Client arg1, ParlorInfo arg2, boolean arg3)
    {
        sendRequest(arg1, UPDATE_PARLOR_CONFIG, new Object[] {
            arg2, Boolean.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #updateParlorPassword} requests. */
    public static final int UPDATE_PARLOR_PASSWORD = 5;

    // from interface ParlorService
    public void updateParlorPassword (Client arg1, String arg2)
    {
        sendRequest(arg1, UPDATE_PARLOR_PASSWORD, new Object[] {
            arg2
        });
    }
}
