//
// $Id$

package com.threerings.bang.avatar.server;

import com.samskivert.io.PersistenceException;
import com.threerings.util.MessageBundle;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberMarshaller;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;

import static com.threerings.bang.Log.log;

/**
 * Provides Barber-related services.
 */
public class BarberManager extends PlaceManager
    implements BarberCodes, BarberProvider
{
    // documentation inherited from interface BarberProvider
    public void purchaseLook (
        ClientObject caller, Look look, BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        // TODO: compute the price of the look and create a BuyLookAction
        throw new InvocationException(INTERNAL_ERROR);
    }

    // documentation inherited from interface BarberProvider
    public void configureLook (
        ClientObject caller, String name, int[] articleIds,
        BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;
        // TODO
        throw new InvocationException(INTERNAL_ERROR);
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BarberObject.class;
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
        _bobj = (BarberObject)_plobj;
        _bobj.setService((BarberMarshaller)BangServer.invmgr.registerDispatcher(
                             new BarberDispatcher(this), false));
    }

    /** Used to purchase a new avatar look. */
    protected static final class BuyLookAction extends FinancialAction
    {
        public BuyLookAction (
            PlayerObject user, Look look, int scripCost, int coinCost,
            BarberService.ConfirmListener listener) {
            super(user, scripCost, coinCost);
            _look = look;
            _listener = listener;
        }

        protected int getCoinType () {
            return CoinTransaction.LOOK_PURCHASE;
        }
        protected String getCoinDescrip () {
            return MessageBundle.compose("m.look_purchase", _look.name);
        }

        protected void persistentAction () throws PersistenceException {
            BangServer.lookrepo.insertLook(_user.playerId, _look);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            BangServer.lookrepo.deleteLook(_user.playerId, _look.name);
        }

        protected void actionCompleted () {
            _user.addToLooks(_look);
            _user.setLook(_look.name);
            _listener.requestProcessed();
        }
        protected void actionFailed () {
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected Look _look;
        protected BarberService.ConfirmListener _listener;
    }

    protected BarberObject _bobj;
}
