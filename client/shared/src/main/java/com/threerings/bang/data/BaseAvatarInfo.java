//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.cast.CharacterDescriptor;

import com.threerings.bang.util.BasicContext;

/**
 * Contains the data common to both avatars and gang buckles.
 */
public abstract class BaseAvatarInfo extends SimpleStreamableObject
    implements Cloneable
{
    /** A fingerprint. */
    public int[] print;

    /** The path to a custom image. */
    public String image;

    /**
     * Returns true if we have either a print, a custom image, or a default image.
     */
    public boolean isValid ()
    {
        return (print != null && print.length > 0) || !StringUtil.isBlank(getImage());
    }

    /**
     * Decodes the stored fingerprint and returns a character descriptor to use in compositing.
     */
    public abstract CharacterDescriptor decodePrint (BasicContext ctx);

    /**
     * Returns the name of the (default) action to use in the above descriptor.
     */
    public abstract String getCharacterAction ();

    /**
     * Returns the width of the composited image.
     */
    public abstract int getWidth ();

    /**
     * Returns the height of the composited image.
     */
    public abstract int getHeight ();

    /**
     * Returns the desired width of framed images.
     */
    public abstract int getFramedWidth ();

    /**
     * Returns the desired height of framed images.
     */
    public abstract int getFramedHeight ();

    /**
     * If this avatar has a half-sized custom image, returns that image's path; otherwise,
     * returns <code>null</code> if there's a fingerprint or {@link #getDefaultImage} if
     * not.
     */
    public String getImage ()
    {
        if (image != null) {
            return image;
        }
        return (print == null || print.length == 0) ? getDefaultImage() : null;
    }

    /**
     * Returns the path of an image to use when {@link #print} and {@link #image} are both
     * <code>null</code>, or <code>null</code> if there is no default image.
     */
    public String getDefaultImage ()
    {
        return null;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof BaseAvatarInfo) {
            BaseAvatarInfo oinfo = (BaseAvatarInfo)other;
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
