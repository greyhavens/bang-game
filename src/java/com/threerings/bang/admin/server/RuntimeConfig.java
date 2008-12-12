//
// $Id$

package com.threerings.bang.admin.server;

import java.lang.reflect.Field;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.admin.server.ConfigRegistry;

import com.threerings.bang.admin.data.ServerConfigObject;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangSession;
import com.threerings.bang.server.BangServer;

import static com.threerings.bang.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public static ServerConfigObject server = new ServerConfigObject();

    /**
     * Creates and registers the runtime configuration objects.
     */
    public static void init (RootDObjectManager omgr, ConfigRegistry confreg)
    {
        Field[] fields = RuntimeConfig.class.getDeclaredFields();
        for (int ii = 0; ii < fields.length; ii++) {
            final Field field = fields[ii];
            final Class<?> oclass = field.getType();
            if (!DObject.class.isAssignableFrom(oclass)) {
                continue;
            }

            String key = field.getName();
            try {
                // create and register the object
                DObject object = omgr.registerObject((DObject)field.get(null));

                // set the tight-ass access controller
                object.setAccessController(ADMIN_CONTROLLER);

                // register the object with the config object registry
                confreg.registerObject(key, key, object);

                // and set our static field
                field.set(null, object);

            } catch (Exception e) {
                log.warning("Failed to set " + key + ": " + e);
            }
        }
    }

    /** An access controller that provides stricter-than-normal access to these
     * configuration objects. */
    protected static AccessController ADMIN_CONTROLLER = new AccessController()
    {
        public boolean allowSubscribe (DObject object, Subscriber<?> subscriber) {
            // if the subscriber is a client; make sure they're an admin
            if (BangSession.class.isInstance(subscriber)) {
                PlayerObject user = (PlayerObject)
                    BangSession.class.cast(subscriber).getClientObject();
                return user.tokens.isAdmin();
            }
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event) {
            // look up the user object of the event originator
            int sourceOid = event.getSourceOid();
            if (sourceOid == -1) {
                return true; // server: ok
            }

            // make sure the originator is an admin
            DObject obj = BangServer.omgr.getObject(sourceOid);
            if (!(obj instanceof PlayerObject)) {
                return false;
            }
            PlayerObject user = (PlayerObject)obj;
            if (!user.tokens.isAdmin()) {
                return false;
            }

            // admins are allowed to change things, but let's log it
            BangServer.generalLog(
                "admin_config changed " + user.username + " " +
                object.getClass().getName() + " " + event);
            return true;
        }
    };
}
