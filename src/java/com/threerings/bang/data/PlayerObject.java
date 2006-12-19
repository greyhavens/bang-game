//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.gang.data.GangCodes;

/**
 * Extends the {@link BodyObject} with custom bits needed by Bang!.
 */
public class PlayerObject extends BodyObject
    implements StatSet.Container
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

    /** The field name of the <code>gangId</code> field. */
    public static final String GANG_ID = "gangId";

    /** The field name of the <code>gangOid</code> field. */
    public static final String GANG_OID = "gangOid";

    /** The field name of the <code>gangRank</code> field. */
    public static final String GANG_RANK = "gangRank";

    /** The field name of the <code>joinedGang</code> field. */
    public static final String JOINED_GANG = "joinedGang";

    /** The field name of the <code>gangNotoriety</code> field. */
    public static final String GANG_NOTORIETY = "gangNotoriety";

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

    /** The field name of the <code>poses</code> field. */
    public static final String POSES = "poses";

    /** The field name of the <code>looks</code> field. */
    public static final String LOOKS = "looks";

    /** The field name of the <code>pardners</code> field. */
    public static final String PARDNERS = "pardners";

    /** The field name of the <code>notifications</code> field. */
    public static final String NOTIFICATIONS = "notifications";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";

    /** The field name of the <code>foes</code> field. */
    public static final String FOES = "foes";

    /** The field name of the <code>lastScenId</code> field. */
    public static final String LAST_SCEN_ID = "lastScenId";

    /** The field name of the <code>lastBoardId</code> field. */
    public static final String LAST_BOARD_ID = "lastBoardId";
    // AUTO-GENERATED: FIELDS END

    /** This user's persistent unique id. */
    public int playerId;

    /** This user's cowboy handle (in-game name). */
    public Handle handle;

    /** Whether this character is male or female. */
    public boolean isMale;

    /** Indicates which access control tokens are held by this user. */
    public BangTokenRing tokens;

    /** The id of the user's gang, if any. */
    public int gangId;
    
    /** The oid of the gang dobj. */
    public int gangOid;
    
    /** The user's rank in the gang. */
    public byte gangRank;
    
    /** The date upon which the user joined the gang. */
    public long joinedGang;
    
    /** The amount of notoriety that the user has gained for his gang. */
    public int gangNotoriety;
    
    /** Contains all items held by this user. */
    public DSet<Item> inventory;

    /** Indicates which town this user currently occupies. */
    public String townId;

    /** The amount of game currency this player is carrying. */
    public int scrip;

    /** The amount of "hard" currency this player is carrying. */
    public int coins;

    /** Statistics tracked for this player. */
    public StatSet stats;

    /** Contains all ratings earned by this player. */
    public DSet<Rating> ratings;

    /** This player's configured avatar poses. See {@link Look.Pose}. */
    public String[] poses;

    /** The avatar looks this player has available. */
    public DSet<Look> looks;

    /** {@link PardnerEntry}s for each of the player's pardners. */
    public DSet<PardnerEntry> pardners;

    /** The set of notifications pending response (pardner requests, etc.) */
    public DSet<Notification> notifications;
    
    /** The player ids of this player's friendly folks, sorted. */
    public int[] friends;

    /** The player ids of this player's not so friendly folks, sorted. */
    public int[] foes;

    /** The last scenario played by this player. */
    public String lastScenId;

    /** The last board played by this player. */
    public int lastBoardId = -1;

    /**
     * Returns the player's rating for the specified scenario. This method will
     * never return null.
     */
    public Rating getRating (String scenario)
    {
        Rating rating = ratings.get(scenario);
        if (rating == null) {
            rating = new Rating();
            rating.scenario = scenario;
        }
        return rating;
    }

    /**
     * Returns the best purse owned by this player or the default purse if the
     * player does not yet have one.
     */
    public Purse getPurse ()
    {
        // use the player's best (highest town indexed) purse
        Purse purse = Purse.DEFAULT_PURSE;
        for (Item item : inventory) {
            if (item instanceof Purse) {
                Purse cpurse = (Purse)item;
                if (cpurse.getTownIndex() > purse.getTownIndex()) {
                    purse = cpurse;
                }
            }
        }
        return purse;
    }

    /**
     * Returns the look currently in effect for this player.
     */
    public Look getLook (Look.Pose pose)
    {
        String pstr = poses[pose.ordinal()];
        return (pstr != null && looks.containsKey(pstr)) ?
            looks.get(pstr) : looks.get("");
    }

    /**
     * Returns true if this player has at least one {@link BigShotItem} in
     * their inventory.
     */
    public boolean hasBigShot ()
    {
        for (Item item : inventory) {
            if (item instanceof BigShotItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this player has earned the specified badge type.
     */
    public boolean holdsBadge (Badge.Type type)
    {
        for (Item item : inventory) {
            if (item instanceof Badge && ((Badge)item).getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this player holds a pass for the specified type of unit.
     */
    public boolean holdsPass (String unit)
    {
        for (Item item : inventory) {
            if (item instanceof UnitPass &&
                ((UnitPass)item).getUnitType().equals(unit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the player holds a ticket to the specified town.
     */
    public boolean holdsTicket (String townId)
    {
        for (Item item : inventory) {
            if (item instanceof TrainTicket &&
                ((TrainTicket)item).getTownId().equals(townId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the specified player's id is in this player's friend list.
     */
    public boolean isFriend (int playerId)
    {
        return Arrays.binarySearch(friends, playerId) >= 0;
    }

    /**
     * Whether the specified player's id is in this player's foe list.
     */
    public boolean isFoe (int playerId)
    {
        return Arrays.binarySearch(foes, playerId) >= 0;
    }

    /**
     * Returns true if the player is in a gang and can recruit other members.
     */
    public boolean canRecruit ()
    {
        return gangId > 0 && gangRank >= GangCodes.RECRUITER_RANK;
    }
    
    @Override // documentation inherited
    public BangTokenRing getTokens ()
    {
        return tokens;
    }

    @Override // documentation inherited
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new BangOccupantInfo(this);
    }

    @Override // documentation inherited
    public Name getVisibleName ()
    {
        // we don't want to return null for our visible name but we do need
        // something unique until they choose a name
        return (handle != null) ? handle : new Name("!!" + username);
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
        for (Item item : inventory) {
            if (item instanceof Article) {
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
        for (PardnerEntry entry : pardners) {
            if (entry.isOnline()) {
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
            PLAYER_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
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
            IS_MALE, Boolean.valueOf(value), Boolean.valueOf(ovalue));
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
    public void setTokens (BangTokenRing value)
    {
        BangTokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the <code>gangId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangId (int value)
    {
        int ovalue = this.gangId;
        requestAttributeChange(
            GANG_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gangId = value;
    }

    /**
     * Requests that the <code>gangOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangOid (int value)
    {
        int ovalue = this.gangOid;
        requestAttributeChange(
            GANG_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gangOid = value;
    }

    /**
     * Requests that the <code>gangRank</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangRank (byte value)
    {
        byte ovalue = this.gangRank;
        requestAttributeChange(
            GANG_RANK, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.gangRank = value;
    }

    /**
     * Requests that the <code>joinedGang</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setJoinedGang (long value)
    {
        long ovalue = this.joinedGang;
        requestAttributeChange(
            JOINED_GANG, Long.valueOf(value), Long.valueOf(ovalue));
        this.joinedGang = value;
    }

    /**
     * Requests that the <code>gangNotoriety</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangNotoriety (int value)
    {
        int ovalue = this.gangNotoriety;
        requestAttributeChange(
            GANG_NOTORIETY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.gangNotoriety = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToInventory (Item elem)
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
    public void updateInventory (Item elem)
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
    public void setInventory (DSet<com.threerings.bang.data.Item> value)
    {
        requestAttributeChange(INVENTORY, value, this.inventory);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.data.Item> clone =
            (value == null) ? null : value.typedClone();
        this.inventory = clone;
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
            SCRIP, Integer.valueOf(value), Integer.valueOf(ovalue));
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
            COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>stats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToStats (Stat elem)
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
    public void updateStats (Stat elem)
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
        @SuppressWarnings("unchecked") StatSet clone =
            (value == null) ? null : (StatSet)value.clone();
        this.stats = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ratings</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRatings (Rating elem)
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
    public void updateRatings (Rating elem)
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
    public void setRatings (DSet<com.threerings.bang.data.Rating> value)
    {
        requestAttributeChange(RATINGS, value, this.ratings);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.data.Rating> clone =
            (value == null) ? null : value.typedClone();
        this.ratings = clone;
    }

    /**
     * Requests that the <code>poses</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPoses (String[] value)
    {
        String[] ovalue = this.poses;
        requestAttributeChange(
            POSES, value, ovalue);
        this.poses = (value == null) ? null : (String[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>poses</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPosesAt (String value, int index)
    {
        String ovalue = this.poses[index];
        requestElementUpdate(
            POSES, index, value, ovalue);
        this.poses[index] = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>looks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToLooks (Look elem)
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
    public void updateLooks (Look elem)
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
    public void setLooks (DSet<com.threerings.bang.avatar.data.Look> value)
    {
        requestAttributeChange(LOOKS, value, this.looks);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.avatar.data.Look> clone =
            (value == null) ? null : value.typedClone();
        this.looks = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pardners</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPardners (PardnerEntry elem)
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
    public void updatePardners (PardnerEntry elem)
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
    public void setPardners (DSet<com.threerings.bang.data.PardnerEntry> value)
    {
        requestAttributeChange(PARDNERS, value, this.pardners);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.data.PardnerEntry> clone =
            (value == null) ? null : value.typedClone();
        this.pardners = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>notifications</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToNotifications (Notification elem)
    {
        requestEntryAdd(NOTIFICATIONS, notifications, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>notifications</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromNotifications (Comparable key)
    {
        requestEntryRemove(NOTIFICATIONS, notifications, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>notifications</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateNotifications (Notification elem)
    {
        requestEntryUpdate(NOTIFICATIONS, notifications, elem);
    }

    /**
     * Requests that the <code>notifications</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setNotifications (DSet<com.threerings.bang.data.Notification> value)
    {
        requestAttributeChange(NOTIFICATIONS, value, this.notifications);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.data.Notification> clone =
            (value == null) ? null : value.typedClone();
        this.notifications = clone;
    }

    /**
     * Requests that the <code>friends</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFriends (int[] value)
    {
        int[] ovalue = this.friends;
        requestAttributeChange(
            FRIENDS, value, ovalue);
        this.friends = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>friends</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setFriendsAt (int value, int index)
    {
        int ovalue = this.friends[index];
        requestElementUpdate(
            FRIENDS, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.friends[index] = value;
    }

    /**
     * Requests that the <code>foes</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFoes (int[] value)
    {
        int[] ovalue = this.foes;
        requestAttributeChange(
            FOES, value, ovalue);
        this.foes = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>foes</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setFoesAt (int value, int index)
    {
        int ovalue = this.foes[index];
        requestElementUpdate(
            FOES, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.foes[index] = value;
    }

    /**
     * Requests that the <code>lastScenId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLastScenId (String value)
    {
        String ovalue = this.lastScenId;
        requestAttributeChange(
            LAST_SCEN_ID, value, ovalue);
        this.lastScenId = value;
    }

    /**
     * Requests that the <code>lastBoardId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLastBoardId (int value)
    {
        int ovalue = this.lastBoardId;
        requestAttributeChange(
            LAST_BOARD_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.lastBoardId = value;
    }
    // AUTO-GENERATED: METHODS END
}
