//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.net.BootstrapData;

/**
 * Augments the main bootstrap data with some Bang! additions.
 */
public class BangBootstrapData extends BootstrapData
{
    /** The room oid of the Saloon. */
    public int saloonOid;

    /** The room oid of the General Store. */
    public int storeOid;

    /** The room oid of the Bank. */
    public int bankOid;

    /** The room oid of the Ranch. */
    public int ranchOid;
}
