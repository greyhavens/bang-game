//
// $Id$

package com.threerings.bang.util;

/**
 * Contains routines relating to username checking, primarily relating to
 * vulgarity in the English language.
 */
public class NameUtil
{
    /**
     * Returns true if the specified name contains any of a list of vulgar stop
     * words or matches any of a list of vulgar regular expressions.
     */
    public static boolean isVulgarName (String name)
    {
        name = name.toLowerCase();
        for (int ii = 0; ii < STOP_WORDS.length; ii++) {
            if (name.indexOf(STOP_WORDS[ii]) != -1) {
                return true;
            }
        }
        for (int ii = 0; ii < STOP_REGEXES.length; ii++) {
            if (name.matches(STOP_REGEXES[ii])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Capitalizes the first word of the supplied name, preserves the case of
     * subsequent words and lower cases all other letters.
     */
    public static String capitalizeName (String name)
    {
        StringBuilder cname = new StringBuilder();
        boolean wasSpace = false;
        for (int ii = 0, ll = name.length(); ii < ll; ii++) {
            char c = name.charAt(ii);
            if (ii == 0) {
                cname.append(Character.toUpperCase(c));
            } else if (wasSpace) {
                cname.append(c);
            } else {
                cname.append(Character.toLowerCase(c));
            }
            wasSpace = Character.isWhitespace(c);
        }
        return cname.toString();
    }

    /** Substrings that are not allowed in handles. */
    protected static final String[] STOP_WORDS = {
        "ballsac",
        "bitch",
        "boner",
        "boob",
        "breast",
        "butt",
        "chink",
        "clit",
        "cock",
        "cunnilingus",
        "cunt",
        "dick",
        "dyke",
        "ejaculate",
        "erection",
        "fag",
        "fart",
        "fellatio",
        "fuck",
        "herpes",
        "homo",
        "jizz",
        "kike",
        "lesb",
        "loin",
        "masturbator",
        "muffdiver",
        "nigg",
        "nipple",
        "orgasm",
        "orgy",
        "penis",
        "piss",
        "porchmonkey",
        "prick",
        "prostitute",
        "pubic",
        "pussy",
        "queer",
        "rape",
        "rapist",
        "rectal",
        "retard",
        "scrotum",
        "semen",
        "shit",
        "slave",
        "slut",
        "sodom",
        "spaj",
        "sperm",
        "suck",
        "syphilis",
        "tarbaby",
        "testicle",
        "titt",
        "twat",
        "vulva",
        "whore",
        "bastard",
    };

    /** Regular expressions that handles are not allowed to match. */
    protected static final String[] STOP_REGEXES = {
        "arse.+",
        ".+arse",
    };
}
