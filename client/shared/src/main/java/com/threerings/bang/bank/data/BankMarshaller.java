//
// $Id$

package com.threerings.bang.bank.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.bank.client.BankService;
import com.threerings.bang.data.PlayerObject;
import com.threerings.coin.data.CoinExOfferInfo;

/**
 * Provides the implementation of the {@link BankService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BankService.java.")
public class BankMarshaller extends InvocationMarshaller<PlayerObject>
    implements BankService
{
    /**
     * Marshalls results to implementations of {@code BankService.OfferListener}.
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
            sendResponse(GOT_OFFERS, new Object[] { arg1, arg2 });
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
    public void cancelOffer (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(CANCEL_OFFER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #getMyOffers} requests. */
    public static final int GET_MY_OFFERS = 2;

    // from interface BankService
    public void getMyOffers (BankService.OfferListener arg1)
    {
        BankMarshaller.OfferMarshaller listener1 = new BankMarshaller.OfferMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_MY_OFFERS, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #postOffer} requests. */
    public static final int POST_OFFER = 3;

    // from interface BankService
    public void postOffer (int arg1, int arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
    {
        InvocationMarshaller.ResultMarshaller listener5 = new InvocationMarshaller.ResultMarshaller();
        listener5.listener = arg5;
        sendRequest(POST_OFFER, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), Boolean.valueOf(arg3), Boolean.valueOf(arg4), listener5
        });
    }
}
