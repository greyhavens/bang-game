//
// $Id$

package com.threerings.bang.gang.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangPeerService;

/**
 * Provides the implementation of the {@link GangPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GangPeerService.java.")
public class GangPeerMarshaller extends InvocationMarshaller<ClientObject>
    implements GangPeerService
{
    /** The method id used to dispatch {@link #addToCoffers} requests. */
    public static final int ADD_TO_COFFERS = 1;

    // from interface GangPeerService
    public void addToCoffers (Handle arg1, String arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(ADD_TO_COFFERS, new Object[] {
            arg1, arg2, Integer.valueOf(arg3), Integer.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #broadcastToMembers} requests. */
    public static final int BROADCAST_TO_MEMBERS = 2;

    // from interface GangPeerService
    public void broadcastToMembers (Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(BROADCAST_TO_MEMBERS, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #buyGangGood} requests. */
    public static final int BUY_GANG_GOOD = 3;

    // from interface GangPeerService
    public void buyGangGood (Handle arg1, String arg2, Object[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(BUY_GANG_GOOD, new Object[] {
            arg1, arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #changeMemberRank} requests. */
    public static final int CHANGE_MEMBER_RANK = 4;

    // from interface GangPeerService
    public void changeMemberRank (Handle arg1, Handle arg2, byte arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(CHANGE_MEMBER_RANK, new Object[] {
            arg1, arg2, Byte.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #changeMemberTitle} requests. */
    public static final int CHANGE_MEMBER_TITLE = 5;

    // from interface GangPeerService
    public void changeMemberTitle (Handle arg1, Handle arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(CHANGE_MEMBER_TITLE, new Object[] {
            arg1, arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #getUpgradeQuote} requests. */
    public static final int GET_UPGRADE_QUOTE = 6;

    // from interface GangPeerService
    public void getUpgradeQuote (Handle arg1, GangGood arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_UPGRADE_QUOTE, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #grantAces} requests. */
    public static final int GRANT_ACES = 7;

    // from interface GangPeerService
    public void grantAces (Handle arg1, int arg2)
    {
        sendRequest(GRANT_ACES, new Object[] {
            arg1, Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #grantScrip} requests. */
    public static final int GRANT_SCRIP = 8;

    // from interface GangPeerService
    public void grantScrip (int arg1)
    {
        sendRequest(GRANT_SCRIP, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #handleInviteResponse} requests. */
    public static final int HANDLE_INVITE_RESPONSE = 9;

    // from interface GangPeerService
    public void handleInviteResponse (Handle arg1, int arg2, Handle arg3, boolean arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(HANDLE_INVITE_RESPONSE, new Object[] {
            arg1, Integer.valueOf(arg2), arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 10;

    // from interface GangPeerService
    public void inviteMember (Handle arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(INVITE_MEMBER, new Object[] {
            arg1, arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #memberEnteredHideout} requests. */
    public static final int MEMBER_ENTERED_HIDEOUT = 11;

    // from interface GangPeerService
    public void memberEnteredHideout (Handle arg1, AvatarInfo arg2)
    {
        sendRequest(MEMBER_ENTERED_HIDEOUT, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #memberLeftHideout} requests. */
    public static final int MEMBER_LEFT_HIDEOUT = 12;

    // from interface GangPeerService
    public void memberLeftHideout (Handle arg1)
    {
        sendRequest(MEMBER_LEFT_HIDEOUT, new Object[] {
            arg1
        });
    }

    /** The method id used to dispatch {@link #processOutfits} requests. */
    public static final int PROCESS_OUTFITS = 13;

    // from interface GangPeerService
    public void processOutfits (Handle arg1, OutfitArticle[] arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
    {
        InvocationMarshaller.ResultMarshaller listener5 = new InvocationMarshaller.ResultMarshaller();
        listener5.listener = arg5;
        sendRequest(PROCESS_OUTFITS, new Object[] {
            arg1, arg2, Boolean.valueOf(arg3), Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #removeFromGang} requests. */
    public static final int REMOVE_FROM_GANG = 14;

    // from interface GangPeerService
    public void removeFromGang (Handle arg1, Handle arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(REMOVE_FROM_GANG, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #renewGangItem} requests. */
    public static final int RENEW_GANG_ITEM = 15;

    // from interface GangPeerService
    public void renewGangItem (Handle arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(RENEW_GANG_ITEM, new Object[] {
            arg1, Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #rentGangGood} requests. */
    public static final int RENT_GANG_GOOD = 16;

    // from interface GangPeerService
    public void rentGangGood (Handle arg1, String arg2, Object[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(RENT_GANG_GOOD, new Object[] {
            arg1, arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #reserveScrip} requests. */
    public static final int RESERVE_SCRIP = 17;

    // from interface GangPeerService
    public void reserveScrip (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(RESERVE_SCRIP, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #sendSpeak} requests. */
    public static final int SEND_SPEAK = 18;

    // from interface GangPeerService
    public void sendSpeak (Handle arg1, String arg2, byte arg3)
    {
        sendRequest(SEND_SPEAK, new Object[] {
            arg1, arg2, Byte.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 19;

    // from interface GangPeerService
    public void setAvatar (int arg1, AvatarInfo arg2)
    {
        sendRequest(SET_AVATAR, new Object[] {
            Integer.valueOf(arg1), arg2
        });
    }

    /** The method id used to dispatch {@link #setBuckle} requests. */
    public static final int SET_BUCKLE = 20;

    // from interface GangPeerService
    public void setBuckle (Handle arg1, BucklePart[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_BUCKLE, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setStatement} requests. */
    public static final int SET_STATEMENT = 21;

    // from interface GangPeerService
    public void setStatement (Handle arg1, String arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(SET_STATEMENT, new Object[] {
            arg1, arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #tradeCompleted} requests. */
    public static final int TRADE_COMPLETED = 22;

    // from interface GangPeerService
    public void tradeCompleted (int arg1, int arg2, String arg3)
    {
        sendRequest(TRADE_COMPLETED, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #updateCoins} requests. */
    public static final int UPDATE_COINS = 23;

    // from interface GangPeerService
    public void updateCoins ()
    {
        sendRequest(UPDATE_COINS, new Object[] {
        });
    }
}
