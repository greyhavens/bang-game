//
// $Id$

package com.threerings.bang.util;

import java.util.HashSet;

/**
 * Defines the set of name creation routines that are needed by the game.
 */
public abstract class NameCreator
{
    /** Returns a set of prefixes that can be prepended to a root to make an AI
     * name. For example: "Robo" Pete, "Tick-tock" Bart, "Clanky" Bob, etc. */
    public abstract HashSet<String> getAIPrefixes (boolean isMale);

    /** Returns a set of prefixes that can be prepended to a root to make a
     * Big Shot name. For example: "Handy" Jack, "Faithful" Alfred, etc. */
    public abstract HashSet<String> getBigShotPrefixes (boolean isMale);
    
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
    
    /** Returns a set of suffixes that can be appended to a root to make a
     * gang name. For example: The Dalton "Gang", The Hatfield "Clan", etc. */
    public abstract HashSet<String> getGangSuffixes ();
}
