//
// $Id$

package com.threerings.bang.util;

import java.util.HashSet;

/**
 * Defines the set of name creation routines that are needed by the game.
 */
public abstract class NameCreator
{
    /** Returns a set of prefixes that can be prepended to a root to make a
     * cowboy name. For example: "Wild" Pete, "Mean" Bart, "Oklahoma" Bob,
     * etc. */
    public abstract HashSet<String> getHandlePrefixes (boolean isMale);

    /** Returns a set of root names, generally proper first names. For example
     * "Pete", "Bart", "Bob". */
    public abstract HashSet<String> getHandleRoots (boolean isMale);

    /** Returns a set of suffixes that can be appended to a root to make a
     * cowboy name. For example: Billy "the Kid", Paul "Regret", etc. */
    public abstract HashSet<String> getHandleSuffixes (boolean isMale);
}
