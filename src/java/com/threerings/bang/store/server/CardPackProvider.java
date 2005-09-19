//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.CardItem;

import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.Good;

/**
 * Creates and delivers cards to a player when they buy a pack of cards in
 * the General Store.
 */
public class CardPackProvider extends Provider
{
    public CardPackProvider (BangUserObject user, Good good)
        throws InvocationException
    {
        super(user, good);

        // create a random selection of cards
        _cards = new String[((CardPackGood)good).getSize()];
        for (int ii = 0; ii < _cards.length; ii++) {
        }
    }

    @Override // documentation inherited
    protected void persistentAction ()
        throws PersistenceException
    {
//         BangServer.itemrepo.insertItem(_item);
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
//         BangServer.itemrepo.deleteItem(_item, "provider_rollback");
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        // send a custom message to their user object detailing the cards
        // they just received so that they can be nicely displayed
        _user.postMessage(CardPackGood.PURCHASED_CARDS, new Object[] { _cards});

        super.actionCompleted();
    }

    /** The cards that will be delivered. */
    protected String[] _cards;

    /** Inventory items that will be updated as a result of this purchase. */
    protected ArrayList<CardItem> _updates = new ArrayList<CardItem>();

    /** Inventory items that will be added as a result of this purchase. */
    protected ArrayList<CardItem> _additions = new ArrayList<CardItem>();

    /** Inventory items that were be updated as a result of this purchase in
     * their pre-updated form. */
    protected ArrayList<CardItem> _originals = new ArrayList<CardItem>();
}
