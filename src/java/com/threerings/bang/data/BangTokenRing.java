//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
public class BangTokenRing extends TokenRing
{
    /** Indicates that the user is an "insider" and may get access to pre-release stuff. */
    public static final int INSIDER = (1 << 1);

    /** Indicates that the user is support personel. */
    public static final int SUPPORT = (1 << 2);

    /** Indicates that the user is a demo account. */
    public static final int DEMO = (1 << 3);

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
     * Convenience function for checking whether this ring holds the {@link #INSIDER} token.
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

    /**
     * Convenience function for checking whether this ring holds the {@link #DEMO} token.
     */
    public boolean isDemo ()
    {
        return holdsToken(DEMO);
    }
}
