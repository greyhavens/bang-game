//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.presents.dobj.DObject;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;

/**
 * Contains the shared state for a pending matched game.
 */
public class MatchObject extends DObject
    implements SpeakObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>speakService</code> field. */
    public static final String SPEAK_SERVICE = "speakService";

    /** The field name of the <code>playerOids</code> field. */
    public static final String PLAYER_OIDS = "playerOids";

    /** The field name of the <code>criterion</code> field. */
    public static final String CRITERION = "criterion";

    /** The field name of the <code>starting</code> field. */
    public static final String STARTING = "starting";
    // AUTO-GENERATED: FIELDS END

    /** Used for chatting among the matchmakees. */
    public SpeakMarshaller speakService;

    /** The player object ids of the players in this game. */
    public int[] playerOids;

    /** The current criterion for matching. */
    public Criterion criterion;

    /** Set to true if this match is about to start. */
    public boolean starting;

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
            PLAYER_OIDS, index, new Integer(value), new Integer(ovalue));
        this.playerOids[index] = value;
    }

    /**
     * Requests that the <code>criterion</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setCriterion (Criterion value)
    {
        Criterion ovalue = this.criterion;
        requestAttributeChange(
            CRITERION, value, ovalue);
        this.criterion = value;
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
            STARTING, new Boolean(value), new Boolean(ovalue));
        this.starting = value;
    }
    // AUTO-GENERATED: METHODS END

    // documentation inherited from interface SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        // nothing doing
    }
}
