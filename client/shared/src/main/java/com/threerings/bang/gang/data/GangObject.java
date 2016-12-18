//
// $Id$

package com.threerings.bang.gang.data;

import java.net.URL;
import java.util.List;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.BuckleUpgrade;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Wallet;
import com.threerings.bang.saloon.data.TopRankObject;
import com.threerings.bang.saloon.data.TopRankedList;

import com.threerings.bang.gang.util.GangUtil;

/**
 * Contains data concerning a single gang.
 */
public class GangObject extends DObject
    implements SpeakObject, TopRankObject, Wallet
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gangPeerService</code> field. */
    public static final String GANG_PEER_SERVICE = "gangPeerService";

    /** The field name of the <code>speakService</code> field. */
    public static final String SPEAK_SERVICE = "speakService";

    /** The field name of the <code>gangId</code> field. */
    public static final String GANG_ID = "gangId";

    /** The field name of the <code>name</code> field. */
    public static final String NAME = "name";

    /** The field name of the <code>founded</code> field. */
    public static final String FOUNDED = "founded";

    /** The field name of the <code>statement</code> field. */
    public static final String STATEMENT = "statement";

    /** The field name of the <code>url</code> field. */
    public static final String URL = "url";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>scrip</code> field. */
    public static final String SCRIP = "scrip";

    /** The field name of the <code>coins</code> field. */
    public static final String COINS = "coins";

    /** The field name of the <code>aces</code> field. */
    public static final String ACES = "aces";

    /** The field name of the <code>notoriety</code> field. */
    public static final String NOTORIETY = "notoriety";

    /** The field name of the <code>buckle</code> field. */
    public static final String BUCKLE = "buckle";

    /** The field name of the <code>outfit</code> field. */
    public static final String OUTFIT = "outfit";

    /** The field name of the <code>inventory</code> field. */
    public static final String INVENTORY = "inventory";

    /** The field name of the <code>members</code> field. */
    public static final String MEMBERS = "members";

    /** The field name of the <code>topRanked</code> field. */
    public static final String TOP_RANKED = "topRanked";

    /** The field name of the <code>tableOid</code> field. */
    public static final String TABLE_OID = "tableOid";

    /** The field name of the <code>rentMultiplier</code> field. */
    public static final String RENT_MULTIPLIER = "rentMultiplier";

    /** The field name of the <code>articleRentMultiplier</code> field. */
    public static final String ARTICLE_RENT_MULTIPLIER = "articleRentMultiplier";
    // AUTO-GENERATED: FIELDS END

    /** Used by peers to make requests on the behalf of their users. */
    public GangPeerMarshaller gangPeerService;

    /** The service used to send chat messages.  This is rewritten for peer nodes. */
    public SpeakMarshaller speakService;

    /** This gang's unique identifier. */
    public int gangId;

    /** The name of this gang. */
    public Handle name;

    /** The day on which this gang was founded. */
    public long founded;

    /** The gang's statement. */
    public String statement;

    /** The gang's URL. */
    public String url;

    /** The gang leader's avatar. */
    public AvatarInfo avatar;

    /** The amount of scrip in the gang's coffers. */
    public int scrip;

    /** The number of coins in the gang's coffers. */
    public int coins;

    /** The number of gang aces available to spend. */
    public int aces;

    /** The gang's rank in terms of notoriety. */
    public byte notoriety;

    /** The ids of the items comprising the currently configured buckle. */
    public int[] buckle;

    /** The currently configured gang outfit. */
    public OutfitArticle[] outfit;

    /** Contains all items held by the gang. */
    public DSet<Item> inventory;

    /** Contains a {@link GangMemberEntry} for each member of this gang. */
    public DSet<GangMemberEntry> members = new DSet<GangMemberEntry>();

    /** Contains info on the top-ranked members by various criterion. */
    public DSet<TopRankedList> topRanked = new DSet<TopRankedList>();

    /** The oid for the table game object.  This is rewritten for peer nodes. */
    public int tableOid;

    /** The rent multiplier for this gang. */
    public float rentMultiplier;

    /** The article rent multiplier for this gang. */
    public float articleRentMultiplier;

    /** On servers using this object as a proxy, the oid on the peer server. */
    public transient int remoteOid;

    // documentation inherited from interface Wallet
    public int getScrip ()
    {
        return scrip;
    }

    // documentation inherited from interface Wallet
    public int getCoins ()
    {
        return coins;
    }

    /**
     * Returns the name used to identity the gang's entry in the coin database.
     */
    public String getCoinAccount ()
    {
        return "{" + name + "}";
    }

    /**
     * Returns the URL of the gang's home page, or <code>null</code> if no valid URL has been
     * configured.
     */
    public URL getURL ()
    {
        try {
            return new URL(url);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Using the buckle parts in the inventory and the part list in the {@link #buckle} field,
     * composes and returns the buckle fingerprint.
     */
    public BuckleInfo getBuckleInfo ()
    {
        return GangUtil.getBuckleInfo(buckle, inventory);
    }

    /**
     * Returns the index of the gang's weight class, which is determined by the upgrades in the
     * inventory (if any).  The weight class determines the maximum number of members and the
     * notoriety cutoffs.
     */
    public byte getWeightClass ()
    {
        return GangUtil.getWeightClass(inventory);
    }

    /**
     * Returns the maximum number of icons that can be used in the buckle.
     */
    public int getMaxBuckleIcons ()
    {
        int icons = GangCodes.DEFAULT_MAX_BUCKLE_ICONS;
        for (Item item : inventory) {
            if (item instanceof BuckleUpgrade) {
                icons = Math.max(icons, ((BuckleUpgrade)item).getIcons());
            }
        }
        return icons;
    }

    /**
     * Returns the {@link GangMemberEntry} corresponding to the most senior active leader of the
     * gang.
     */
    public GangMemberEntry getSeniorLeader ()
    {
        return GangUtil.getSeniorLeader(members);
    }

    /**
     * Returns true if the gang holds an item that is equivalent in content to the one specified.
     */
    public boolean holdsEquivalentItem (Item item)
    {
        for (Item oitem : inventory) {
            if (item.isEquivalent(oitem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a snapshot of this gang's inventory, useful for iterating over if you plan to remove
     * items from their inventory.
     */
    public List<Item> inventorySnapshot ()
    {
        return inventory.toArrayList();
    }

    // documentation inherited from interface SpeakObject
    public String getChatIdentifier (UserMessage message) {
        return DEFAULT_IDENTIFIER;
    }

    // documentation inherited from interface SpeakObject
    public void applyToListeners (SpeakObject.ListenerOp op)
    {
        for (GangMemberEntry member : members) {
            // if they're in the Hideout, then they're subscribed to the gang object
            if (member.isInHideout()) {
                op.apply(this, member.handle);
            }
        }
    }

    // documentation inherited from interface TopRankObject
    public DSet<TopRankedList> getTopRanked ()
    {
        return topRanked;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>gangPeerService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGangPeerService (GangPeerMarshaller value)
    {
        GangPeerMarshaller ovalue = this.gangPeerService;
        requestAttributeChange(
            GANG_PEER_SERVICE, value, ovalue);
        this.gangPeerService = value;
    }

    /**
     * Requests that the <code>speakService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSpeakService (SpeakMarshaller value)
    {
        SpeakMarshaller ovalue = this.speakService;
        requestAttributeChange(
            SPEAK_SERVICE, value, ovalue);
        this.speakService = value;
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
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setName (Handle value)
    {
        Handle ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
    }

    /**
     * Requests that the <code>founded</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFounded (long value)
    {
        long ovalue = this.founded;
        requestAttributeChange(
            FOUNDED, Long.valueOf(value), Long.valueOf(ovalue));
        this.founded = value;
    }

    /**
     * Requests that the <code>statement</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStatement (String value)
    {
        String ovalue = this.statement;
        requestAttributeChange(
            STATEMENT, value, ovalue);
        this.statement = value;
    }

    /**
     * Requests that the <code>url</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setUrl (String value)
    {
        String ovalue = this.url;
        requestAttributeChange(
            URL, value, ovalue);
        this.url = value;
    }

    /**
     * Requests that the <code>avatar</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvatar (AvatarInfo value)
    {
        AvatarInfo ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
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
     * Requests that the <code>aces</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAces (int value)
    {
        int ovalue = this.aces;
        requestAttributeChange(
            ACES, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.aces = value;
    }

    /**
     * Requests that the <code>notoriety</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNotoriety (byte value)
    {
        byte ovalue = this.notoriety;
        requestAttributeChange(
            NOTORIETY, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.notoriety = value;
    }

    /**
     * Requests that the <code>buckle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBuckle (int[] value)
    {
        int[] ovalue = this.buckle;
        requestAttributeChange(
            BUCKLE, value, ovalue);
        this.buckle = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>buckle</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBuckleAt (int value, int index)
    {
        int ovalue = this.buckle[index];
        requestElementUpdate(
            BUCKLE, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.buckle[index] = value;
    }

    /**
     * Requests that the <code>outfit</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setOutfit (OutfitArticle[] value)
    {
        OutfitArticle[] ovalue = this.outfit;
        requestAttributeChange(
            OUTFIT, value, ovalue);
        this.outfit = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>outfit</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setOutfitAt (OutfitArticle value, int index)
    {
        OutfitArticle ovalue = this.outfit[index];
        requestElementUpdate(
            OUTFIT, index, value, ovalue);
        this.outfit[index] = value;
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
    public void removeFromInventory (Comparable<?> key)
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
    public void setInventory (DSet<Item> value)
    {
        requestAttributeChange(INVENTORY, value, this.inventory);
        DSet<Item> clone = (value == null) ? null : value.clone();
        this.inventory = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>members</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMembers (GangMemberEntry elem)
    {
        requestEntryAdd(MEMBERS, members, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>members</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMembers (Comparable<?> key)
    {
        requestEntryRemove(MEMBERS, members, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>members</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMembers (GangMemberEntry elem)
    {
        requestEntryUpdate(MEMBERS, members, elem);
    }

    /**
     * Requests that the <code>members</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMembers (DSet<GangMemberEntry> value)
    {
        requestAttributeChange(MEMBERS, value, this.members);
        DSet<GangMemberEntry> clone = (value == null) ? null : value.clone();
        this.members = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>topRanked</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTopRanked (TopRankedList elem)
    {
        requestEntryAdd(TOP_RANKED, topRanked, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>topRanked</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTopRanked (Comparable<?> key)
    {
        requestEntryRemove(TOP_RANKED, topRanked, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>topRanked</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTopRanked (TopRankedList elem)
    {
        requestEntryUpdate(TOP_RANKED, topRanked, elem);
    }

    /**
     * Requests that the <code>topRanked</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTopRanked (DSet<TopRankedList> value)
    {
        requestAttributeChange(TOP_RANKED, value, this.topRanked);
        DSet<TopRankedList> clone = (value == null) ? null : value.clone();
        this.topRanked = clone;
    }

    /**
     * Requests that the <code>tableOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTableOid (int value)
    {
        int ovalue = this.tableOid;
        requestAttributeChange(
            TABLE_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.tableOid = value;
    }

    /**
     * Requests that the <code>rentMultiplier</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRentMultiplier (float value)
    {
        float ovalue = this.rentMultiplier;
        requestAttributeChange(
            RENT_MULTIPLIER, Float.valueOf(value), Float.valueOf(ovalue));
        this.rentMultiplier = value;
    }

    /**
     * Requests that the <code>articleRentMultiplier</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setArticleRentMultiplier (float value)
    {
        float ovalue = this.articleRentMultiplier;
        requestAttributeChange(
            ARTICLE_RENT_MULTIPLIER, Float.valueOf(value), Float.valueOf(ovalue));
        this.articleRentMultiplier = value;
    }
    // AUTO-GENERATED: METHODS END
}
