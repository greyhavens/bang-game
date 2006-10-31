//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.bang.bank.client.BankService;
import com.threerings.coin.data.CoinExOfferInfo;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BankService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BankMarshaller extends InvocationMarshaller
    implements BankService
{
    /**
     * Marshalls results to implementations of {@link OfferListener}.
     */
    public static class OfferMarshaller extends ListenerMarshaller
        implements OfferListener
    {
        /** The method id used to dispatch {@link #gotOffers}
         * responses. */
        public static final int GOT_OFFERS = 1;

        // from interface OfferMarshaller
        public void gotOffers (CoinExOfferInfo[] arg1, CoinExOfferInfo[] arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, GOT_OFFERS,
                               new Object[] { arg1, arg2 }));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case GOT_OFFERS:
                ((OfferListener)listener).gotOffers(
                    (CoinExOfferInfo[])args[0], (CoinExOfferInfo[])args[1]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #cancelOffer} requests. */
    public static final int CANCEL_OFFER = 1;

    // from interface BankService
    public void cancelOffer (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CANCEL_OFFER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getMyOffers} requests. */
    public static final int GET_MY_OFFERS = 2;

    // from interface BankService
    public void getMyOffers (Client arg1, BankService.OfferListener arg2)
    {
        BankMarshaller.OfferMarshaller listener2 = new BankMarshaller.OfferMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_MY_OFFERS, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #postOffer} requests. */
    public static final int POST_OFFER = 3;

    // from interface BankService
    public void postOffer (Client arg1, int arg2, int arg3, boolean arg4, boolean arg5, InvocationService.ResultListener arg6)
    {
        InvocationMarshaller.ResultMarshaller listener6 = new InvocationMarshaller.ResultMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, POST_OFFER, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Boolean.valueOf(arg4), Boolean.valueOf(arg5), listener6
        });
    }
}
