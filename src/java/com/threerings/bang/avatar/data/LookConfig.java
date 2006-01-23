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
}
