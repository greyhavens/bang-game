//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.Client;
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
    /** The method id used to dispatch {@link #joinMatch} requests. */
    public static final int JOIN_MATCH = 1;

    // documentation inherited from interface
    public void joinMatch (Client arg1)
    {
        sendRequest(arg1, JOIN_MATCH, new Object[] {
            
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 2;

    // documentation inherited from interface
    public void leaveMatch (Client arg1)
    {
        sendRequest(arg1, LEAVE_MATCH, new Object[] {
            
        });
    }

    /** The method id used to dispatch {@link #startMatchMaking} requests. */
    public static final int START_MATCH_MAKING = 3;

    // documentation inherited from interface
    public void startMatchMaking (Client arg1, ParlorGameConfig arg2)
    {
        sendRequest(arg1, START_MATCH_MAKING, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #updateGameConfig} requests. */
    public static final int UPDATE_GAME_CONFIG = 4;

    // documentation inherited from interface
    public void updateGameConfig (Client arg1, ParlorGameConfig arg2)
    {
        sendRequest(arg1, UPDATE_GAME_CONFIG, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #updateParlorConfig} requests. */
    public static final int UPDATE_PARLOR_CONFIG = 5;

    // documentation inherited from interface
    public void updateParlorConfig (Client arg1, ParlorInfo arg2, boolean arg3)
    {
        sendRequest(arg1, UPDATE_PARLOR_CONFIG, new Object[] {
            arg2, new Boolean(arg3)
        });
    }

    /** The method id used to dispatch {@link #updateParlorPassword} requests. */
    public static final int UPDATE_PARLOR_PASSWORD = 6;

    // documentation inherited from interface
    public void updateParlorPassword (Client arg1, String arg2)
    {
        sendRequest(arg1, UPDATE_PARLOR_PASSWORD, new Object[] {
            arg2
        });
    }

}
