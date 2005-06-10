//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.BangService;
import com.threerings.bang.data.BangMarshaller;
import com.threerings.bang.data.piece.Piece;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BangProvider}.
 */
public class BangDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BangDispatcher (BangProvider provider)
    {
        this.provider = provider;
    }

    // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new BangMarshaller();
    }

    // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BangMarshaller.MOVE:
            ((BangProvider)provider).move(
                source,
                ((Integer)args[0]).intValue(), ((Short)args[1]).shortValue(), ((Short)args[2]).shortValue(), ((Integer)args[3]).intValue(), (InvocationService.InvocationListener)args[4]
            );
            return;

        case BangMarshaller.PLAY_CARD:
            ((BangProvider)provider).playCard(
                source,
                ((Integer)args[0]).intValue(), ((Short)args[1]).shortValue(), ((Short)args[2]).shortValue()
            );
            return;

        case BangMarshaller.PURCHASE_PIECES:
            ((BangProvider)provider).purchasePieces(
                source,
                (Piece[])args[0]
            );
            return;

        case BangMarshaller.SELECT_STARTERS:
            ((BangProvider)provider).selectStarters(
                source,
                ((Integer)args[0]).intValue(), (int[])args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
        }
    }
}
