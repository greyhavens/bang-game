//
// $Id$

package com.threerings.bang.ranch.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ListUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchMarshaller;
import com.threerings.bang.ranch.data.RanchObject;

import static com.threerings.bang.Log.log;

/**
 * Provides ranch-related services.
 */
public class RanchManager extends PlaceManager
    implements RanchCodes, RanchProvider
{
    // documentation inherited from interface RanchProvider
    public void recruitBigShot (ClientObject caller, String type, Name name,
                                RanchService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        UnitConfig config = UnitConfig.getConfig(type, false);
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

        // the client should prevent this
        if (name.toString().length() > BigShotItem.MAX_NAME_LENGTH) {
            log.warning("Requested to recruit bigshot with long name " +
                        "[who=" + user.who() + ", type=" + type +
                        ", name=" + name + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create and deliver the unit to the player; all the heavy lifting is
        // handled by the financiial action
        BigShotItem unit = new BigShotItem(user.playerId, config.type);
        unit.setGivenName(name);
        new RecruitBigShotAction(user, config, unit, listener).start();
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new RanchObject();
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // TODO: anything?
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _robj = (RanchObject)_plobj;
        _robj.setService((RanchMarshaller)BangServer.invmgr.registerDispatcher(
                             new RanchDispatcher(this), false));
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class RecruitBigShotAction extends FinancialAction
    {
        public RecruitBigShotAction (
            PlayerObject user, UnitConfig config, BigShotItem unit,
            RanchService.ResultListener listener)
            throws InvocationException
        {
            super(user, config.scripCost, config.coinCost);
            _unit = unit;
            _listener = listener;
        }

        protected int getCoinType () {
            return CoinTransaction.BIGSHOT_PURCHASE;
        }
        protected String getCoinDescrip () {
            return MessageBundle.compose(
                "m.bigshot_purchase", UnitConfig.getName(_unit.getType()));
        }

        protected String persistentAction () throws PersistenceException {
            BangServer.itemrepo.insertItem(_unit);
            return null;
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            BangServer.itemrepo.deleteItem(_unit, "recruit_rollback");
        }

        protected void actionCompleted () {
            _user.addToInventory(_unit);
            _listener.requestProcessed(_unit);
        }
        protected void actionFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected BigShotItem _unit;
        protected RanchService.ResultListener _listener;
    }

    protected RanchObject _robj;
}
