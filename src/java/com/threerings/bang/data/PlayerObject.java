//
// $Id$

package com.threerings.bang.data;

import java.util.Iterator;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.TokenRing;

import com.threerings.bang.avatar.data.Look;

/**
 * Extends the {@link BodyObject} with custom bits needed by Bang!.
 */
public class PlayerObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerId</code> field. */
    public static final String PLAYER_ID = "playerId";

    /** The field name of the <code>handle</code> field. */
    public static final String HANDLE = "handle";

    /** The field name of the <code>isMale</code> field. */
    public static final String IS_MALE = "isMale";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>inventory</code> field. */
    public static final String INVENTORY = "inventory";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scrip</code> field. */
    public static final String SCRIP = "scrip";

    /** The field name of the <code>coins</code> field. */
    public static final String COINS = "coins";

    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>ratings</code> field. */
    public static final String RATINGS = "ratings";

    /** The field name of the <code>look</code> field. */
    public static final String LOOK = "look";

    /** The field name of the <code>looks</code> field. */
    public static final String LOOKS = "looks";

    /** The field name of the <code>pardners</code> field. */
    public static final String PARDNERS = "pardners";
    // AUTO-GENERATED: FIELDS END

    /** This user's persistent unique id. */
    public int playerId;

    /** This user's cowboy handle (in-game name). */
    public Handle handle;

    /** Whether this character is male or female. */
    public boolean isMale;

    /** Indicates which access control tokens are held by this user. */
    public TokenRing tokens;

    /** Contains all items held by this user. */
    public DSet inventory;

    /** Indicates which town this user currently occupies. */
    public String townId;

    /** The amount of game currency this player is carrying. */
    public int scrip;

    /** The amount of "hard" currency this player is carrying. */
    public int coins;

    /** Statistics tracked for this player. */
    public StatSet stats;

    /** Contains all ratings earned by this player. */
    public DSet ratings;

    /** This player's current avatar look. */
    public String look;

    /** The avatar looks this player has available. */
    public DSet looks;

    /** {@link PardnerEntry}s for each of the player's pardners. */
    public DSet pardners;
    
    /**
     * Returns the player's rating for the specified scenario. This method will
     * never return null.
     */
    public Rating getRating (String scenario)
    {
        Rating rating = (Rating)ratings.get(scenario);
        if (rating == null) {
            rating = new Rating();
            rating.scenario = scenario;
        }
        return rating;
    }

    /**
     * Returns the purse owned by this player or the default purse if the
     * player does not yet have one.
     */
    public Purse getPurse ()
    {
        for (Iterator iter = inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof Purse) {
                return (Purse)item;
            }
        }
        return Purse.DEFAULT_PURSE;
    }

    /**
     * Returns the look currently in effect for this player.
     */
    public Look getLook ()
    {
        return (Look)looks.get(look);
    }

    /**
     * Returns true if this player has at least one {@link BigShotItem} in
     * their inventory.
     */
    public boolean hasBigShot ()
    {
        for (Iterator iter = inventory.iterator(); iter.hasNext(); ) {
            if (iter.next() instanceof BigShotItem) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public TokenRing getTokens ()
    {
        return tokens;
    }

    @Override // documentation inherited
    public OccupantInfo createOccupantInfo ()
    {
        return new BangOccupantInfo(this);
    }

    @Override // documentation inherited
    public Name getVisibleName ()
    {
        return handle;
    }

    @Override // documentation inherited
    public String who ()
    {
        return "'" + handle + "' " + super.who();
    }

    /**
     * Counts the number of avatar articles in this player's inventory.
     */
    public int getDudsCount ()
    {
        int count = 0;
        for (Iterator iter = inventory.iterator(); iter.hasNext(); ) {
            if (iter.next() instanceof Article) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines how many of this player's pardners are online.
     */
    public int getOnlinePardnerCount ()
    {
        int count = 0;
        for (Iterator it = pardners.iterator(); it.hasNext(); ) {
            if (((PardnerEntry)it.next()).isOnline()) {
                count++;
            }
        }
        return count;
    }
    
    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>playerId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayerId (int value)
    {
        int ovalue = this.playerId;
        requestAttributeChange(
            PLAYER_ID, new Integer(value), new Integer(ovalue));
        this.playerId = value;
    }

    /**
     * Requests that the <code>handle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHandle (Handle value)
    {
        Handle ovalue = this.handle;
        requestAttributeChange(
            HANDLE, value, ovalue);
        this.handle = value;
    }

    /**
     * Requests that the <code>isMale</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setIsMale (boolean value)
    {
        boolean ovalue = this.isMale;
        requestAttributeChange(
            IS_MALE, new Boolean(value), new Boolean(ovalue));
        this.isMale = value;
    }

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

    /**
     * Requests that the <code>scrip</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScrip (int value)
    {
        int ovalue = this.scrip;
        requestAttributeChange(
            SCRIP, new Integer(value), new Integer(ovalue));
        this.scrip = value;
    }

    /**
     * Requests that the <code>coins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setCoins (int value)
    {
        int ovalue = this.coins;
        requestAttributeChange(
            COINS, new Integer(value), new Integer(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>stats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToStats (DSet.Entry elem)
    {
        requestEntryAdd(STATS, stats, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>stats</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromStats (Comparable key)
    {
        requestEntryRemove(STATS, stats, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>stats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateStats (DSet.Entry elem)
    {
        requestEntryUpdate(STATS, stats, elem);
    }

    /**
     * Requests that the <code>stats</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setStats (StatSet value)
    {
        requestAttributeChange(STATS, value, this.stats);
        this.stats = (value == null) ? null : (StatSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ratings</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRatings (DSet.Entry elem)
    {
        requestEntryAdd(RATINGS, ratings, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>ratings</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromRatings (Comparable key)
    {
        requestEntryRemove(RATINGS, ratings, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>ratings</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateRatings (DSet.Entry elem)
    {
        requestEntryUpdate(RATINGS, ratings, elem);
    }

    /**
     * Requests that the <code>ratings</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setRatings (DSet value)
    {
        requestAttributeChange(RATINGS, value, this.ratings);
        this.ratings = (value == null) ? null : (DSet)value.clone();
    }

    /**
     * Requests that the <code>look</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLook (String value)
    {
        String ovalue = this.look;
        requestAttributeChange(
            LOOK, value, ovalue);
        this.look = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>looks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToLooks (DSet.Entry elem)
    {
        requestEntryAdd(LOOKS, looks, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>looks</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromLooks (Comparable key)
    {
        requestEntryRemove(LOOKS, looks, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>looks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateLooks (DSet.Entry elem)
    {
        requestEntryUpdate(LOOKS, looks, elem);
    }

    /**
     * Requests that the <code>looks</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setLooks (DSet value)
    {
        requestAttributeChange(LOOKS, value, this.looks);
        this.looks = (value == null) ? null : (DSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pardners</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPardners (DSet.Entry elem)
    {
        requestEntryAdd(PARDNERS, pardners, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>pardners</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPardners (Comparable key)
    {
        requestEntryRemove(PARDNERS, pardners, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>pardners</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePardners (DSet.Entry elem)
    {
        requestEntryUpdate(PARDNERS, pardners, elem);
    }

    /**
     * Requests that the <code>pardners</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPardners (DSet value)
    {
        requestAttributeChange(PARDNERS, value, this.pardners);
        this.pardners = (value == null) ? null : (DSet)value.clone();
    }
    // AUTO-GENERATED: METHODS END
}
