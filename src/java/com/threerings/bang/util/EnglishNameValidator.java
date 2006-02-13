//
// $Id$

package com.threerings.bang.util;

import com.threerings.util.NameUtil;

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
}
