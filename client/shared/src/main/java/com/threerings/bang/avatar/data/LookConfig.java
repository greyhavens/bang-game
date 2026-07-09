//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains all the information needed to configure a new look.
 */
public class LookConfig extends SimpleStreamableObject
{
    /** The name of the new look. */
    public String name;

    /** The global hair colorization id. */
    public int hair;

    /** The global skin colorization id. */
    public int skin;

    /** The aspect selections. */
    public String[] aspects;

    /** The aspect colorizations (already properly encoded). */
    public int[] colors;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.avatar.data.LookConfig.class, "name", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.avatar.data.LookConfig.class, "hair", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.avatar.data.LookConfig.class, "skin", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.avatar.data.LookConfig.class, "aspects", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.avatar.data.LookConfig.class, "colors", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.avatar.data.LookConfig.class, "name", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.avatar.data.LookConfig.class, "hair", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.avatar.data.LookConfig.class, "skin", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.avatar.data.LookConfig.class, "aspects", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.avatar.data.LookConfig.class, "colors", this, out);
    }
}
