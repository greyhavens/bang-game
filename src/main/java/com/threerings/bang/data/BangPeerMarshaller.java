//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.BangPeerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link BangPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BangPeerMarshaller extends InvocationMarshaller
    implements BangPeerService
{
    /** The method id used to dispatch {@link #deliverGangInvite} requests. */
    public static final int DELIVER_GANG_INVITE = 1;

    // from interface BangPeerService
    public void deliverGangInvite (Client arg1, Handle arg2, Handle arg3, int arg4, Handle arg5, String arg6)
    {
        sendRequest(arg1, DELIVER_GANG_INVITE, new Object[] {
            arg2, arg3, Integer.valueOf(arg4), arg5, arg6
        });
    }

    /** The method id used to dispatch {@link #deliverItem} requests. */
    public static final int DELIVER_ITEM = 2;

    // from interface BangPeerService
    public void deliverItem (Client arg1, Item arg2, String arg3)
    {
        sendRequest(arg1, DELIVER_ITEM, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerInvite} requests. */
    public static final int DELIVER_PARDNER_INVITE = 3;

    // from interface BangPeerService
    public void deliverPardnerInvite (Client arg1, Handle arg2, Handle arg3, String arg4)
    {
        sendRequest(arg1, DELIVER_PARDNER_INVITE, new Object[] {
            arg2, arg3, arg4
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerInviteResponse} requests. */
    public static final int DELIVER_PARDNER_INVITE_RESPONSE = 4;

    // from interface BangPeerService
    public void deliverPardnerInviteResponse (Client arg1, Handle arg2, Handle arg3, boolean arg4, boolean arg5)
    {
        sendRequest(arg1, DELIVER_PARDNER_INVITE_RESPONSE, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), Boolean.valueOf(arg5)
        });
    }

    /** The method id used to dispatch {@link #deliverPardnerRemoval} requests. */
    public static final int DELIVER_PARDNER_REMOVAL = 5;

    // from interface BangPeerService
    public void deliverPardnerRemoval (Client arg1, Handle arg2, Handle arg3)
    {
        sendRequest(arg1, DELIVER_PARDNER_REMOVAL, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #getGangOid} requests. */
    public static final int GET_GANG_OID = 6;

    // from interface BangPeerService
    public void getGangOid (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GANG_OID, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
