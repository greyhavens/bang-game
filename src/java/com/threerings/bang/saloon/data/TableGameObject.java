//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.presents.dobj.DObject;

/**
 * Contains data related to configuring and starting table games.
 */
public class TableGameObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>game</code> field. */
    public static final String GAME = "game";

    /** The field name of the <code>playerOids</code> field. */
    public static final String PLAYER_OIDS = "playerOids";

    /** The field name of the <code>starting</code> field. */
    public static final String STARTING = "starting";
    // AUTO-GENERATED: FIELDS END

    /** The means by which the client makes requests to the server. */
    public TableGameMarshaller service;

    /** The table configuration. */
    public ParlorGameConfig game;

    /** Used when match-making a game. */
    public int[] playerOids;

    /** Indications that the game is about to start. */
    public boolean starting;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (TableGameMarshaller value)
    {
        TableGameMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>game</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGame (ParlorGameConfig value)
    {
        ParlorGameConfig ovalue = this.game;
        requestAttributeChange(
            GAME, value, ovalue);
        this.game = value;
    }

    /**
     * Requests that the <code>playerOids</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayerOids (int[] value)
    {
        int[] ovalue = this.playerOids;
        requestAttributeChange(
            PLAYER_OIDS, value, ovalue);
        this.playerOids = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>playerOids</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPlayerOidsAt (int value, int index)
    {
        int ovalue = this.playerOids[index];
        requestElementUpdate(
            PLAYER_OIDS, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.playerOids[index] = value;
    }

    /**
     * Requests that the <code>starting</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStarting (boolean value)
    {
        boolean ovalue = this.starting;
        requestAttributeChange(
            STARTING, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.starting = value;
    }
    // AUTO-GENERATED: METHODS END
}
