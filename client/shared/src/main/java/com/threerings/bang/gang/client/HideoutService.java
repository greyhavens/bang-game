//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.NameCreator;

import com.threerings.bang.saloon.data.Criterion;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;

/**
 * Provides hideout-related functionality.
 */
public interface HideoutService extends InvocationService<PlayerObject>
{
    /**
     * Requests to form a new gang.
     *
     * @param root the root of the name (the part that comes after "The" and
     * before the suffix)
     * @param suffix the name suffix, which must be one of the approved gang
     * suffixes from the {@link NameCreator}
     */
    public void formGang (Handle root, String suffix, ConfirmListener listener);

    /**
     * Requests to leave the current gang.
     */
    public void leaveGang (ConfirmListener listener);

    /**
     * Requests to set the gang's statement and URL.
     */
    public void setStatement (String statement, String url, ConfirmListener listener);

    /**
     * Requests to reconfigure the gang's buckle.
     *
     * @param parts the parts to use in the buckle, in order, with any necessary
     * state changes
     */
    public void setBuckle (BucklePart[] parts, ConfirmListener listener);

    /**
     * Requests to contribute scrip and/or coins to the gang's coffers.
     */
    public void addToCoffers (int scrip, int coins, ConfirmListener listener);

    /**
     * Requests to expel a member from the gang.
     */
    public void expelMember (Handle handle, ConfirmListener listener);

    /**
     * Requests to promote or demote a gang member.
     */
    public void changeMemberRank (Handle handle, byte rank, ConfirmListener listener);

    /**
     * Requests to change the title of a gang member.
     */
    public void changeMemberTitle (Handle handle, int title, ConfirmListener listener);

    /**
     * Downloads part of the gang's history.
     *
     * @param offset the offset at which to start
     * @param listener a listener to notify with the array of {@link HistoryEntry}s
     */
    public void getHistoryEntries (int offset, String filter, ResultListener listener);

    /**
     * Requests that a game be located meeting the specified criterion.
     */
    public void findMatch (Criterion criterion, ResultListener listener);

    /**
     * Requests that we leave our currently pending match.
     */
    public void leaveMatch (int matchOid);

    /**
     * Requests a price quote for the specified gang outfit.  The listener will receive an integer
     * array containing the scrip and coin cost to buy the specified articles for every member who
     * doesn't already own them.
     */
    public void getOutfitQuote (OutfitArticle[] outfit, ResultListener listener);

    /**
     * Purchases gang outfits for all members who don't already own them.  The listener will
     * receive an integer array containing the number of members who received articles and the
     * total number of articles purchased.
     */
    public void buyOutfits (OutfitArticle[] outfit, ResultListener listener);

    /**
     * Purchases a gang good with the specified arguments.
     */
    public void buyGangGood (String type, Object[] args, ConfirmListener listener);

    /**
     * Rents a good for all gang members with the specified arguments.
     */
    public void rentGangGood (String type, Object[] args, ConfirmListener listener);

    /**
     * Renews a gang rented item.
     */
    public void renewGangItem (int itemId, ConfirmListener listener);

    /**
     * Broadcast a message to all online members of the player's gang.
     */
    public void broadcastToMembers (String message, ConfirmListener listener);

    /**
     * Requests a price quote for the specified gang upgrade.  The listener will receive an integer
     * array containing the scip and coin cost to buy the gang upgrade.
     */
    public void getUpgradeQuote (GangGood good, ResultListener listener);
}
