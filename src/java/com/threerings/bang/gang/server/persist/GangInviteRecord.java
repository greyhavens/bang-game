//
// $Id$

package com.threerings.bang.gang.server.persist;

import com.threerings.bang.data.Handle;

/**
 * Does something extraordinary.
 */
public class GangInviteRecord
{
    /** The name of the player extending the invitation. */
    public Handle inviter;

    /** The id of the gang to which the player has been invited. */
    public int gangId;
        
    /** The name of the gang to which the player has been invited. */
    public Handle name;
        
    /** The text of the invitation. */
    public String message;
        
    /** Creates a new invitation. */
    public GangInviteRecord (Handle inviter, int gangId, Handle name, String message)
    {
        this.inviter = inviter;
        this.gangId = gangId;
        this.name = name;
        this.message = message;
    }
}
