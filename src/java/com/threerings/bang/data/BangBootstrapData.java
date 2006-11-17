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
}
