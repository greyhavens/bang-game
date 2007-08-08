//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;

import com.threerings.bang.gang.data.OutfitArticle;

/**
 * The interface through which peers make requests on the behalf of users to the peer that
 * currently hosts their gang object.
 */
public interface GangPeerService extends InvocationService
{
    /**
     * Grants aces to a player's gang.
     */
    public void grantAces (Client client, Handle handle, int aces);

    /**
     * Sets the avatar of the gang's senior leader.
     */
    public void setAvatar (Client client, int playerId, AvatarInfo info);

    /**
     * Called when a member enters the Hideout on any server.
     */
    public void memberEnteredHideout (Client client, Handle handle, AvatarInfo info);

    /**
     * Called when a member leaves the Hideout on any server.
     */
    public void memberLeftHideout (Client client, Handle handle);

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
     * Sets the gang's buckle.
     */
    public void setBuckle (
        Client client, Handle handle, BucklePart[] parts, ConfirmListener listener);

    /**
     * Adds to the gang's coffers.
     */
    public void addToCoffers (
        Client client, Handle handle, String coinAccount, int scrip, int coins,
        ConfirmListener listener);

    /**
     * Reserves scrip to be used in an exchange offer.
     */
    public void reserveScrip (Client client, int scrip, ResultListener listener);

    /**
     * Grants scrip from a failed exchange offer.
     */
    public void grantScrip (Client client, int scrip);

    /**
     * Tells a gang to update it's coin count.
     */
    public void updateCoins (Client client);

    /**
     * Logs a completed exchange transaction in the gang history.
     */
    public void tradeCompleted (Client client, int price, int vol, String member);

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
     * Changes a member's title.
     */
    public void changeMemberTitle (
        Client client, Handle handle, Handle target, int title, ConfirmListener listener);

    /**
     * Gets a quote for or buys the specified gang articles.
     */
    public void processOutfits (
        Client client, Handle handle, OutfitArticle[] outfit, boolean buy,
        boolean admin, ResultListener listener);

    /**
     * Purchases a good for the gang.
     */
    public void buyGangGood (
        Client client, Handle handle, String type, Object[] args, boolean admin,
        ConfirmListener listener);

    /**
     * Purchases a good for the gang.
     */
    public void rentGangGood (
        Client client, Handle handle, String type, Object[] args, boolean admin,
        ConfirmListener listener);

    /**
     * Renews a ranted gang item.
     */
    public void renewGangItem (
        Client client, Handle handle, int itemId, ConfirmListener listener);

    /**
     * Broadcasts a message to all online members.
     */
    public void broadcastToMembers (
        Client client, Handle handle, String message, ConfirmListener listener);
}
