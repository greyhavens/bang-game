//
// $Id$

package com.threerings.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Item;

/**
 * Defines the service through which servers send Bang-specific requests to other servers.
 */
public interface BangPeerService extends InvocationService
{
    /**
     * Notifies the server that one of its online users has received an item.
     *
     * @param source a qualified translatable string describing the source of the item
     */
    public void deliverItem (Client client, Item item, String source);
}
