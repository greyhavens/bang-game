//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.util.Name;

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
        Client client, Name root, String suffix, ConfirmListener listener);
    
    /**
     * Requests to leave the current gang.
     */
    public void leaveGang (Client client, ConfirmListener listener);
    
    /**
     * Requests to contribute scrip and/or coins to the gang's coffers.
     */
    public void addToCoffers (
        Client client, int scrip, int coins, ConfirmListener listener);
}
