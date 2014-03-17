//
// $Id$

package com.threerings.bang.saloon.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.ParlorService;

/**
 * Provides the implementation of the {@link ParlorService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from ParlorService.java.")
public class ParlorMarshaller extends InvocationMarshaller<PlayerObject>
    implements ParlorService
{
    /** The method id used to dispatch {@link #bootPlayer} requests. */
    public static final int BOOT_PLAYER = 1;

    // from interface ParlorService
    public void bootPlayer (int arg1)
    {
        sendRequest(BOOT_PLAYER, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #findSaloonMatch} requests. */
    public static final int FIND_SALOON_MATCH = 2;

    // from interface ParlorService
    public void findSaloonMatch (Criterion arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(FIND_SALOON_MATCH, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #leaveSaloonMatch} requests. */
    public static final int LEAVE_SALOON_MATCH = 3;

    // from interface ParlorService
    public void leaveSaloonMatch (int arg1)
    {
        sendRequest(LEAVE_SALOON_MATCH, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #updateParlorConfig} requests. */
    public static final int UPDATE_PARLOR_CONFIG = 4;

    // from interface ParlorService
    public void updateParlorConfig (ParlorInfo arg1, boolean arg2)
    {
        sendRequest(UPDATE_PARLOR_CONFIG, new Object[] {
            arg1, Boolean.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #updateParlorPassword} requests. */
    public static final int UPDATE_PARLOR_PASSWORD = 5;

    // from interface ParlorService
    public void updateParlorPassword (String arg1)
    {
        sendRequest(UPDATE_PARLOR_PASSWORD, new Object[] {
            arg1
        });
    }
}
