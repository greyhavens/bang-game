//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.PresentsObjectAccess;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.data.GangObject;

/**
 * Restricts subscription to a {@link GangObject} to that gang's own members.
 *
 * <p>A GangObject is a plain distributed object (not a crowd "place"), so it never receives a
 * {@link com.threerings.crowd.server.CrowdObjectAccess.PlaceAccessController} and would otherwise
 * fall back to the permissive default controller — letting any client subscribe by guessing its
 * (small-int) oid and read the gang's roster, coffers and inventory. Here a real game client is
 * gated to its own gang; server-internal and peer/proxy subscriptions (the cross-node gang
 * replication) are left untouched.
 */
public class GangObjectAccess implements AccessController
{
    /** Shared stateless instance. */
    public static final GangObjectAccess INSTANCE = new GangObjectAccess();

    // from interface AccessController
    public boolean allowSubscribe (DObject object, Subscriber<?> sub)
    {
        // real game clients arrive as a ProxySubscriber wrapping their PlayerObject; peer/server
        // subscriptions (cross-node replication) are plain Subscribers and are always allowed
        if (sub instanceof ProxySubscriber) {
            ClientObject co = ((ProxySubscriber)sub).getClientObject();
            if (co instanceof PlayerObject) {
                return ((PlayerObject)co).gangId == ((GangObject)object).gangId;
            }
        }
        return true;
    }

    // from interface AccessController
    public boolean allowDispatch (DObject object, DEvent event)
    {
        // unchanged from the default: clients may only post message / invocation-request events
        return PresentsObjectAccess.DEFAULT.allowDispatch(object, event);
    }
}
