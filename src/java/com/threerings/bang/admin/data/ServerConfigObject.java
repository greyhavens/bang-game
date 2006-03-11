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
    // AUTO-GENERATED: FIELDS END

    /** Whether or not to require insiders or testers. */
    public boolean openToPublic = true;

    /** Whether or not to allow non-admins to log on. */
    public boolean nonAdminsAllowed = true;

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
            OPEN_TO_PUBLIC, new Boolean(value), new Boolean(ovalue));
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
            NON_ADMINS_ALLOWED, new Boolean(value), new Boolean(ovalue));
        this.nonAdminsAllowed = value;
    }
    // AUTO-GENERATED: METHODS END
}
