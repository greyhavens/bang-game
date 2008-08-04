//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.bang.saloon.client.TableGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link TableGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class TableGameMarshaller extends InvocationMarshaller
    implements TableGameService
{
    /** The method id used to dispatch {@link #joinMatch} requests. */
    public static final int JOIN_MATCH = 1;

    // from interface TableGameService
    public void joinMatch (Client arg1)
    {
        sendRequest(arg1, JOIN_MATCH, new Object[] {});
    }

    /** The method id used to dispatch {@link #joinMatchSlot} requests. */
    public static final int JOIN_MATCH_SLOT = 2;

    // from interface TableGameService
    public void joinMatchSlot (Client arg1, int arg2)
    {
        sendRequest(arg1, JOIN_MATCH_SLOT, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 3;

    // from interface TableGameService
    public void leaveMatch (Client arg1)
    {
        sendRequest(arg1, LEAVE_MATCH, new Object[] {});
    }

    /** The method id used to dispatch {@link #startMatchMaking} requests. */
    public static final int START_MATCH_MAKING = 4;

    // from interface TableGameService
    public void startMatchMaking (Client arg1, ParlorGameConfig arg2, byte[] arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, START_MATCH_MAKING, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updateGameConfig} requests. */
    public static final int UPDATE_GAME_CONFIG = 5;

    // from interface TableGameService
    public void updateGameConfig (Client arg1, ParlorGameConfig arg2)
    {
        sendRequest(arg1, UPDATE_GAME_CONFIG, new Object[] {
            arg2
        });
    }
}
