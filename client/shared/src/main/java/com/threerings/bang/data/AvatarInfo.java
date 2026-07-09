//
// $Id$

package com.threerings.bang.data;

import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.util.BasicContext;

/**
 * Contains information on an avatar: either a fingerprint or a custom image.
 */
public class AvatarInfo extends BaseAvatarInfo
{
    /** A default constructor used during unserialization. */
    public AvatarInfo ()
    {
    }

    /** Creates an info record with only a fingerprint. */
    public AvatarInfo (int[] print)
    {
        this.print = print;
    }

    // documentation inherited
    public CharacterDescriptor decodePrint (BasicContext ctx)
    {
        return (print == null || print.length == 0) ?
            null : ctx.getAvatarLogic().decodeAvatar(print);
    }

    // documentation inherited
    public String getCharacterAction ()
    {
        return "default";
    }

    // documentation inherited
    public int getWidth ()
    {
        return AvatarLogic.WIDTH;
    }

    // documentation inherited
    public int getHeight ()
    {
        return AvatarLogic.HEIGHT;
    }

    // documentation inherited
    public int getFramedWidth ()
    {
        return AvatarLogic.FRAMED_WIDTH;
    }

    // documentation inherited
    public int getFramedHeight ()
    {
        return AvatarLogic.FRAMED_HEIGHT;
    }

    @Override // documentation inherited
    public String getDefaultImage ()
    {
        return "ui/status/silhouette.png";
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof AvatarInfo && super.equals(other));
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BaseAvatarInfo.class, "print", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BaseAvatarInfo.class, "image", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BaseAvatarInfo.class, "print", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BaseAvatarInfo.class, "image", this, out);
    }
}
