//
// $Id$

package com.threerings.bang.admin.data;

import com.threerings.nio.conman.ConMgrStats;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.BangConfig;

/**
 * Contains server status information that is communicated to admins via the
 * Server Dashboard display.
 */
public class StatusObject extends DObject
{
    /** A superset of {@link BangConfig.Type} which distinguishes between ranked and gang games. */
    public static enum GameType {
        TUTORIAL, PRACTICE, BOUNTY, SALOON, RANKED, GANG
    };

    /** Used to keep tabs on games in play. */
    public static class GameInfo implements DSet.Entry
    {
        /** The oid of the game object. */
        public Integer gameOid;

        /** The type of game being played. */
        public GameType type;

        /** The number of human players in the game. */
        public int players;

        // documentation inherited from interface DSet.Entry
        public Comparable<?> getKey () {
            return gameOid;
        }

        public GameInfo () {
        }

        public GameInfo (int gameOid, BangConfig.Type type, boolean rated, boolean awardAces,
                         int players) {
            this.gameOid = gameOid;
            this.players = players;
            if (type == BangConfig.Type.SALOON) {
                this.type = awardAces ? GameType.GANG : (rated ? GameType.RANKED : GameType.SALOON);
            } else {
                this.type = Enum.valueOf(GameType.class, type.toString());
            }
        }
    }

    /** Used to keep track of the population of players in different places. */
    public static class PlaceInfo implements DSet.Entry
    {
        /** The string identifier for this place. */
        public String ident;

        /** The number of players in the house. */
        public int occupants;

        /** The time at which we should next publish this place info. */
        public transient long nextPublishStamp;

        // documentation inherited from interface DSet.Entry
        public Comparable<?> getKey () {
            return ident;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>serverStartTime</code> field. */
    public static final String SERVER_START_TIME = "serverStartTime";

    /** The field name of the <code>serverRebootTime</code> field. */
    public static final String SERVER_REBOOT_TIME = "serverRebootTime";

    /** The field name of the <code>connStats</code> field. */
    public static final String CONN_STATS = "connStats";

    /** The field name of the <code>playersOnline</code> field. */
    public static final String PLAYERS_ONLINE = "playersOnline";

    /** The field name of the <code>places</code> field. */
    public static final String PLACES = "places";

    /** The field name of the <code>pendingMatches</code> field. */
    public static final String PENDING_MATCHES = "pendingMatches";

    /** The field name of the <code>games</code> field. */
    public static final String GAMES = "games";
    // AUTO-GENERATED: FIELDS END

    /** Provides admin services. */
    public BangAdminMarshaller service;

    /** The time at which the server started up. */
    public long serverStartTime;

    /** The time at which a reboot is scheduled or 0L. */
    public long serverRebootTime;

    /** Stats on our connection manager. */
    public ConMgrStats connStats;

    /** The number of players online on this server. */
    public int playersOnline;

    /** Contains information on the populations of the various places. */
    public DSet<PlaceInfo> places = new DSet<PlaceInfo>();

    /** The number of matches waiting for players. */
    public int pendingMatches;

    /** Information on all active games. */
    public DSet<GameInfo> games = new DSet<GameInfo>();

    /**
     * Massages and updates the players online count (rounding to the nearest 5 when >1000 players
     * to avoid super frequent updates).
     */
    public void updatePlayersOnline (int players)
    {
        if (players > 1000) {
            players = 5 * Math.round(players/5f);
        }
        setPlayersOnline(players);
    }

    /**
     * Updates the number of occupants in the specified place.
     */
    public void updatePlaceInfo (String ident, int occupants)
    {
        long now = System.currentTimeMillis();
        PlaceInfo info = places.get(ident);
        if (info == null) {
            info = new PlaceInfo();
            info.ident = ident;
            info.occupants = occupants;
            info.nextPublishStamp = now + PI_PUBLISH_INTERVAL;
            addToPlaces(info);

        } else {
            info.occupants = occupants;
            if (now > info.nextPublishStamp) {
                info.nextPublishStamp = now + PI_PUBLISH_INTERVAL;
                updatePlaces(info);
            }
        }
    }

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
     * Requests that the specified entry be added to the
     * <code>places</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPlaces (StatusObject.PlaceInfo elem)
    {
        requestEntryAdd(PLACES, places, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>places</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPlaces (Comparable<?> key)
    {
        requestEntryRemove(PLACES, places, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>places</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePlaces (StatusObject.PlaceInfo elem)
    {
        requestEntryUpdate(PLACES, places, elem);
    }

    /**
     * Requests that the <code>places</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPlaces (DSet<StatusObject.PlaceInfo> value)
    {
        requestAttributeChange(PLACES, value, this.places);
        DSet<StatusObject.PlaceInfo> clone = (value == null) ? null : value.clone();
        this.places = clone;
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
    public void removeFromGames (Comparable<?> key)
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
    public void setGames (DSet<StatusObject.GameInfo> value)
    {
        requestAttributeChange(GAMES, value, this.games);
        DSet<StatusObject.GameInfo> clone = (value == null) ? null : value.clone();
        this.games = clone;
    }
    // AUTO-GENERATED: METHODS END

    protected static final long PI_PUBLISH_INTERVAL = 1000L;
}
