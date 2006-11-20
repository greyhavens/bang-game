//
// $Id$

package com.threerings.bang.store.server;

import com.samskivert.io.PersistenceException;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Item;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.store.data.Good;

/**
 * Creates an item and inserts it into the player's inventory.
 */
public abstract class ItemProvider extends Provider
{
    public ItemProvider (PlayerObject user, Good good, Object[] args)
        throws InvocationException
    {
        super(user, good, args);
        _item = createItem();
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        BangServer.itemrepo.insertItem(_item);
        return null;
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        BangServer.itemrepo.deleteItem(_item, "item_provider_rollback");
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _user.addToInventory(_item);
        super.actionCompleted();
    }

    /** Creates the item that will be delivered by this provider. */
    protected abstract Item createItem ()
        throws InvocationException;

    /** The item that will be delivered. */
    protected Item _item;
}
