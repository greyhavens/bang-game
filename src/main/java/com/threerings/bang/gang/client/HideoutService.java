//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameCreator;

import com.threerings.bang.saloon.data.Criterion;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.coin.data.CoinExOfferInfo;

/**
 * Provides hideout-related functionality.
 */
public interface HideoutService extends InvocationService
{
    /**
     * Requests to form a new gang.
     *
     * @param root the root of the name (the part that comes after "The" and
     * before the suffix)
     * @param suffix the name suffix, which must be one of the approved gang
     * suffixes from the {@link NameCreator}
     */
    public void formGang (
        Client client, Handle root, String suffix, ConfirmListener listener);

    /**
     * Requests to leave the current gang.
     */
    public void leaveGang (Client client, ConfirmListener listener);

    /**
     * Requests to set the gang's statement and URL.
     */
    public void setStatement (
        Client client, String statement, String url, ConfirmListener listener);

    /**
     * Requests to reconfigure the gang's buckle.
     *
     * @param parts the parts to use in the buckle, in order, with any necessary
     * state changes
     */
    public void setBuckle (
        Client client, BucklePart[] parts, ConfirmListener listener);

    /**
     * Requests to contribute scrip and/or coins to the gang's coffers.
     */
    public void addToCoffers (
        Client client, int scrip, int coins, ConfirmListener listener);

    /**
     * Requests to expel a member from the gang.
     */
    public void expelMember (
        Client client, Handle handle, ConfirmListener listener);

    /**
     * Requests to promote or demote a gang member.
     */
    public void changeMemberRank (
        Client client, Handle handle, byte rank, ConfirmListener listener);

    /**
     * Requests to change the title of a gang member.
     */
    public void changeMemberTitle (
        Client client, Handle handle, int title, ConfirmListener listener);

    /**
     * Downloads part of the gang's history.
     *
     * @param offset the offset at which to start
     * @param listener a listener to notify with the array of {@link HistoryEntry}s
     */
    public void getHistoryEntries (
            Client client, int offset, String filter, ResultListener listener);

    /**
     * Requests that a game be located meeting the specified criterion.
     */
    public void findMatch (Client client, Criterion criterion, ResultListener listener);

    /**
     * Requests that we leave our currently pending match.
     */
    public void leaveMatch (Client client, int matchOid);

    /**
     * Requests a price quote for the specified gang outfit.  The listener will receive an integer
     * array containing the scrip and coin cost to buy the specified articles for every member who
     * doesn't already own them.
     */
    public void getOutfitQuote (Client client, OutfitArticle[] outfit, ResultListener listener);

    /**
     * Purchases gang outfits for all members who don't already own them.  The listener will
     * receive an integer array containing the number of members who received articles and the
     * total number of articles purchased.
     */
    public void buyOutfits (Client client, OutfitArticle[] outfit, ResultListener listener);

    /**
     * Purchases a gang good with the specified arguments.
     */
    public void buyGangGood (Client client, String type, Object[] args, ConfirmListener listener);

    /**
     * Rents a good for all gang members with the specified arguments.
     */
    public void rentGangGood (Client client, String type, Object[] args, ConfirmListener listener);

    /**
     * Renews a gang rented item.
     */
    public void renewGangItem (Client client, int itemId, ConfirmListener listener);

    /**
     * Broadcast a message to all online members of the player's gang.
     */
    public void broadcastToMembers (Client client, String message, ConfirmListener listener);

    /**
     * Requests that the specified offer be posted to the market.
     *
     * @param rl a result listener that will be notified with a {@link
     * CoinExOfferInfo} for a posted offer or null for an immediately executed
     * transaction.
     */
    public void postOffer (Client client, int coins, int pricePerCoin, ResultListener rl);

    /**
     * Requests a price quote for the specified gang upgrade.  The listener will receive an integer
     * array containing the scip and coin cost to buy the gang upgrade.
     */
    public void getUpgradeQuote (Client client, GangGood good, ResultListener listener);
}
