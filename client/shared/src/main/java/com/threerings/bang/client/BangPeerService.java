//
// $Id$

package com.threerings.bang.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;

/**
 * Defines the service through which servers send Bang-specific requests to other servers.
 */
public interface BangPeerService extends InvocationService<ClientObject>
{
    /**
     * Notifies the server that one of its online users has received a request to be someone's
     * pardner.
     */
    public void deliverPardnerInvite (Handle invitee, Handle inviter, String message);

    /**
     * Notifies the server that one of its online users has received a response to a pardner
     * request.
     */
    public void deliverPardnerInviteResponse (
        Handle inviter, Handle invitee, boolean accept, boolean full);

    /**
     * Notifies the server that one of its online users has lost a pardner.
     */
    public void deliverPardnerRemoval (Handle removee, Handle remover);

    /**
     * Notifies the server that one of its online users has received a request to be a member of
     * someone's gang.
     */
    public void deliverGangInvite (
        Handle invitee, Handle inviter, int gangId, Handle name, String message);

    /**
     * Notifies the server that one of its online users has received an item.
     *
     * @param source a qualified translatable string describing the source of the item
     */
    public void deliverItem (Item item, String source);

    /**
     * Requests the oid of a gang for which this peer holds a lock.
     */
    public void getGangOid (int gangId, ResultListener listener);
}
