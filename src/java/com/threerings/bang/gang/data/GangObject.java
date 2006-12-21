//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;

import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.data.TopRankObject;
import com.threerings.bang.saloon.data.TopRankedList;

/**
 * Contains data concerning a single gang.
 */
public class GangObject extends DObject
    implements SpeakObject, TopRankObject
{
    // AUTO-GENERATED: FIELDS START
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

    /** The field name of the <code>notoriety</code> field. */
    public static final String NOTORIETY = "notoriety";

    /** The field name of the <code>members</code> field. */
    public static final String MEMBERS = "members";

    /** The field name of the <code>topRanked</code> field. */
    public static final String TOP_RANKED = "topRanked";
    // AUTO-GENERATED: FIELDS END

    /** Used for chatting among the gang members. */
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
    public int[] avatar;
    
    /** The amount of scrip in the gang's coffers. */
    public int scrip;
    
    /** The number of coins in the gang's coffers. */
    public int coins;
    
    /** The gang's total notoriety. */
    public int notoriety;
    
    /** Contains a {@link GangMemberInfo} for each member of this gang. */
    public DSet<GangMemberEntry> members = new DSet<GangMemberEntry>();

    /** Contains info on the top-ranked members by various criterion. */
    public DSet<TopRankedList> topRanked = new DSet<TopRankedList>();
    
    // documentation inherited from interface SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        // no-op
    }
    
    // documentation inherited from interface TopRankObject
    public DSet<TopRankedList> getTopRanked ()
    {
        return topRanked;
    }
    
    /**
     * Returns the {@link GangMemberEntry} corresponding to the most senior leader of the gang.
     */
    public GangMemberEntry getSeniorLeader ()
    {
        GangMemberEntry senior = null;
        for (GangMemberEntry entry : members) {
            if (entry.rank == GangCodes.LEADER_RANK &&
                (senior == null || entry.rank < senior.rank)) {
                senior = entry;
            }
        }
        return senior;
    }
    
    // AUTO-GENERATED: METHODS START
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
    public void setAvatar (int[] value)
    {
        int[] ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>avatar</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setAvatarAt (int value, int index)
    {
        int ovalue = this.avatar[index];
        requestElementUpdate(
            AVATAR, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.avatar[index] = value;
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
     * Requests that the <code>notoriety</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNotoriety (int value)
    {
        int ovalue = this.notoriety;
        requestAttributeChange(
            NOTORIETY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.notoriety = value;
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
    public void removeFromMembers (Comparable key)
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
    public void setMembers (DSet<com.threerings.bang.gang.data.GangMemberEntry> value)
    {
        requestAttributeChange(MEMBERS, value, this.members);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.gang.data.GangMemberEntry> clone =
            (value == null) ? null : value.typedClone();
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
    public void removeFromTopRanked (Comparable key)
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
    public void setTopRanked (DSet<com.threerings.bang.saloon.data.TopRankedList> value)
    {
        requestAttributeChange(TOP_RANKED, value, this.topRanked);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.saloon.data.TopRankedList> clone =
            (value == null) ? null : value.typedClone();
        this.topRanked = clone;
    }
    // AUTO-GENERATED: METHODS END
}
