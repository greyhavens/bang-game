//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.OutfitArticle;

/**
 * The interface through which peers make requests on the behalf of users to the peer that
 * currently hosts their gang object.
 */
public interface GangPeerService extends InvocationService<ClientObject>
{
    /**
     * Grants aces to a player's gang.
     */
    public void grantAces (Handle handle, int aces);

    /**
     * Sets the avatar of the gang's senior leader.
     */
    public void setAvatar (int playerId, AvatarInfo info);

    /**
     * Called when a member enters the Hideout on any server.
     */
    public void memberEnteredHideout (Handle handle, AvatarInfo info);

    /**
     * Called when a member leaves the Hideout on any server.
     */
    public void memberLeftHideout (Handle handle);

    /**
     * Invites a user into the gang.
     */
    public void inviteMember (Handle handle, Handle target, String message,
                              ConfirmListener listener);

    /**
     * Handles the response to an invitation to join.
     */
    public void handleInviteResponse (Handle handle, int playerId, Handle inviter, boolean accept,
                                      ConfirmListener listener);

    /**
     * Handles a remote member's request to speak on the gang object.
     */
    public void sendSpeak (Handle handle, String message, byte mode);

    /**
     * Sets the gang's statement and URL.
     */
    public void setStatement (Handle handle, String statement, String url, ConfirmListener listener);

    /**
     * Sets the gang's buckle.
     */
    public void setBuckle (Handle handle, BucklePart[] parts, ConfirmListener listener);

    /**
     * Adds to the gang's coffers.
     */
    public void addToCoffers (Handle handle, String coinAccount, int scrip, int coins,
                              ConfirmListener listener);

    /**
     * Reserves scrip to be used in an exchange offer.
     */
    public void reserveScrip (int scrip, ConfirmListener listener);

    /**
     * Grants scrip from a failed exchange offer.
     */
    public void grantScrip (int scrip);

    /**
     * Tells a gang to update it's coin count.
     */
    public void updateCoins ();

    /**
     * Logs a completed exchange transaction in the gang history.
     */
    public void tradeCompleted (int price, int vol, String member);

    /**
     * Removes a player from the gang.
     */
    public void removeFromGang (Handle handle, Handle target, ConfirmListener listener);

    /**
     * Changes a member's rank.
     */
    public void changeMemberRank (Handle handle, Handle target, byte rank, ConfirmListener listener);

    /**
     * Changes a member's title.
     */
    public void changeMemberTitle (Handle handle, Handle target, int title,
                                   ConfirmListener listener);

    /**
     * Gets a quote for or buys the specified gang articles.
     */
    public void processOutfits (Handle handle, OutfitArticle[] outfit, boolean buy, boolean admin,
                                ResultListener listener);

    /**
     * Purchases a good for the gang.
     */
    public void buyGangGood (Handle handle, String type, Object[] args, boolean admin,
                             ConfirmListener listener);

    /**
     * Purchases a good for the gang.
     */
    public void rentGangGood (Handle handle, String type, Object[] args, boolean admin,
                              ConfirmListener listener);

    /**
     * Renews a ranted gang item.
     */
    public void renewGangItem (Handle handle, int itemId, ConfirmListener listener);

    /**
     * Broadcasts a message to all online members.
     */
    public void broadcastToMembers (Handle handle, String message, ConfirmListener listener);

    /**
     * Gets a quote for a gang upgrade.
     */
    public void getUpgradeQuote (Handle handle, GangGood good, ResultListener listener);
}
