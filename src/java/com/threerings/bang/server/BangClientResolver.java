//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;

import com.threerings.crowd.server.CrowdClientResolver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.Item;

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

    // documentation inherited
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);
        BangUserObject buser = (BangUserObject)clobj;

        // load up this player's items
        ArrayList<Item> items = BangServer.itemrepo.loadItems(buser.userId);
        if (items != null) {
            buser.inventory = new DSet(items.iterator());
        } else {
            buser.inventory = new DSet();
        }
    }
}
