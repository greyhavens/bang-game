//
// $Id$

package com.threerings.bang.store.server;

import com.google.inject.Inject;
import com.samskivert.io.PersistenceException;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Item;

import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.store.data.Good;

/**
 * Creates an item and inserts it into the player's inventory.
 */
public class ItemProvider extends Provider
{
    public ItemProvider (PlayerObject user, Good good, Object[] args)
        throws InvocationException
    {
        super(user, good, args);
        _item = createItem();
        if (!_item.allowsDuplicates() && user.holdsEquivalentItem(_item)) {
            throw new InvocationException("m.already_owned");
        }
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        // we check here as well as on the dobj thread because another server may have
        // created the item
        return (_itemrepo.insertItem(_item) ? null : "m.already_owned");
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        _itemrepo.deleteItem(_item, "item_provider_rollback");
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _user.addToInventory(_item);
        super.actionCompleted();
    }

    /** Creates the item that will be delivered by this provider. */
    protected Item createItem ()
        throws InvocationException
    {
        return _good.createItem(_user.playerId);
    }

    /** The item that will be delivered. */
    protected Item _item;

    // depends
    @Inject protected ItemRepository _itemrepo;
}
