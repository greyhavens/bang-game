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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.BootstrapData.class, "connectionId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.BootstrapData.class, "clientOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.BootstrapData.class, "services", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "statusOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "townOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "saloonOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "storeOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "ranchOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "barberOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "stationOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "hideoutOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "officeOid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangBootstrapData.class, "tourniesOid", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.BootstrapData.class, "connectionId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.BootstrapData.class, "clientOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.BootstrapData.class, "services", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "statusOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "townOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "saloonOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "storeOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "ranchOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "barberOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "stationOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "hideoutOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "officeOid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangBootstrapData.class, "tourniesOid", this, out);
    }
}
