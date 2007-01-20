//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.saloon.data.Criterion;
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

    /** The method id used to dispatch {@link #buyOutfits} requests. */
    public static final int BUY_OUTFITS = 2;

    // from interface HideoutService
    public void buyOutfits (Client arg1, OutfitArticle[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BUY_OUTFITS, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #changeMemberRank} requests. */
    public static final int CHANGE_MEMBER_RANK = 3;

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
    public static final int EXPEL_MEMBER = 4;

    // from interface HideoutService
    public void expelMember (Client arg1, Handle arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, EXPEL_MEMBER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #findMatch} requests. */
    public static final int FIND_MATCH = 5;

    // from interface HideoutService
    public void findMatch (Client arg1, Criterion arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FIND_MATCH, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #formGang} requests. */
    public static final int FORM_GANG = 6;

    // from interface HideoutService
    public void formGang (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, FORM_GANG, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #getHistoryEntries} requests. */
    public static final int GET_HISTORY_ENTRIES = 7;

    // from interface HideoutService
    public void getHistoryEntries (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_HISTORY_ENTRIES, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getOutfitQuote} requests. */
    public static final int GET_OUTFIT_QUOTE = 8;

    // from interface HideoutService
    public void getOutfitQuote (Client arg1, OutfitArticle[] arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_OUTFIT_QUOTE, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #leaveGang} requests. */
    public static final int LEAVE_GANG = 9;

    // from interface HideoutService
    public void leaveGang (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_GANG, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 10;

    // from interface HideoutService
    public void leaveMatch (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_MATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #setStatement} requests. */
    public static final int SET_STATEMENT = 11;

    // from interface HideoutService
    public void setStatement (Client arg1, String arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_STATEMENT, new Object[] {
            arg2, arg3, listener4
        });
    }
}
