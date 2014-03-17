//
// $Id$

package com.threerings.bang.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.client.BangPeerService;

/**
 * Provides the implementation of the {@link BangPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangPeerService.java.")
public class BangPeerMarshaller extends InvocationMarshaller<ClientObject>
    implements BangPeerService
{
    /** The method id used to dispatch {@link #deliverGangInvite} requests. */
    public static final int DELIVER_GANG_INVITE = 1;

    // from interface BangPeerService
    public void deliverGangInvite (Handle arg1, Handle arg2, int arg3, Handle arg4, String arg5)
    {
        sendRequest(DELIVER_GANG_INVITE, new Object[] {
            arg1, arg2, Integer.valueOf(arg3), arg4, arg5
        });
    }

    /** The method id used to dispatch {@link #deliverItem} requests. */
    public static final int DELIVER_ITEM = 2;

    // from interface BangPeerService
    public void deliverItem (Item arg1, String arg2)
    {
        sendRequest(DELIVER_ITEM, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerInvite} requests. */
    public static final int DELIVER_PARDNER_INVITE = 3;

    // from interface BangPeerService
    public void deliverPardnerInvite (Handle arg1, Handle arg2, String arg3)
    {
        sendRequest(DELIVER_PARDNER_INVITE, new Object[] {
            arg1, arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerInviteResponse} requests. */
    public static final int DELIVER_PARDNER_INVITE_RESPONSE = 4;

    // from interface BangPeerService
    public void deliverPardnerInviteResponse (Handle arg1, Handle arg2, boolean arg3, boolean arg4)
    {
        sendRequest(DELIVER_PARDNER_INVITE_RESPONSE, new Object[] {
            arg1, arg2, Boolean.valueOf(arg3), Boolean.valueOf(arg4)
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerRemoval} requests. */
    public static final int DELIVER_PARDNER_REMOVAL = 5;

    // from interface BangPeerService
    public void deliverPardnerRemoval (Handle arg1, Handle arg2)
    {
        sendRequest(DELIVER_PARDNER_REMOVAL, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #getGangOid} requests. */
    public static final int GET_GANG_OID = 6;

    // from interface BangPeerService
    public void getGangOid (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_GANG_OID, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
