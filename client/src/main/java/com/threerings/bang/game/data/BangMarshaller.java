//
// $Id$

package com.threerings.bang.game.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.client.BangService;

/**
 * Provides the implementation of the {@link BangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangService.java.")
public class BangMarshaller extends InvocationMarshaller<PlayerObject>
    implements BangService
{
    /**
     * Marshalls results to implementations of {@code BangService.BoardListener}.
     */
    public static class BoardMarshaller extends ListenerMarshaller
        implements BoardListener
    {
        /** The method id used to dispatch {@link #requestProcessed}
         * responses. */
        public static final int REQUEST_PROCESSED = 1;

        // from interface BoardMarshaller
        public void requestProcessed (BoardData arg1)
        {
            sendResponse(REQUEST_PROCESSED, new Object[] { arg1 });
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case REQUEST_PROCESSED:
                ((BoardListener)listener).requestProcessed(
                    (BoardData)args[0]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #cancelOrder} requests. */
    public static final int CANCEL_ORDER = 1;

    // from interface BangService
    public void cancelOrder (int arg1)
    {
        sendRequest(CANCEL_ORDER, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #getBoard} requests. */
    public static final int GET_BOARD = 2;

    // from interface BangService
    public void getBoard (BangService.BoardListener arg1)
    {
        BangMarshaller.BoardMarshaller listener1 = new BangMarshaller.BoardMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_BOARD, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #order} requests. */
    public static final int ORDER = 3;

    // from interface BangService
    public void order (int arg1, short arg2, short arg3, int arg4, InvocationService.ResultListener arg5)
    {
        InvocationMarshaller.ResultMarshaller listener5 = new InvocationMarshaller.ResultMarshaller();
        listener5.listener = arg5;
        sendRequest(ORDER, new Object[] {
            Integer.valueOf(arg1), Short.valueOf(arg2), Short.valueOf(arg3), Integer.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #playCard} requests. */
    public static final int PLAY_CARD = 4;

    // from interface BangService
    public void playCard (int arg1, Object arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(PLAY_CARD, new Object[] {
            Integer.valueOf(arg1), arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #reportPerformance} requests. */
    public static final int REPORT_PERFORMANCE = 5;

    // from interface BangService
    public void reportPerformance (String arg1, String arg2, int[] arg3)
    {
        sendRequest(REPORT_PERFORMANCE, new Object[] {
            arg1, arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #selectTeam} requests. */
    public static final int SELECT_TEAM = 6;

    // from interface BangService
    public void selectTeam (int arg1, String[] arg2, int[] arg3)
    {
        sendRequest(SELECT_TEAM, new Object[] {
            Integer.valueOf(arg1), arg2, arg3
        });
    }
}
