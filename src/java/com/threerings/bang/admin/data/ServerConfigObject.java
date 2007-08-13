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

    /** The field name of the <code>bankEnabled</code> field. */
    public static final String BANK_ENABLED = "bankEnabled";

    /** The field name of the <code>barberEnabled</code> field. */
    public static final String BARBER_ENABLED = "barberEnabled";

    /** The field name of the <code>hideoutEnabled</code> field. */
    public static final String HIDEOUT_ENABLED = "hideoutEnabled";

    /** The field name of the <code>officeEnabled</code> field. */
    public static final String OFFICE_ENABLED = "officeEnabled";

    /** The field name of the <code>ranchEnabled</code> field. */
    public static final String RANCH_ENABLED = "ranchEnabled";

    /** The field name of the <code>saloonEnabled</code> field. */
    public static final String SALOON_ENABLED = "saloonEnabled";

    /** The field name of the <code>stationEnabled</code> field. */
    public static final String STATION_ENABLED = "stationEnabled";

    /** The field name of the <code>storeEnabled</code> field. */
    public static final String STORE_ENABLED = "storeEnabled";

    /** The field name of the <code>anonymousAccessEnabled</code> field. */
    public static final String ANONYMOUS_ACCESS_ENABLED = "anonymousAccessEnabled";

    /** The field name of the <code>selectPhaseTimeout</code> field. */
    public static final String SELECT_PHASE_TIMEOUT = "selectPhaseTimeout";

    /** The field name of the <code>rentMultiplier</code> field. */
    public static final String RENT_MULTIPLIER = "rentMultiplier";

    /** The field name of the <code>articleRentMultiplier</code> field. */
    public static final String ARTICLE_RENT_MULTIPLIER = "articleRentMultiplier";
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

    /** Controls activation of Bank services. */
    public boolean bankEnabled = true;

    /** Controls activation of Barber services. */
    public boolean barberEnabled = true;

    /** Controls activation of Hideout services. */
    public boolean hideoutEnabled = false;

    /** Controls activation of Sheriff's Office services. */
    public boolean officeEnabled = false;

    /** Controls activation of Ranch services. */
    public boolean ranchEnabled = true;

    /** Controls activation of Saloon services. */
    public boolean saloonEnabled = true;

    /** Controls activation of Train Station services. */
    public boolean stationEnabled = true;

    /** Controls activation of General Store services. */
    public boolean storeEnabled = true;

    /** Controls access of anonymous accounts. */
    public boolean anonymousAccessEnabled = false;

    /** The select phase timeout in seconds. */
    public int selectPhaseTimeout = 180;

    /** The rent multipliers by gang weight class. */
    public float[] rentMultiplier = { 5f, 12f, 23f, 45f, 110f };

    /** The article rent multipliers by gang weight class. */
    public float[] articleRentMultiplier = { 2.5f, 6f, 11.5f, 22.5f, 55f };

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

    /**
     * Requests that the <code>bankEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBankEnabled (boolean value)
    {
        boolean ovalue = this.bankEnabled;
        requestAttributeChange(
            BANK_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.bankEnabled = value;
    }

    /**
     * Requests that the <code>barberEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBarberEnabled (boolean value)
    {
        boolean ovalue = this.barberEnabled;
        requestAttributeChange(
            BARBER_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.barberEnabled = value;
    }

    /**
     * Requests that the <code>hideoutEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHideoutEnabled (boolean value)
    {
        boolean ovalue = this.hideoutEnabled;
        requestAttributeChange(
            HIDEOUT_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.hideoutEnabled = value;
    }

    /**
     * Requests that the <code>officeEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setOfficeEnabled (boolean value)
    {
        boolean ovalue = this.officeEnabled;
        requestAttributeChange(
            OFFICE_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.officeEnabled = value;
    }

    /**
     * Requests that the <code>ranchEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRanchEnabled (boolean value)
    {
        boolean ovalue = this.ranchEnabled;
        requestAttributeChange(
            RANCH_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.ranchEnabled = value;
    }

    /**
     * Requests that the <code>saloonEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSaloonEnabled (boolean value)
    {
        boolean ovalue = this.saloonEnabled;
        requestAttributeChange(
            SALOON_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.saloonEnabled = value;
    }

    /**
     * Requests that the <code>stationEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStationEnabled (boolean value)
    {
        boolean ovalue = this.stationEnabled;
        requestAttributeChange(
            STATION_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.stationEnabled = value;
    }

    /**
     * Requests that the <code>storeEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStoreEnabled (boolean value)
    {
        boolean ovalue = this.storeEnabled;
        requestAttributeChange(
            STORE_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.storeEnabled = value;
    }

    /**
     * Requests that the <code>anonymousAccessEnabled</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAnonymousAccessEnabled (boolean value)
    {
        boolean ovalue = this.anonymousAccessEnabled;
        requestAttributeChange(
            ANONYMOUS_ACCESS_ENABLED, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.anonymousAccessEnabled = value;
    }

    /**
     * Requests that the <code>selectPhaseTimeout</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSelectPhaseTimeout (int value)
    {
        int ovalue = this.selectPhaseTimeout;
        requestAttributeChange(
            SELECT_PHASE_TIMEOUT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.selectPhaseTimeout = value;
    }

    /**
     * Requests that the <code>rentMultiplier</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRentMultiplier (float[] value)
    {
        float[] ovalue = this.rentMultiplier;
        requestAttributeChange(
            RENT_MULTIPLIER, value, ovalue);
        this.rentMultiplier = (value == null) ? null : (float[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>rentMultiplier</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setRentMultiplierAt (float value, int index)
    {
        float ovalue = this.rentMultiplier[index];
        requestElementUpdate(
            RENT_MULTIPLIER, index, Float.valueOf(value), Float.valueOf(ovalue));
        this.rentMultiplier[index] = value;
    }

    /**
     * Requests that the <code>articleRentMultiplier</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setArticleRentMultiplier (float[] value)
    {
        float[] ovalue = this.articleRentMultiplier;
        requestAttributeChange(
            ARTICLE_RENT_MULTIPLIER, value, ovalue);
        this.articleRentMultiplier = (value == null) ? null : (float[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>articleRentMultiplier</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setArticleRentMultiplierAt (float value, int index)
    {
        float ovalue = this.articleRentMultiplier[index];
        requestElementUpdate(
            ARTICLE_RENT_MULTIPLIER, index, Float.valueOf(value), Float.valueOf(ovalue));
        this.articleRentMultiplier[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
