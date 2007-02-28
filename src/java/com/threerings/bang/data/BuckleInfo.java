//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains information on a gang buckle: either a fingerprint or a custom image.
 */
public class BuckleInfo extends SimpleStreamableObject
{
    /** A buckle fingerprint. */
    public int[] print;

    /** The path to a custom buckle image. */
    public String image;

    /** A default constructor used during unserialization. */
    public BuckleInfo ()
    {
    }

    /** Creates an info record with only a fingerprint. */
    public BuckleInfo (int[] print)
    {
        this.print = print;
    }
}
