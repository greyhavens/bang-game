//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.NameCreator;

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
}
