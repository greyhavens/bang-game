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

    /** The oid for the list of active/pending tournies. */
    public int tourniesOid;

    /**
     * Returns the place oid for the specified shop.
     */
    public int getPlaceOid (Shop shop)
    {
        switch (shop) {
        case BARBER: return barberOid;
        case HIDEOUT: return hideoutOid;
        case OFFICE: return officeOid;
        case RANCH: return ranchOid;
        case SALOON: return saloonOid;
        case STATION: return stationOid;
        case STORE: return storeOid;
        default: throw new IllegalArgumentException("ZOUNDS! I know not this " + shop + ".");
        }
    }
}
