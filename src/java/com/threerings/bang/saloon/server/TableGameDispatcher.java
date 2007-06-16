//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.TableGameService;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.TableGameMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link TableGameProvider}.
 */
public class TableGameDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public TableGameDispatcher (TableGameProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new TableGameMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case TableGameMarshaller.JOIN_MATCH:
            ((TableGameProvider)provider).joinMatch(
                source                
            );
            return;

        case TableGameMarshaller.JOIN_MATCH_SLOT:
            ((TableGameProvider)provider).joinMatchSlot(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case TableGameMarshaller.LEAVE_MATCH:
            ((TableGameProvider)provider).leaveMatch(
                source                
            );
            return;

        case TableGameMarshaller.START_MATCH_MAKING:
            ((TableGameProvider)provider).startMatchMaking(
                source,
                (ParlorGameConfig)args[0], (byte[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case TableGameMarshaller.UPDATE_GAME_CONFIG:
            ((TableGameProvider)provider).updateGameConfig(
                source,
                (ParlorGameConfig)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
