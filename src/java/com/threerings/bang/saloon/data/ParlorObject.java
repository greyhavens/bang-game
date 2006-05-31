//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.Handle;

/**
 * Contains information shared among all occupants of a back parlor room.
 */
public class ParlorObject extends PlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>info</code> field. */
    public static final String INFO = "info";

    /** The field name of the <code>onlyCreatorStart</code> field. */
    public static final String ONLY_CREATOR_START = "onlyCreatorStart";

    /** The field name of the <code>game</code> field. */
    public static final String GAME = "game";

    /** The field name of the <code>playerOids</code> field. */
    public static final String PLAYER_OIDS = "playerOids";

    /** The field name of the <code>starting</code> field. */
    public static final String STARTING = "starting";
    // AUTO-GENERATED: FIELDS END

    /** Provides access to parlor services. */
    public ParlorMarshaller service;

    /** The configuration of this parlor. */
    public ParlorInfo info;

    /** Whether only the parlor creator can start games. */
    public boolean onlyCreatorStart;

    /** Information on a game being configured. */
    public ParlorGameConfig game;

    /** Used when match-making a game. */
    public int[] playerOids;

    /** Indicates that the game is about to start. */
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
    public void setService (ParlorMarshaller value)
    {
        ParlorMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>info</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setInfo (ParlorInfo value)
    {
        ParlorInfo ovalue = this.info;
        requestAttributeChange(
            INFO, value, ovalue);
        this.info = value;
    }

    /**
     * Requests that the <code>onlyCreatorStart</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setOnlyCreatorStart (boolean value)
    {
        boolean ovalue = this.onlyCreatorStart;
        requestAttributeChange(
            ONLY_CREATOR_START, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.onlyCreatorStart = value;
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
        this.playerOids = (value == null) ? null : (int[])value.clone();
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
