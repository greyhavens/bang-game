//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

/**
 * Extends the {@link BodyObject} with custom bits needed by Bang!.
 */
public class BangUserObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";
    // AUTO-GENERATED: FIELDS END

    /** Indicates which access control tokens are held by this user. */
    public TokenRing tokens;

    @Override // documentation inherited
    public TokenRing getTokens ()
    {
        return tokens;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (TokenRing value)
    {
        TokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }
    // AUTO-GENERATED: METHODS END
}
