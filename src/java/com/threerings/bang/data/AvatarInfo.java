//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains information on an avatar: either a fingerprint or a custom image.
 */
public class AvatarInfo extends SimpleStreamableObject
    implements Cloneable
{
    /** An avatar fingerprint. */
    public int[] print;

    /** The path to a custom avatar image. */
    public String image;

    /** A default constructor used during unserialization. */
    public AvatarInfo ()
    {
    }

    /** Creates an info record with only a fingerprint. */
    public AvatarInfo (int[] print)
    {
        this.print = print;
    }

    /**
     * Returns true if we have either a print or a custom image.
     */
    public boolean isValid ()
    {
        return (print != null && print.length > 0) || !StringUtil.isBlank(image);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof AvatarInfo) {
            AvatarInfo oinfo = (AvatarInfo)other;
            return Arrays.equals(print, oinfo.print) && ObjectUtil.equals(image, oinfo.image);
        } else {
            return false;
        }
    }

    @Override // from Object
    public int hashCode ()
    {
        int hashCode = 0;
        if (print != null) {
            for (int value : print) {
                hashCode ^= value;
            }
        }
        if (image != null) {
            hashCode ^= image.hashCode();
        }
        return hashCode;
    }

    @Override // from Object
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
}
