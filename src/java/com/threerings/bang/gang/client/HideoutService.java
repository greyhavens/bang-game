//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameCreator;

import com.threerings.bang.saloon.data.Criterion;

import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;

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
     * Downloads part of the gang's history.
     *
     * @param offset the offset at which to start
     * @param listener a listener to notify with the array of {@link HistoryEntry}s
     */    
    public void getHistoryEntries (Client client, int offset, ResultListener listener);
    
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
     * Purchases gang outfits for all members who don't already own them.
     */
    public void buyOutfits (Client client, OutfitArticle[] outfit, ConfirmListener listener);
}
