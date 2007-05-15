//
// $Id$

package com.threerings.bang.data;

import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.util.BasicContext;

/**
 * Contains information on a gang buckle: either a fingerprint or a custom image.
 */
public class BuckleInfo extends BaseAvatarInfo
{
    /** A default constructor used during unserialization. */
    public BuckleInfo ()
    {
    }

    /** Creates an info record with only a fingerprint. */
    public BuckleInfo (int[] print)
    {
        this.print = print;
    }

    /** Creates an info record with only an image. */
    public BuckleInfo (String image)
    {
        this.image = image;
    }

    // documentation inherited
    public CharacterDescriptor decodePrint (BasicContext ctx)
    {
        return (print == null || print.length == 0) ?
            null : ctx.getAvatarLogic().decodeBuckle(print);
    }

    // documentation inherited
    public String getCharacterAction ()
    {
        return "static";
    }

    // documentation inherited
    public int getWidth ()
    {
        return AvatarLogic.BUCKLE_WIDTH;
    }

    // documentation inherited
    public int getHeight ()
    {
        return AvatarLogic.BUCKLE_HEIGHT;
    }

    // documentation inherited
    public int getFramedWidth ()
    {
        return AvatarLogic.BUCKLE_WIDTH;
    }

    // documentation inherited
    public int getFramedHeight ()
    {
        return AvatarLogic.BUCKLE_HEIGHT;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof BuckleInfo && super.equals(other));
    }
}
