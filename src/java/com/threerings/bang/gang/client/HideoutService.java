//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameCreator;

import com.threerings.bang.gang.data.HistoryEntry;

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
}
