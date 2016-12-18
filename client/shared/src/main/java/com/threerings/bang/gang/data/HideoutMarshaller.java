//
// $Id$

package com.threerings.bang.gang.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.saloon.data.Criterion;

/**
 * Provides the implementation of the {@link HideoutService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from HideoutService.java.")
public class HideoutMarshaller extends InvocationMarshaller<PlayerObject>
    implements HideoutService
{
    /** The method id used to dispatch {@link #addToCoffers} requests. */
    public static final int ADD_TO_COFFERS = 1;

    // from interface HideoutService
    public void addToCoffers (int arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(ADD_TO_COFFERS, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #broadcastToMembers} requests. */
    public static final int BROADCAST_TO_MEMBERS = 2;

    // from interface HideoutService
    public void broadcastToMembers (String arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(BROADCAST_TO_MEMBERS, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #buyGangGood} requests. */
    public static final int BUY_GANG_GOOD = 3;

    // from interface HideoutService
    public void buyGangGood (String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(BUY_GANG_GOOD, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #buyOutfits} requests. */
    public static final int BUY_OUTFITS = 4;

    // from interface HideoutService
    public void buyOutfits (OutfitArticle[] arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(BUY_OUTFITS, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #changeMemberRank} requests. */
    public static final int CHANGE_MEMBER_RANK = 5;

    // from interface HideoutService
    public void changeMemberRank (Handle arg1, byte arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(CHANGE_MEMBER_RANK, new Object[] {
            arg1, Byte.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #changeMemberTitle} requests. */
    public static final int CHANGE_MEMBER_TITLE = 6;

    // from interface HideoutService
    public void changeMemberTitle (Handle arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(CHANGE_MEMBER_TITLE, new Object[] {
            arg1, Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #expelMember} requests. */
    public static final int EXPEL_MEMBER = 7;

    // from interface HideoutService
    public void expelMember (Handle arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(EXPEL_MEMBER, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #findMatch} requests. */
    public static final int FIND_MATCH = 8;

    // from interface HideoutService
    public void findMatch (Criterion arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(FIND_MATCH, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #formGang} requests. */
    public static final int FORM_GANG = 9;

    // from interface HideoutService
    public void formGang (Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(FORM_GANG, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getHistoryEntries} requests. */
    public static final int GET_HISTORY_ENTRIES = 10;

    // from interface HideoutService
    public void getHistoryEntries (int arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_HISTORY_ENTRIES, new Object[] {
            Integer.valueOf(arg1), arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getOutfitQuote} requests. */
    public static final int GET_OUTFIT_QUOTE = 11;

    // from interface HideoutService
    public void getOutfitQuote (OutfitArticle[] arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_OUTFIT_QUOTE, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #getUpgradeQuote} requests. */
    public static final int GET_UPGRADE_QUOTE = 12;

    // from interface HideoutService
    public void getUpgradeQuote (GangGood arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_UPGRADE_QUOTE, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #leaveGang} requests. */
    public static final int LEAVE_GANG = 13;

    // from interface HideoutService
    public void leaveGang (InvocationService.ConfirmListener arg1)
    {
        InvocationMarshaller.ConfirmMarshaller listener1 = new InvocationMarshaller.ConfirmMarshaller();
        listener1.listener = arg1;
        sendRequest(LEAVE_GANG, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 14;

    // from interface HideoutService
    public void leaveMatch (int arg1)
    {
        sendRequest(LEAVE_MATCH, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #renewGangItem} requests. */
    public static final int RENEW_GANG_ITEM = 15;

    // from interface HideoutService
    public void renewGangItem (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(RENEW_GANG_ITEM, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #rentGangGood} requests. */
    public static final int RENT_GANG_GOOD = 16;

    // from interface HideoutService
    public void rentGangGood (String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(RENT_GANG_GOOD, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setBuckle} requests. */
    public static final int SET_BUCKLE = 17;

    // from interface HideoutService
    public void setBuckle (BucklePart[] arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_BUCKLE, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #setStatement} requests. */
    public static final int SET_STATEMENT = 18;

    // from interface HideoutService
    public void setStatement (String arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_STATEMENT, new Object[] {
            arg1, arg2, listener3
        });
    }
}
