//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangPeerService;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link GangPeerService}.
 */
public interface GangPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link GangPeerService#addToCoffers} request.
     */
    public void addToCoffers (ClientObject caller, Handle arg1, String arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#buyGangGood} request.
     */
    public void buyGangGood (ClientObject caller, Handle arg1, String arg2, Object[] arg3, boolean arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#changeMemberRank} request.
     */
    public void changeMemberRank (ClientObject caller, Handle arg1, Handle arg2, byte arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#grantAces} request.
     */
    public void grantAces (ClientObject caller, Handle arg1, int arg2);

    /**
     * Handles a {@link GangPeerService#handleInviteResponse} request.
     */
    public void handleInviteResponse (ClientObject caller, Handle arg1, int arg2, Handle arg3, boolean arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#inviteMember} request.
     */
    public void inviteMember (ClientObject caller, Handle arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#processOutfits} request.
     */
    public void processOutfits (ClientObject caller, Handle arg1, OutfitArticle[] arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#removeFromGang} request.
     */
    public void removeFromGang (ClientObject caller, Handle arg1, Handle arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#sendSpeak} request.
     */
    public void sendSpeak (ClientObject caller, Handle arg1, String arg2, byte arg3);

    /**
     * Handles a {@link GangPeerService#setAvatar} request.
     */
    public void setAvatar (ClientObject caller, int arg1, AvatarInfo arg2);

    /**
     * Handles a {@link GangPeerService#setBuckle} request.
     */
    public void setBuckle (ClientObject caller, Handle arg1, BucklePart[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link GangPeerService#setStatement} request.
     */
    public void setStatement (ClientObject caller, Handle arg1, String arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
