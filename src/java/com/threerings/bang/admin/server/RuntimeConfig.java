//
// $Id$

package com.threerings.bang.admin.server;

import java.lang.reflect.Field;
import java.util.logging.Level;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.bang.admin.data.ServerConfigObject;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangClient;
import com.threerings.bang.server.BangServer;

import static com.threerings.bang.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public static ServerConfigObject server;

    /**
     * Creates and registers the runtime configuration objects.
     */
    public static void init (DObjectManager omgr)
    {
        Field[] fields = RuntimeConfig.class.getDeclaredFields();
        for (int ii = 0; ii < fields.length; ii++) {
            final Field field = fields[ii];
            final Class<?> oclass = field.getType();
            if (!DObject.class.isAssignableFrom(oclass)) {
                continue;
            }

            omgr.createObject(
                (Class<DObject>)oclass, new Subscriber<DObject>() {
                public void objectAvailable (DObject object) {
                    // set the tight-ass access controller
                    object.setAccessController(ADMIN_CONTROLLER);
                    // register the object with the config object registry
                    String key = field.getName();
                    BangServer.confreg.registerObject(key, key, object);
                    try {
                        // and set our static field
                        field.set(null, object);
                    } catch (IllegalAccessException iae) {
                        log.warning("Failed to set " + key + ": " + iae);
                    }
                }
                public void requestFailed (int oid, ObjectAccessException oae) {
                    log.warning("Unable to create " + oclass + ": " + oae);
                }
            });
        }
    }

    /** An access controller that provides stricter-than-normal access to these
     * configuration objects. */
    protected static AccessController ADMIN_CONTROLLER = new AccessController()
    {
        public boolean allowSubscribe (DObject object, Subscriber subscriber) {
            // if the subscriber is a client; make sure they're an admin
            if (subscriber instanceof BangClient) {
                PlayerObject user = (PlayerObject)
                    ((BangClient)subscriber).getClientObject();
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
