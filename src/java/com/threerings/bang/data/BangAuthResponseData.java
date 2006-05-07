//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.net.AuthResponseData;

/**
 * Extends the normal auth response data with Bang-specific bits.
 */
public class BangAuthResponseData extends AuthResponseData
{
    /** A machine identifier to be assigned to this machine. */
    public String ident;
}
