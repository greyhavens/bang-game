//
// $Id$

package com.threerings.bang.gang.server;

import com.samskivert.io.PersistenceException;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.Item;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;

/**
 * Creates an item and inserts it into the gang's inventory.
 */
public class GangItemProvider extends GangGoodProvider
{
    public GangItemProvider (GangObject gang, boolean admin, GangGood good, Object[] args)
        throws InvocationException
    {
        super(gang, admin, good, args);
        _item = createItem();
        if (!_item.allowsDuplicates() && gang.holdsEquivalentItem(_item)) {
            throw new InvocationException("m.already_owned");
        }
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        // we check here as well as on the dobj thread because another server may have
        // created the item
        return (BangServer.itemrepo.insertItem(_item) ? null : "m.already_owned");
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
        _gang.addToInventory(_item);
        super.actionCompleted();
    }

    /** Creates the item that will be delivered by this provider. */
    protected Item createItem ()
        throws InvocationException
    {
        return _good.createItem(_gang.gangId);
    }

    /** The item that will be delivered. */
    protected Item _item;
}
