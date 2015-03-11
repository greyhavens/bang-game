//
// $Id$

package com.threerings.bang.util;

import com.threerings.bang.data.Handle;

/**
 * Handles the validation of names for the English language.
 */
public class EnglishNameValidator extends NameValidator
{
    @Override // documentation inherited
    public boolean isValidHandle (Handle handle)
    {
        String hstr = (handle != null) ? handle.toString() : "";
        return !(hstr.length() < getMinHandleLength() ||
                 hstr.length() > getMaxHandleLength() ||
                 !isValidCharacters(hstr) ||
                 !hstr.replaceAll(" +", " ").equals(hstr));
    }

    @Override // documentation inherited
    public boolean isReservedHandle (Handle handle)
    {
        String name = (handle != null) ? handle.toString() : "";
        name = name.replaceAll(" ", "");
        name = name.toLowerCase();
        for (int ii = 0; ii < RESERVED_WORDS.length; ii++) {
            if (name.indexOf(RESERVED_WORDS[ii]) != -1) {
                return true;
            }
        }
        for (int ii = 0; ii < RESERVED_REGEXES.length; ii++) {
            if (name.matches(RESERVED_REGEXES[ii])) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean isVulgarHandle (Handle handle)
    {
        String hstr = (handle != null) ? handle.toString() : "";
        return NameUtil.isVulgarName(hstr);
    }

    @Override // documentation inherited
    public Handle makeCasedHandle (String handle)
    {
        // remove excess spaces
        handle = handle.replaceAll(" +", " ");
        return new Handle(NameUtil.capitalizeName(handle));
    }

    @Override // documentation inherited
    public int getMinHandleLength ()
    {
        return MIN_HANDLE_CHARS;
    }

    @Override // documentation inherited
    public int getMaxHandleLength ()
    {
        return MAX_HANDLE_CHARS;
    }

    @Override // documentation inherited
    public char[] getValidLetters ()
    {
        return VALID_LETTERS;
    }
    
    /** Determines whether the supplied string is all characters (ie. no
     * numbers or punctuation) or spaces. */
    protected boolean isValidCharacters (String text)
    {
        for (int ii = 0, ll = text.length(); ii < ll; ii++) {
            char c = text.charAt(ii);
            if (c != ' ' && !Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    /** The minimum number of characters for a player handle. */
    protected static final int MIN_HANDLE_CHARS = 4;

    /** The maximum number of characters for a player handle. */
    protected static final int MAX_HANDLE_CHARS = 18;

    // we're most likely going to call our support personel sheriffs or
    // deputies, so don't allow anyone to use those in a name

    /** Substrings that are not allowed in handles. */
    protected static final String[] RESERVED_WORDS = {
        "deput",
    };

    /** Regular expressions that handles are not allowed to match. */
    protected static final String[] RESERVED_REGEXES = {
        ".*sh[e][r]+[il][f]+.*",
    };
    
    /** The letters that we allow in names. */
    protected static final char[] VALID_LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
}
