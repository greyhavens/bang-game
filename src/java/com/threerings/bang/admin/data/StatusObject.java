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

        /** The scenarios to be used in the game. */
        public String[] scenario;

        // documentation inherited from interface DSet.Entry
        public Comparable getKey () {
            return gameOid;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playersOnline</code> field. */
    public static final String PLAYERS_ONLINE = "playersOnline";

    /** The field name of the <code>serverStartTime</code> field. */
    public static final String SERVER_START_TIME = "serverStartTime";

    /** The field name of the <code>games</code> field. */
    public static final String GAMES = "games";

    /** The field name of the <code>connStats</code> field. */
    public static final String CONN_STATS = "connStats";
    // AUTO-GENERATED: FIELDS END

    /** The number of players online on this server. */
    public int playersOnline;

    /** The time at which the server started up. */
    public long serverStartTime;

    /** Information on all active games. */
    public DSet games = new DSet();

    /** Stats on our connection manager. */
    public ConMgrStats connStats;

    // AUTO-GENERATED: METHODS START
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
            PLAYERS_ONLINE, new Integer(value), new Integer(ovalue));
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
            SERVER_START_TIME, new Long(value), new Long(ovalue));
        this.serverStartTime = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>games</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGames (DSet.Entry elem)
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
    public void updateGames (DSet.Entry elem)
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
    public void setGames (DSet value)
    {
        requestAttributeChange(GAMES, value, this.games);
        this.games = (value == null) ? null : (DSet)value.clone();
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
    // AUTO-GENERATED: METHODS END
}
