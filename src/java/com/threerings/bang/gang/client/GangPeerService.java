//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;

import com.threerings.bang.gang.data.OutfitArticle;

/**
 * The interface through which peers make requests on the behalf of users to the peer that
 * currently hosts their gang object.
 */
public interface GangPeerService extends InvocationService
{
    /**
     * Grants notoriety points to a player and his gang.
     */
    public void grantNotoriety (Client client, Handle handle, int points);

    /**
     * Sets the avatar of the gang's senior leader.
     */
    public void setAvatar (Client client, int playerId, AvatarInfo info);

    /**
     * Invites a user into the gang.
     */
    public void inviteMember (
        Client client, Handle handle, Handle target, String message, ConfirmListener listener);

    /**
     * Handles the response to an invitation to join.
     */
    public void handleInviteResponse (
        Client client, Handle handle, int playerId, Handle inviter, boolean accept,
        ConfirmListener listener);

    /**
     * Handles a remote member's request to speak on the gang object.
     */
    public void sendSpeak (Client client, Handle handle, String message, byte mode);

    /**
     * Sets the gang's statement and URL.
     */
    public void setStatement (
        Client client, Handle handle, String statement, String url, ConfirmListener listener);

    /**
     * Adds to the gang's coffers.
     */
    public void addToCoffers (
        Client client, Handle handle, String coinAccount, int scrip, int coins,
        ConfirmListener listener);

    /**
     * Removes a player from the gang.
     */
    public void removeFromGang (
        Client client, Handle handle, Handle target, ConfirmListener listener);

    /**
     * Changes a member's rank.
     */
    public void changeMemberRank (
        Client client, Handle handle, Handle target, byte rank, ConfirmListener listener);

    /**
     * Gets a quote for or buys the specified gang articles.
     */
    public void processOutfits (
        Client client, Handle handle, OutfitArticle[] outfit, boolean buy,
        boolean admin, ResultListener listener);
}
