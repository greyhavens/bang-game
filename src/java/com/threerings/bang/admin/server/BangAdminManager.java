//
// $Id$

package com.threerings.bang.admin.server;

import com.samskivert.util.Interval;
import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.RebootManager;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.admin.data.BangAdminMarshaller;
import com.threerings.bang.admin.data.StatusObject;

import static com.threerings.bang.Log.log;

/**
 * Handles administrative bits for a Bang! server.
 */
public class BangAdminManager
    implements BangAdminProvider
{
    /** Contains server status information published to admins. */
    public StatusObject statobj;

    /**
     * Prepares the admin manager for operation.
     */
    public void init (BangServer server)
    {
        _server = server;
        _rebmgr = new BangRebootManager(server);

        // create and configure our status object
        statobj = BangServer.omgr.registerObject(new StatusObject());
        statobj.serverStartTime = System.currentTimeMillis();
        statobj.setService((BangAdminMarshaller)
                           BangServer.invmgr.registerDispatcher(new BangAdminDispatcher(this)));

        // start up our connection manager stat monitor
        _conmgrStatsUpdater.schedule(5000L, true);

        // initialize our reboot manager
        _rebmgr.init();
    }

    // from interface BangAdminProvider
    public void scheduleReboot (ClientObject caller, int minutes)
    {
        PlayerObject user = (PlayerObject)caller;
        if (!user.tokens.isSupport()) {
            log.warning("Got reboot schedule request from non-admin/support " +
                        "[who=" + user.who() + "].");
            return;
        }

        // if this is a zero minute reboot, just do the deed
        if (minutes == 0) {
            log.info("Performing immediate shutdown [for=" + user.who() + "].");
            _server.shutdown();
            return;
        }

        // shave 5 seconds off to avoid rounding up to the next time
        long when = System.currentTimeMillis() + minutes * 60 * 1000L - 5000L;
        _rebmgr.scheduleReboot(when, user.who());
    }

    /** Used to manage automatic reboots. */
    protected class BangRebootManager extends RebootManager
    {
        public BangRebootManager (BangServer server) {
            super(server);
        }

        public void scheduleReboot (long rebootTime, String initiator) {
            super.scheduleReboot(rebootTime, initiator);
            statobj.setServerRebootTime(rebootTime);
        }

        protected void broadcast (String message) {
            BangServer.chatprov.broadcast(
                null, BangCodes.BANG_MSGS, message, true);
        }

        protected int getDayFrequency () {
            return -1; // no automatically scheduled reboots for now
        }

        protected int getRebootHour () {
            return 8;
        }

        protected boolean getSkipWeekends () {
            return false;
        }

        protected String getCustomRebootMessage () {
            // for now we don't have auto-reboots, so let's not claim every hand scheduled reboot
            // is a "regularly scheduled reboot"
            return MessageBundle.taint("");
        }
    }

    /** This reads the status from the connection manager and stuffs it into
     * our server status object every 5 seconds. Because it reads synchronized
     * data and then just posts an event, it's OK that it runs directly on the
     * Interval dispatch thread. */
    protected Interval _conmgrStatsUpdater = new Interval() {
        public void expired () {
            statobj.setConnStats(BangServer.conmgr.getStats());
        }
    };

    protected BangServer _server;
    protected BangRebootManager _rebmgr;
}
