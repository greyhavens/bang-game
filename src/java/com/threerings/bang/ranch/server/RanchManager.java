//
// $Id$

package com.threerings.bang.ranch.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ListUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.ranch.data.RanchCodes;

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
        final PlayerObject user = (PlayerObject)caller;
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

        // create and deliver the unit to the player; all the heavy
        // lifting is handled by the financiial action
        new RecruitBigShotAction(user, config, listener).start();
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class RecruitBigShotAction extends FinancialAction
    {
        public RecruitBigShotAction (PlayerObject user, UnitConfig config,
                                     RanchService.ResultListener listener) {
            super(user, config.scripCost, config.goldCost);
            _unit = new BigShotItem(user.playerId, config.type);
            _listener = listener;
        }

        protected void persistentAction () throws PersistenceException {
            BangServer.itemrepo.insertItem(_unit);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            BangServer.itemrepo.deleteItem(_unit, "recruit_rollback");
        }

        protected void actionCompleted () {
            _user.addToInventory(_unit);
            _listener.requestProcessed(_unit);
        }
        protected void actionFailed () {
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected BigShotItem _unit;
        protected RanchService.ResultListener _listener;
    }
}
