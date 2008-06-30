//
// $Id$

package com.threerings.bang.game.server;

import com.threerings.bang.game.client.BangService;
import com.threerings.bang.game.data.BangMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BangProvider}.
 */
public class BangDispatcher extends InvocationDispatcher<BangMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BangDispatcher (BangProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public BangMarshaller createMarshaller ()
    {
        return new BangMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BangMarshaller.CANCEL_ORDER:
            ((BangProvider)provider).cancelOrder(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case BangMarshaller.GET_BOARD:
            ((BangProvider)provider).getBoard(
                source,
                (BangService.BoardListener)args[0]
            );
            return;

        case BangMarshaller.ORDER:
            ((BangProvider)provider).order(
                source,
                ((Integer)args[0]).intValue(), ((Short)args[1]).shortValue(), ((Short)args[2]).shortValue(), ((Integer)args[3]).intValue(), (InvocationService.ResultListener)args[4]
            );
            return;

        case BangMarshaller.PLAY_CARD:
            ((BangProvider)provider).playCard(
                source,
                ((Integer)args[0]).intValue(), args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case BangMarshaller.REPORT_PERFORMANCE:
            ((BangProvider)provider).reportPerformance(
                source,
                (String)args[0], (String)args[1], (int[])args[2]
            );
            return;

        case BangMarshaller.SELECT_TEAM:
            ((BangProvider)provider).selectTeam(
                source,
                ((Integer)args[0]).intValue(), (String[])args[1], (int[])args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
