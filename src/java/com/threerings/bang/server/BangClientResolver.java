//
// $Id$

package com.threerings.bang.server;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.bang.data.BangUserObject;

/**
 * Customizes the client resolver to use our {@link BangUserObject}.
 */
public class BangClientResolver extends CrowdClientResolver
{
    // documentation inherited
    public Class getClientObjectClass ()
    {
        return BangUserObject.class;
    }
}
