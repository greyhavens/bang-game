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
    // AUTO-GENERATED: FIELDS END

    /** Provides access to parlor services. */
    public ParlorMarshaller service;

    /** The configuration of this parlor. */
    public ParlorInfo info;

    /** Whether only the parlor creator can start games. */
    public boolean onlyCreatorStart;

    /** Information on a game being configured. */
    public ParlorGameConfig game;

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
            ONLY_CREATOR_START, new Boolean(value), new Boolean(ovalue));
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
    // AUTO-GENERATED: METHODS END
}
