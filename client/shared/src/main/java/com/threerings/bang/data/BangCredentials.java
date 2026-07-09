//
// $Id$

package com.threerings.bang.data;

import com.samskivert.servlet.user.Password;

import com.threerings.presents.net.UsernamePasswordCreds;
import com.threerings.util.Name;

/**
 * Contains extra information used during authentication with the game server.
 */
public class BangCredentials extends UsernamePasswordCreds
{
    /** The machine identifier of the client, if one is known. */
    public String ident;

    /** If the client is asking for anonymous access. */
    public boolean anonymous;

    /** The affiliate for anonymous access users. */
    public String affiliate;

    /**
     * Creates credentials with the specified username and password.
     * {@link #ident} should be set before logging in.
     */
    public BangCredentials (Name username, Password password)
    {
        super(username, password == null ? "" : password.getEncrypted());
        anonymous = password == null;
    }

    /**
     * Creates a blank instance for unserialization.
     */
    public BangCredentials ()
    {
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", ident=").append(ident);
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.UsernamePasswordCreds.class, "_username", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.net.UsernamePasswordCreds.class, "_password", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangCredentials.class, "ident", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangCredentials.class, "anonymous", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangCredentials.class, "affiliate", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.UsernamePasswordCreds.class, "_username", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.net.UsernamePasswordCreds.class, "_password", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangCredentials.class, "ident", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangCredentials.class, "anonymous", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangCredentials.class, "affiliate", this, out);
    }
}
