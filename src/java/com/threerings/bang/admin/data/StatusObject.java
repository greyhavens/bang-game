//
// $Id$

package com.threerings.bang.admin.data;

import com.threerings.presents.data.ConMgrStats;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains server status information that is communicated to admins via the
 * Server Dashboard display.
 */
public class StatusObject extends DObject
{
    /** Used to keep tabs on games in play. */
    public static class GameInfo implements DSet.Entry
    {
        /** The oid of the game object. */
        public Integer gameOid;

        /** The number of human players in the game. */
        public int players;

        // documentation inherited from interface DSet.Entry
        public Comparable getKey () {
            return gameOid;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>playersOnline</code> field. */
    public static final String PLAYERS_ONLINE = "playersOnline";

    /** The field name of the <code>serverStartTime</code> field. */
    public static final String SERVER_START_TIME = "serverStartTime";

    /** The field name of the <code>games</code> field. */
    public static final String GAMES = "games";

    /** The field name of the <code>pendingMatches</code> field. */
    public static final String PENDING_MATCHES = "pendingMatches";

    /** The field name of the <code>connStats</code> field. */
    public static final String CONN_STATS = "connStats";

    /** The field name of the <code>serverRebootTime</code> field. */
    public static final String SERVER_REBOOT_TIME = "serverRebootTime";
    // AUTO-GENERATED: FIELDS END

    /** Provides admin services. */
    public BangAdminMarshaller service;

    /** The number of players online on this server. */
    public int playersOnline;

    /** The time at which the server started up. */
    public long serverStartTime;

    /** Information on all active games. */
    public DSet<GameInfo> games = new DSet<GameInfo>();

    /** The number of matches waiting for players. */
    public int pendingMatches;

    /** Stats on our connection manager. */
    public ConMgrStats connStats;

    /** The time at which a reboot is scheduled or 0L. */
    public long serverRebootTime;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (BangAdminMarshaller value)
    {
        BangAdminMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>playersOnline</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayersOnline (int value)
    {
        int ovalue = this.playersOnline;
        requestAttributeChange(
            PLAYERS_ONLINE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.playersOnline = value;
    }

    /**
     * Requests that the <code>serverStartTime</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setServerStartTime (long value)
    {
        long ovalue = this.serverStartTime;
        requestAttributeChange(
            SERVER_START_TIME, Long.valueOf(value), Long.valueOf(ovalue));
        this.serverStartTime = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>games</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGames (StatusObject.GameInfo elem)
    {
        requestEntryAdd(GAMES, games, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>games</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGames (Comparable key)
    {
        requestEntryRemove(GAMES, games, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>games</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGames (StatusObject.GameInfo elem)
    {
        requestEntryUpdate(GAMES, games, elem);
    }

    /**
     * Requests that the <code>games</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGames (DSet<com.threerings.bang.admin.data.StatusObject.GameInfo> value)
    {
        requestAttributeChange(GAMES, value, this.games);
        @SuppressWarnings("unchecked") DSet<com.threerings.bang.admin.data.StatusObject.GameInfo> clone =
            (value == null) ? null : value.typedClone();
        this.games = clone;
    }

    /**
     * Requests that the <code>pendingMatches</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPendingMatches (int value)
    {
        int ovalue = this.pendingMatches;
        requestAttributeChange(
            PENDING_MATCHES, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.pendingMatches = value;
    }

    /**
     * Requests that the <code>connStats</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setConnStats (ConMgrStats value)
    {
        ConMgrStats ovalue = this.connStats;
        requestAttributeChange(
            CONN_STATS, value, ovalue);
        this.connStats = value;
    }

    /**
     * Requests that the <code>serverRebootTime</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setServerRebootTime (long value)
    {
        long ovalue = this.serverRebootTime;
        requestAttributeChange(
            SERVER_REBOOT_TIME, Long.valueOf(value), Long.valueOf(ovalue));
        this.serverRebootTime = value;
    }
    // AUTO-GENERATED: METHODS END
}
