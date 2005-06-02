//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

/**
 * Extends the {@link BodyObject} with custom bits needed by Bang!.
 */
public class BangUserObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>inventory</code> field. */
    public static final String INVENTORY = "inventory";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";
    // AUTO-GENERATED: FIELDS END

    /** This user's persistent unique id. */
    public int userId;

    /** Indicates which access control tokens are held by this user. */
    public TokenRing tokens;

    /** Contains all items held by this user. */
    public DSet inventory;

    /** Indicates which town this user currently occupies. */
    public String townId;

    @Override // documentation inherited
    public TokenRing getTokens ()
    {
        return tokens;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (TokenRing value)
    {
        TokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToInventory (DSet.Entry elem)
    {
        requestEntryAdd(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>inventory</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromInventory (Comparable key)
    {
        requestEntryRemove(INVENTORY, inventory, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateInventory (DSet.Entry elem)
    {
        requestEntryUpdate(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the <code>inventory</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setInventory (DSet value)
    {
        requestAttributeChange(INVENTORY, value, this.inventory);
        this.inventory = (value == null) ? null : (DSet)value.clone();
    }

    /**
     * Requests that the <code>townId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTownId (String value)
    {
        String ovalue = this.townId;
        requestAttributeChange(
            TOWN_ID, value, ovalue);
        this.townId = value;
    }
    // AUTO-GENERATED: METHODS END
}
