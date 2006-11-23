//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link HideoutService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class HideoutMarshaller extends InvocationMarshaller
    implements HideoutService
{
    /** The method id used to dispatch {@link #addToCoffers} requests. */
    public static final int ADD_TO_COFFERS = 1;

    // from interface HideoutService
    public void addToCoffers (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ADD_TO_COFFERS, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #changeMemberRank} requests. */
    public static final int CHANGE_MEMBER_RANK = 2;

    // from interface HideoutService
    public void changeMemberRank (Client arg1, Handle arg2, byte arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, CHANGE_MEMBER_RANK, new Object[] {
            arg2, Byte.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #expelMember} requests. */
    public static final int EXPEL_MEMBER = 3;

    // from interface HideoutService
    public void expelMember (Client arg1, Handle arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, EXPEL_MEMBER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #formGang} requests. */
    public static final int FORM_GANG = 4;

    // from interface HideoutService
    public void formGang (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, FORM_GANG, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #leaveGang} requests. */
    public static final int LEAVE_GANG = 5;

    // from interface HideoutService
    public void leaveGang (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_GANG, new Object[] {
            listener2
        });
    }
}
