//
// $Id$

package com.threerings.bang.store.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.inject.Inject;
import com.samskivert.io.PersistenceException;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.server.persist.ItemRepository;

import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.Good;

/**
 * Creates and delivers cards to a player when they buy a pack of cards in the General Store.
 */
public class CardPackProvider extends Provider
{
    public CardPackProvider (PlayerObject user, Good good)
        throws InvocationException
    {
        super(user, good, null);

        // create a random selection of cards
        _cards = new String[((CardPackGood)good).getSize()];
        for (int ii = 0; ii < _cards.length; ii++) {
            _cards[ii] = Card.selectRandomCard(good.getTownId(), null, -1);
        }

        // now determine which of those cards are already held by the player and which require the
        // creation of new CardItem inventory items and add the cards to the appropriate items
        HashMap<String,CardItem> have = new HashMap<String,CardItem>();
        for (Item item : user.inventory) {
            if (item instanceof CardItem) {
                CardItem citem = (CardItem)item;
                have.put(citem.getType(), citem);
            }
        }
        for (int ii = 0; ii < _cards.length; ii++) {
            CardItem item = _items.get(_cards[ii]);
            if (item == null) {
                item = have.get(_cards[ii]);
                if (item == null) {
                    item = new CardItem(user.playerId, _cards[ii]);
                } else {
                    _originals.add(item);
                    item = (CardItem)item.clone();
                }
                _items.put(_cards[ii], item);
            }
            item.addCard();
        }
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        // insert or update the various items
        for (CardItem item : _items.values()) {
            if (item.getItemId() == 0) {
                _itemrepo.insertItem(item);
            } else {
                _itemrepo.updateItem(item);
            }
        }
        return null;
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        // restore the original items; removing them from the items map in the process
        for (CardItem item : _originals) {
            _itemrepo.updateItem(item);
            _items.remove(item.getType());
        }

        // now the items remaining in the mapping are all newly created; delete those which have an
        // item id associated with them
        for (CardItem item : _items.values()) {
            if (item.getItemId() != 0) {
                _itemrepo.deleteItem(
                    item, "cardpack_provider_rollback");
            }
        }
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        // broadcast the update to the player's inventory
        for (CardItem item : _items.values()) {
            if (_user.inventory.contains(item)) {
                _user.updateInventory(item);
            } else {
                _user.addToInventory(item);
            }
        }

        // send a custom message to their user object detailing the cards they just received so
        // that they can be nicely displayed
        _user.postMessage(CardPackGood.PURCHASED_CARDS, new Object[] { _cards });

        super.actionCompleted();
    }

    /** The cards that will be delivered. */
    protected String[] _cards;

    /** Inventory items that will be added or updated as a result of this purchase. */
    protected HashMap<String,CardItem> _items = new HashMap<String,CardItem>();

    /** Inventory items updated as a result of this purchase in their pre-updated form. */
    protected ArrayList<CardItem> _originals = new ArrayList<CardItem>();

    // dependencies
    @Inject protected ItemRepository _itemrepo;
}
