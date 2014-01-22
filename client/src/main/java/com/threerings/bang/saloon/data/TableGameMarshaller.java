//
// $Id$

package com.threerings.bang.saloon.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.TableGameService;

/**
 * Provides the implementation of the {@link TableGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TableGameService.java.")
public class TableGameMarshaller extends InvocationMarshaller<PlayerObject>
    implements TableGameService
{
    /** The method id used to dispatch {@link #joinMatch} requests. */
    public static final int JOIN_MATCH = 1;

    // from interface TableGameService
    public void joinMatch ()
    {
        sendRequest(JOIN_MATCH, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #joinMatchSlot} requests. */
    public static final int JOIN_MATCH_SLOT = 2;

    // from interface TableGameService
    public void joinMatchSlot (int arg1)
    {
        sendRequest(JOIN_MATCH_SLOT, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 3;

    // from interface TableGameService
    public void leaveMatch ()
    {
        sendRequest(LEAVE_MATCH, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #startMatchMaking} requests. */
    public static final int START_MATCH_MAKING = 4;

    // from interface TableGameService
    public void startMatchMaking (ParlorGameConfig arg1, byte[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(START_MATCH_MAKING, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #updateGameConfig} requests. */
    public static final int UPDATE_GAME_CONFIG = 5;

    // from interface TableGameService
    public void updateGameConfig (ParlorGameConfig arg1)
    {
        sendRequest(UPDATE_GAME_CONFIG, new Object[] {
            arg1
        });
    }
}
