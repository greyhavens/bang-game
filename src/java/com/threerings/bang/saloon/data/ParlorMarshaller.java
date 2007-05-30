//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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

    /** The method id used to dispatch {@link #joinMatch} requests. */
    public static final int JOIN_MATCH = 3;

    // from interface ParlorService
    public void joinMatch (Client arg1)
    {
        sendRequest(arg1, JOIN_MATCH, new Object[] {
            
        });
    }

    /** The method id used to dispatch {@link #joinMatchSlot} requests. */
    public static final int JOIN_MATCH_SLOT = 4;

    // from interface ParlorService
    public void joinMatchSlot (Client arg1, int arg2)
    {
        sendRequest(arg1, JOIN_MATCH_SLOT, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 5;

    // from interface ParlorService
    public void leaveMatch (Client arg1)
    {
        sendRequest(arg1, LEAVE_MATCH, new Object[] {
            
        });
    }

    /** The method id used to dispatch {@link #leaveSaloonMatch} requests. */
    public static final int LEAVE_SALOON_MATCH = 6;

    // from interface ParlorService
    public void leaveSaloonMatch (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_SALOON_MATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #startMatchMaking} requests. */
    public static final int START_MATCH_MAKING = 7;

    // from interface ParlorService
    public void startMatchMaking (Client arg1, ParlorGameConfig arg2, byte[] arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, START_MATCH_MAKING, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updateGameConfig} requests. */
    public static final int UPDATE_GAME_CONFIG = 8;

    // from interface ParlorService
    public void updateGameConfig (Client arg1, ParlorGameConfig arg2)
    {
        sendRequest(arg1, UPDATE_GAME_CONFIG, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #updateParlorConfig} requests. */
    public static final int UPDATE_PARLOR_CONFIG = 9;

    // from interface ParlorService
    public void updateParlorConfig (Client arg1, ParlorInfo arg2, boolean arg3)
    {
        sendRequest(arg1, UPDATE_PARLOR_CONFIG, new Object[] {
            arg2, Boolean.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #updateParlorPassword} requests. */
    public static final int UPDATE_PARLOR_PASSWORD = 10;

    // from interface ParlorService
    public void updateParlorPassword (Client arg1, String arg2)
    {
        sendRequest(arg1, UPDATE_PARLOR_PASSWORD, new Object[] {
            arg2
        });
    }
}
