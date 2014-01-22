//
// $Id$

package com.threerings.bang.gang.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangPeerService;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.OutfitArticle;

/**
 * Defines the server-side of the {@link GangPeerService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GangPeerService.java.")
public interface GangPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link GangPeerService#addToCoffers} request.
     */
    void addToCoffers (ClientObject caller, Handle arg1, String arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#broadcastToMembers} request.
     */
    void broadcastToMembers (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#buyGangGood} request.
     */
    void buyGangGood (ClientObject caller, Handle arg1, String arg2, Object[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#changeMemberRank} request.
     */
    void changeMemberRank (ClientObject caller, Handle arg1, Handle arg2, byte arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#changeMemberTitle} request.
     */
    void changeMemberTitle (ClientObject caller, Handle arg1, Handle arg2, int arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#getUpgradeQuote} request.
     */
    void getUpgradeQuote (ClientObject caller, Handle arg1, GangGood arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#grantAces} request.
     */
    void grantAces (ClientObject caller, Handle arg1, int arg2);

    /**
     * Handles a {@link GangPeerService#grantScrip} request.
     */
    void grantScrip (ClientObject caller, int arg1);

    /**
     * Handles a {@link GangPeerService#handleInviteResponse} request.
     */
    void handleInviteResponse (ClientObject caller, Handle arg1, int arg2, Handle arg3, boolean arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#inviteMember} request.
     */
    void inviteMember (ClientObject caller, Handle arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#memberEnteredHideout} request.
     */
    void memberEnteredHideout (ClientObject caller, Handle arg1, AvatarInfo arg2);

    /**
     * Handles a {@link GangPeerService#memberLeftHideout} request.
     */
    void memberLeftHideout (ClientObject caller, Handle arg1);

    /**
     * Handles a {@link GangPeerService#processOutfits} request.
     */
    void processOutfits (ClientObject caller, Handle arg1, OutfitArticle[] arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#removeFromGang} request.
     */
    void removeFromGang (ClientObject caller, Handle arg1, Handle arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#renewGangItem} request.
     */
    void renewGangItem (ClientObject caller, Handle arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#rentGangGood} request.
     */
    void rentGangGood (ClientObject caller, Handle arg1, String arg2, Object[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#reserveScrip} request.
     */
    void reserveScrip (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#sendSpeak} request.
     */
    void sendSpeak (ClientObject caller, Handle arg1, String arg2, byte arg3);

    /**
     * Handles a {@link GangPeerService#setAvatar} request.
     */
    void setAvatar (ClientObject caller, int arg1, AvatarInfo arg2);

    /**
     * Handles a {@link GangPeerService#setBuckle} request.
     */
    void setBuckle (ClientObject caller, Handle arg1, BucklePart[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#setStatement} request.
     */
    void setStatement (ClientObject caller, Handle arg1, String arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#tradeCompleted} request.
     */
    void tradeCompleted (ClientObject caller, int arg1, int arg2, String arg3);

    /**
     * Handles a {@link GangPeerService#updateCoins} request.
     */
    void updateCoins (ClientObject caller);
}
