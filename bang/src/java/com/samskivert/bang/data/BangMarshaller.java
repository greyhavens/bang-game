//
// $Id$

package com.samskivert.bang.data;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.piece.Piece;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangMarshaller extends InvocationMarshaller
    implements BangService
{
    /** The method id used to dispatch {@link #move} requests. */
    public static final int MOVE = 1;

    // documentation inherited from interface
    public void move (Client arg1, int arg2, short arg3, short arg4, int arg5, InvocationService.InvocationListener arg6)
    {
        ListenerMarshaller listener6 = new ListenerMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, MOVE, new Object[] {
            new Integer(arg2), new Short(arg3), new Short(arg4), new Integer(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #purchasePiece} requests. */
    public static final int PURCHASE_PIECE = 2;

    // documentation inherited from interface
    public void purchasePiece (Client arg1, Piece arg2)
    {
        sendRequest(arg1, PURCHASE_PIECE, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #readyToPlay} requests. */
    public static final int READY_TO_PLAY = 3;

    // documentation inherited from interface
    public void readyToPlay (Client arg1)
    {
        sendRequest(arg1, READY_TO_PLAY, new Object[] {
            
        });
    }

    /** The method id used to dispatch {@link #surprise} requests. */
    public static final int SURPRISE = 4;

    // documentation inherited from interface
    public void surprise (Client arg1, int arg2, short arg3, short arg4)
    {
        sendRequest(arg1, SURPRISE, new Object[] {
            new Integer(arg2), new Short(arg3), new Short(arg4)
        });
    }

}
