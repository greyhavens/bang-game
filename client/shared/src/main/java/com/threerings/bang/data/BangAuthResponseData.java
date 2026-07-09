//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.net.AuthResponseData;

/**
 * Extends the normal auth response data with Bang-specific bits.
 */
public class BangAuthResponseData extends AuthResponseData
{
    /** A machine identifier to be assigned to this machine. */
    public String ident;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.dobj.DObject.class, "_oid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.AuthResponseData.class, "code", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangAuthResponseData.class, "ident", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.dobj.DObject.class, "_oid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.AuthResponseData.class, "code", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangAuthResponseData.class, "ident", this, out);
    }
}
