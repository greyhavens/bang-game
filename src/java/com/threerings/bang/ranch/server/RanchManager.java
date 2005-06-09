//
// $Id$

package com.threerings.bang.ranch.server;

import com.samskivert.util.ListUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.server.BangServer;

import static com.threerings.bang.Log.log;

/**
 * Provides ranch-related services.
 */
public class RanchManager
    implements RanchCodes, RanchProvider
{
    /**
     * Prepares the ranch manager for operation.
     */
    public void init (InvocationManager invmgr)
    {
        // register ourselves with the invocation manager
        invmgr.registerDispatcher(new RanchDispatcher(this), true);
    }

    // documentation inherited from interface RanchProvider
    public void recruitBigShot (ClientObject caller, String type,
                                final RanchService.ResultListener listener)
        throws InvocationException
    {
        final BangUserObject user = (BangUserObject)caller;
        final UnitConfig config = UnitConfig.getConfig(type);
        if (config == null) {
            log.warning("Requested to recruit bogus unit [who=" + user.who() +
                        ", type=" + type + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure this big shot is available for sale in this town
        if (!ListUtil.contains(UnitConfig.getTownUnits(user.townId), config)) {
            log.warning("Requested to recruit illegal unit [who=" + user.who() +
                        ", town=" + user.townId + ", type=" + type + "].");
            throw new InvocationException(ACCESS_DENIED);
        }

        // make sure they've got the scrip and gold to pay for it
        if (user.scrip < config.scripCost || user.gold < config.goldCost) {
            throw new InvocationException(INSUFFICIENT_FUNDS);
        }
    }
}
