//
// $Id$

package com.threerings.bang.ranch.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ListUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.server.BangInvoker;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.ItemRepository;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchMarshaller;
import com.threerings.bang.ranch.data.RanchObject;

import static com.threerings.bang.Log.log;

/**
 * Provides ranch-related services.
 */
@Singleton
public class RanchManager extends ShopManager
    implements RanchCodes, RanchProvider
{
    // documentation inherited from interface RanchProvider
    public void recruitBigShot (PlayerObject caller, String type, Name name,
                                RanchService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);
        if (user.tokens.isAnonymous()) {
            log.warning("Requested to recruit unit from anonymous user", "who", user.who());
            throw new InvocationException(ACCESS_DENIED);
        }

        UnitConfig config = UnitConfig.getConfig(type, false);
        if (config == null) {
            log.warning("Requested to recruit bogus unit", "who", user.who(), "type", type);
            throw new InvocationException(INTERNAL_ERROR);
        }
        if (config.rank != UnitConfig.Rank.BIGSHOT) {
            log.warning("Requested to recruit non-bigshot", "who", user.who(), "type", type);
            throw new InvocationException(INTERNAL_ERROR);
        }
        if (config.scripCost <= 0) {
            log.warning("Requested to recruit bogus bigshot", "who", user.who(), "type", type);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure this big shot is available for sale in this town
        if (!ListUtil.contains(UnitConfig.getTownUnits(user.townId), config)) {
            log.warning("Requested to recruit illegal unit", "who", user.who(),
                        "town", user.townId, "type", type);
            throw new InvocationException(ACCESS_DENIED);
        }

        // the client should prevent this
        if (name.toString().length() > BigShotItem.MAX_NAME_LENGTH) {
            log.warning("Requested to recruit bigshot with long name", "who", user.who(),
                        "type", type, "name", name);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create and deliver the unit to the player; all the heavy lifting is
        // handled by the financiial action
        BigShotItem unit = new BigShotItem(user.playerId, config.type);
        unit.setGivenName(name);
        _invoker.post(new RecruitBigShotAction(user, config, unit, listener));
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "ranch";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new RanchObject();
    }

    @Override // from PlaceManager
    protected void didInit ()
    {
        super.didInit();

        // TODO: anything?
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _robj = (RanchObject)_plobj;
        _robj.setService(BangServer.invmgr.registerProvider(this, RanchMarshaller.class));
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class RecruitBigShotAction extends FinancialAction
    {
        public RecruitBigShotAction (PlayerObject user, UnitConfig config, BigShotItem unit,
                                     RanchService.ResultListener listener)
        {
            super(user, config.scripCost, config.getCoinCost(user));
            _unit = unit;
            _listener = listener;
        }

        protected String getCoinDescrip () {
            return MessageBundle.compose(
                "m.bigshot_purchase", UnitConfig.getName(_unit.getType()));
        }

        protected String persistentAction () throws PersistenceException {
            _itemrepo.insertItem(_unit);
            return null;
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            _itemrepo.deleteItem(_unit, "recruit_rollback");
        }

        protected void actionCompleted () {
            _user.addToInventory(_unit);
            _listener.requestProcessed(_unit);
            super.actionCompleted();
        }
        protected void actionFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected String getPurchaseType () {
            return "ranch";
        }
        protected String getGoodType () {
            return "BigShot";
        }

        protected BigShotItem _unit;
        protected RanchService.ResultListener _listener;

        @Inject protected ItemRepository _itemrepo;
    }

    protected RanchObject _robj;

    // dependencies
    @Inject protected BangInvoker _invoker;
}
