//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangPeerService;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link GangPeerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GangPeerMarshaller extends InvocationMarshaller
    implements GangPeerService
{
    /** The method id used to dispatch {@link #addToCoffers} requests. */
    public static final int ADD_TO_COFFERS = 1;

    // from interface GangPeerService
    public void addToCoffers (Client arg1, Handle arg2, String arg3, int arg4, int arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, ADD_TO_COFFERS, new Object[] {
            arg2, arg3, Integer.valueOf(arg4), Integer.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #broadcastToMembers} requests. */
    public static final int BROADCAST_TO_MEMBERS = 2;

    // from interface GangPeerService
    public void broadcastToMembers (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, BROADCAST_TO_MEMBERS, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #buyGangGood} requests. */
    public static final int BUY_GANG_GOOD = 3;

    // from interface GangPeerService
    public void buyGangGood (Client arg1, Handle arg2, String arg3, Object[] arg4, boolean arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, BUY_GANG_GOOD, new Object[] {
            arg2, arg3, arg4, Boolean.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #changeMemberRank} requests. */
    public static final int CHANGE_MEMBER_RANK = 4;

    // from interface GangPeerService
    public void changeMemberRank (Client arg1, Handle arg2, Handle arg3, byte arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, CHANGE_MEMBER_RANK, new Object[] {
            arg2, arg3, Byte.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #grantAces} requests. */
    public static final int GRANT_ACES = 5;

    // from interface GangPeerService
    public void grantAces (Client arg1, Handle arg2, int arg3)
    {
        sendRequest(arg1, GRANT_ACES, new Object[] {
            arg2, Integer.valueOf(arg3)
        });
    }

    /** The method id used to dispatch {@link #handleInviteResponse} requests. */
    public static final int HANDLE_INVITE_RESPONSE = 6;

    // from interface GangPeerService
    public void handleInviteResponse (Client arg1, Handle arg2, int arg3, Handle arg4, boolean arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, HANDLE_INVITE_RESPONSE, new Object[] {
            arg2, Integer.valueOf(arg3), arg4, Boolean.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 7;

    // from interface GangPeerService
    public void inviteMember (Client arg1, Handle arg2, Handle arg3, String arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, INVITE_MEMBER, new Object[] {
            arg2, arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #memberEnteredHideout} requests. */
    public static final int MEMBER_ENTERED_HIDEOUT = 8;

    // from interface GangPeerService
    public void memberEnteredHideout (Client arg1, Handle arg2, AvatarInfo arg3)
    {
        sendRequest(arg1, MEMBER_ENTERED_HIDEOUT, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #memberLeftHideout} requests. */
    public static final int MEMBER_LEFT_HIDEOUT = 9;

    // from interface GangPeerService
    public void memberLeftHideout (Client arg1, Handle arg2)
    {
        sendRequest(arg1, MEMBER_LEFT_HIDEOUT, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #processOutfits} requests. */
    public static final int PROCESS_OUTFITS = 10;

    // from interface GangPeerService
    public void processOutfits (Client arg1, Handle arg2, OutfitArticle[] arg3, boolean arg4, boolean arg5, InvocationService.ResultListener arg6)
    {
        InvocationMarshaller.ResultMarshaller listener6 = new InvocationMarshaller.ResultMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, PROCESS_OUTFITS, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), Boolean.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #removeFromGang} requests. */
    public static final int REMOVE_FROM_GANG = 11;

    // from interface GangPeerService
    public void removeFromGang (Client arg1, Handle arg2, Handle arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, REMOVE_FROM_GANG, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #sendSpeak} requests. */
    public static final int SEND_SPEAK = 12;

    // from interface GangPeerService
    public void sendSpeak (Client arg1, Handle arg2, String arg3, byte arg4)
    {
        sendRequest(arg1, SEND_SPEAK, new Object[] {
            arg2, arg3, Byte.valueOf(arg4)
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 13;

    // from interface GangPeerService
    public void setAvatar (Client arg1, int arg2, AvatarInfo arg3)
    {
        sendRequest(arg1, SET_AVATAR, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #setBuckle} requests. */
    public static final int SET_BUCKLE = 14;

    // from interface GangPeerService
    public void setBuckle (Client arg1, Handle arg2, BucklePart[] arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_BUCKLE, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #setStatement} requests. */
    public static final int SET_STATEMENT = 15;

    // from interface GangPeerService
    public void setStatement (Client arg1, Handle arg2, String arg3, String arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_STATEMENT, new Object[] {
            arg2, arg3, arg4, listener5
        });
    }
}
