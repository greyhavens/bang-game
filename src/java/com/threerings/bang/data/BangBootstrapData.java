//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.net.BootstrapData;

/**
 * Augments the main bootstrap data with some Bang! additions.
 */
public class BangBootstrapData extends BootstrapData
{
    /** The oid of the server status object (only non-zero for admins). */
    public int statusOid;

    /** The oid of the town object. */
    public int townOid;

    /** The room oid of the Saloon. */
    public int saloonOid;

    /** The room oid of the General Store. */
    public int storeOid;

    /** The room oid of the Bank. */
    public int bankOid;

    /** The room oid of the Ranch. */
    public int ranchOid;

    /** The room oid of the Barber. */
    public int barberOid;

    /** The room oid of the Train Station. */
    public int stationOid;

    /** The room oid of the Hideout. */
    public int hideoutOid;

    /** The room oid of the Sheriff's office. */
    public int officeOid;

    /**
     * Returns the place oid for the shop with the specified string identifier or -1 if the
     * identifier is unknown.
     */
    public int getPlaceOid (String name)
    {
        if ("saloon".equals(name)) {
            return saloonOid;
        } else if ("store".equals(name)) {
            return storeOid;
        } else if ("bank".equals(name)) {
            return bankOid;
        } else if ("ranch".equals(name)) {
            return ranchOid;
        } else if ("barber".equals(name)) {
            return barberOid;
        } else if ("station".equals(name)) {
            return stationOid;
        } else if ("hideout".equals(name)) {
            return hideoutOid;
        } else if ("office".equals(name)) {
            return officeOid;
        } else {
            return -1;
        }
    }
}
