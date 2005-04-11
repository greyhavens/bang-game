//
// $Id$

package com.samskivert.bang.server;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.BangMarshaller;
import com.samskivert.bang.data.piece.Piece;
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

        case BangMarshaller.PURCHASE_PIECE:
            ((BangProvider)provider).purchasePiece(
                source,
                (Piece)args[0]
            );
            return;

        case BangMarshaller.READY_TO_PLAY:
            ((BangProvider)provider).readyToPlay(
                source                
            );
            return;

        case BangMarshaller.SURPRISE:
            ((BangProvider)provider).surprise(
                source,
                ((Integer)args[0]).intValue(), ((Short)args[1]).shortValue(), ((Short)args[2]).shortValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
        }
    }
}
