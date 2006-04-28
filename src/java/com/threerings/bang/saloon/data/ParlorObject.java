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
    /** The field name of the <code>info</code> field. */
    public static final String INFO = "info";

    /** The field name of the <code>password</code> field. */
    public static final String PASSWORD = "password";

    /** The field name of the <code>onlyCreatorStart</code> field. */
    public static final String ONLY_CREATOR_START = "onlyCreatorStart";

    /** The field name of the <code>rounds</code> field. */
    public static final String ROUNDS = "rounds";

    /** The field name of the <code>players</code> field. */
    public static final String PLAYERS = "players";

    /** The field name of the <code>tinCans</code> field. */
    public static final String TIN_CANS = "tinCans";

    /** The field name of the <code>teamSize</code> field. */
    public static final String TEAM_SIZE = "teamSize";

    /** The field name of the <code>scenarios</code> field. */
    public static final String SCENARIOS = "scenarios";
    // AUTO-GENERATED: FIELDS END

    /** The configuration of this parlor. */
    public ParlorInfo info;

    /** The password for this parlor if one is set. */
    public String password;

    /** Whether only the parlor creator can start games. */
    public boolean onlyCreatorStart;

    /** The number of rounds for the current game. */
    public int rounds;

    /** The number of players in the current game. */
    public int players;

    /** The number of tin cans in the current game. */
    public int tinCans;

    /** The team size of the current game. */
    public int teamSize;

    /** The scenarios allowed for the current game. */
    public String[] scenarios;

    // AUTO-GENERATED: METHODS START
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
     * Requests that the <code>password</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPassword (String value)
    {
        String ovalue = this.password;
        requestAttributeChange(
            PASSWORD, value, ovalue);
        this.password = value;
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
     * Requests that the <code>rounds</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRounds (int value)
    {
        int ovalue = this.rounds;
        requestAttributeChange(
            ROUNDS, new Integer(value), new Integer(ovalue));
        this.rounds = value;
    }

    /**
     * Requests that the <code>players</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayers (int value)
    {
        int ovalue = this.players;
        requestAttributeChange(
            PLAYERS, new Integer(value), new Integer(ovalue));
        this.players = value;
    }

    /**
     * Requests that the <code>tinCans</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTinCans (int value)
    {
        int ovalue = this.tinCans;
        requestAttributeChange(
            TIN_CANS, new Integer(value), new Integer(ovalue));
        this.tinCans = value;
    }

    /**
     * Requests that the <code>teamSize</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTeamSize (int value)
    {
        int ovalue = this.teamSize;
        requestAttributeChange(
            TEAM_SIZE, new Integer(value), new Integer(ovalue));
        this.teamSize = value;
    }

    /**
     * Requests that the <code>scenarios</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScenarios (String[] value)
    {
        String[] ovalue = this.scenarios;
        requestAttributeChange(
            SCENARIOS, value, ovalue);
        this.scenarios = (value == null) ? null : (String[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>scenarios</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setScenariosAt (String value, int index)
    {
        String ovalue = this.scenarios[index];
        requestElementUpdate(
            SCENARIOS, index, value, ovalue);
        this.scenarios[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
