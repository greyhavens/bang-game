//
// $Id$

package com.threerings.bang.util;

import com.threerings.bang.data.Handle;

/**
 * Defines the name validation routines needed by the game.
 */
public abstract class NameValidator
{
    /**
     * Returns true if the supplied handle meets our formatting requirements.
     */
    public abstract boolean isValidHandle (Handle handle);

    /**
     * Returns true if the supplied handle matches any of our reserved words or
     * regular expressions.
     */
    public abstract boolean isReservedHandle (Handle handle);

    /**
     * Returns true if the supplied handle matches any of our stop words or
     * regular expressions.
     */
    public abstract boolean isVulgarHandle (Handle handle);

    /**
     * Creates a handle from the supplied text, properly adjusting the case
     * according to rules appropriate to the default language.
     */
    public abstract Handle makeCasedHandle (String text);

    /**
     * Returns the minimum length of a handle in characters.
     */
    public abstract int getMinHandleLength ();

    /**
     * Returns the maximum length of a handle in characters.
     */
    public abstract int getMaxHandleLength ();
    
    /**
     * Returns the (lowercase versions of the) letters that names can include. 
     */
    public abstract char[] getValidLetters ();
}
