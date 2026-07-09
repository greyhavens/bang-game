//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.Handle;

/**
 * Contains the contents of a gang's info page.
 */
public class GangInfo extends SimpleStreamableObject
{
    /** Contains information on a single gang member. */
    public static class Member extends SimpleStreamableObject
    {
        /** The member's name. */
        public Handle handle;

        /** Whether or not the member is currently active. */
        public boolean active;

        public Member (Handle handle, boolean active)
        {
            this.handle = handle;
            this.active = active;
        }

        public Member ()
        {
        }

        // from interface Streamable
        public void readObject (com.threerings.io.ObjectInputStream ins)
            throws java.io.IOException, java.lang.ClassNotFoundException
        {
            com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.Member.class, "handle", this, ins);
            com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.Member.class, "active", this, ins);
        }

        // from interface Streamable
        public void writeObject (com.threerings.io.ObjectOutputStream out)
            throws java.io.IOException
        {
            com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.Member.class, "handle", this, out);
            com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.Member.class, "active", this, out);
        }
    }

    /** The name of the gang. */
    public Handle name;

    /** The day on which this gang was founded. */
    public long founded;

    /** The gang's weight class. */
    public byte weightClass;

    /** The gang's notoriety level. */
    public byte notoriety;

    /** The gang's statement. */
    public String statement;

    /** The gang's URL. */
    public String url;

    /** The gang's buckle. */
    public BuckleInfo buckle;

    /** The gang leader's avatar. */
    public AvatarInfo avatar;

    /** The presorted leaders of the gang. */
    public Member[] leaders;

    /** The presorted normal members of the gang. */
    public Member[] members;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "name", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "founded", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "weightClass", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "notoriety", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "statement", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "url", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "buckle", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "avatar", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "leaders", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangInfo.class, "members", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "name", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "founded", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "weightClass", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "notoriety", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "statement", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "url", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "buckle", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "avatar", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "leaders", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangInfo.class, "members", this, out);
    }
}
