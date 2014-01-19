//
// $Id$

package com.threerings.bang.game.util;

import java.util.ArrayList;
import java.util.Iterator;

import java.nio.ByteBuffer;

/**
 * A set of utility functions for generating and working with array diffs.
 */
public class ArrayDiffUtil
{
    /**
     * Creates a patch that if applied to one array would create the other.
     * <code>src</code> and <code>mod</code> must be the same length.  The
     * diff is stored as a series of records in a byte buffer of the form:
     * <code>offset</code>: Offset into the array
     * <code>length</code>: Length of the diff data
     * <code>data</code>: Length bytes of the XORd of src and mod
     */
    public static byte[] createPatch (byte[] src, byte[] mod)
    {
        if (src.length != mod.length) {
            return null;
        }
        
        // determine the size of the diff
        int values = 0;
        int groups = 0;
        boolean same = true;
        int length = 0;
        ArrayList<Integer> lengths = new ArrayList<Integer>();

        for (int ii = 0; ii < src.length; ii++) {
            if (src[ii] != mod[ii]) {
                if (same) {
                    length = 0;
                    groups++;
                    same = false;
                }
                values++;
                length++;
            } else if (!same) {
                same = true;
                lengths.add(length);
            }
        }

        // generate the diff
        byte[] diff = new byte[values + groups * 8];
        ByteBuffer buf = ByteBuffer.wrap(diff);
        same = true;
        Iterator<Integer> iter = lengths.iterator();
        for (int ii = 0; ii < src.length; ii++) {
            if (src[ii] != mod[ii]) {
                if (same) {
                    same = false;
                    buf.putInt(ii);
                    buf.putInt(iter.next());
                }
                buf.put((byte)(src[ii] ^ mod[ii]));
            } else {
                same = true;
            }
        }
        return diff;    
    }

    /**
     * Applies a patch to the supplied array, overwriting the contents.
     */
    public static void applyPatch (byte[] src, byte[] patch)
    {
        ByteBuffer buf = ByteBuffer.wrap(patch);
        while (buf.hasRemaining()) {
            int offset = buf.getInt();
            int length = buf.getInt();
            for (int ii = offset; ii < offset + length; ii++) {
                src[ii] = (byte)(src[ii] ^ buf.get());
            }
        }
    }
}
