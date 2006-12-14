//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
public class BangTokenRing extends TokenRing
{
    /** TEMP: a flag to limit music only to ourselves and the SomaTone guys. */
    public static final int INSIDER = (1 << 1);

    /** Indicates that the user is support personel. */
    public static final int SUPPORT = (1 << 2);

    /**
     * A default constructor, used when unserializing token rings.
     */
    public BangTokenRing ()
    {
    }

    /**
     * Constructs a token ring with the supplied set of tokens.
     */
    public BangTokenRing (int tokens)
    {
        super(tokens);
    }

    /**
     * TEMP: Convenience function for checking whether this ring holds the {@link #INSIDER} token.
     */
    public boolean isInsider ()
    {
        return holdsToken(INSIDER);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #SUPPORT} token.
     */
    public boolean isSupport ()
    {
        return holdsToken(SUPPORT);
    }
}
