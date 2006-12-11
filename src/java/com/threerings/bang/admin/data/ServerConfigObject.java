//
// $Id$

package com.threerings.bang.admin.data;

/**
 * Contains runtime configurable general server configuration.
 */
public class ServerConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>openToPublic</code> field. */
    public static final String OPEN_TO_PUBLIC = "openToPublic";

    /** The field name of the <code>nonAdminsAllowed</code> field. */
    public static final String NON_ADMINS_ALLOWED = "nonAdminsAllowed";

    /** The field name of the <code>allowNewGames</code> field. */
    public static final String ALLOW_NEW_GAMES = "allowNewGames";

    /** The field name of the <code>freeIndianPost</code> field. */
    public static final String FREE_INDIAN_POST = "freeIndianPost";

    /** The field name of the <code>nearRankRange</code> field. */
    public static final String NEAR_RANK_RANGE = "nearRankRange";

    /** The field name of the <code>looseRankRange</code> field. */
    public static final String LOOSE_RANK_RANGE = "looseRankRange";
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to require insiders or testers. */
    public boolean openToPublic = true;

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

    /** Whether or not new games can be started. */
    public boolean allowNewGames = true;

    /** Whether or not to allow free travel to Indian Trading Post. */
    public boolean freeIndianPost = false;

    /** The +/- rating range for "near my rank" matched games. */
    public int nearRankRange = 200;

    /** The +/- raing range for "looser match" matched games. */
    public int looseRankRange = 400;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>openToPublic</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setOpenToPublic (boolean value)
    {
        boolean ovalue = this.openToPublic;
        requestAttributeChange(
            OPEN_TO_PUBLIC, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.openToPublic = value;
    }

    /**
     * Requests that the <code>nonAdminsAllowed</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNonAdminsAllowed (boolean value)
    {
        boolean ovalue = this.nonAdminsAllowed;
        requestAttributeChange(
            NON_ADMINS_ALLOWED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.nonAdminsAllowed = value;
    }

    /**
     * Requests that the <code>allowNewGames</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAllowNewGames (boolean value)
    {
        boolean ovalue = this.allowNewGames;
        requestAttributeChange(
            ALLOW_NEW_GAMES, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.allowNewGames = value;
    }

    /**
     * Requests that the <code>freeIndianPost</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFreeIndianPost (boolean value)
    {
        boolean ovalue = this.freeIndianPost;
        requestAttributeChange(
            FREE_INDIAN_POST, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.freeIndianPost = value;
    }

    /**
     * Requests that the <code>nearRankRange</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNearRankRange (int value)
    {
        int ovalue = this.nearRankRange;
        requestAttributeChange(
            NEAR_RANK_RANGE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.nearRankRange = value;
    }

    /**
     * Requests that the <code>looseRankRange</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLooseRankRange (int value)
    {
        int ovalue = this.looseRankRange;
        requestAttributeChange(
            LOOSE_RANK_RANGE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.looseRankRange = value;
    }
    // AUTO-GENERATED: METHODS END
}
