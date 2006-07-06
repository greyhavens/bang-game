//
// $Id$

package com.threerings.bang.station.server;

import com.samskivert.io.PersistenceException;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.station.client.StationService;
import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationMarshaller;
import com.threerings.bang.station.data.StationObject;

/**
 * Implements the server-side of the Train Station services.
 */
public class StationManager extends PlaceManager
    implements StationCodes, StationProvider
{
    // documentation inherited from interface StationProvider
    public void buyTicket (ClientObject caller,
                           StationService.ResultListener listener)
        throws InvocationException
    {
        // TODO
        throw new InvocationException(INTERNAL_ERROR);
    }

    @Override // documentation inherited
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return StationObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _stobj = (StationObject)_plobj;
        _stobj.setService(
            (StationMarshaller)BangServer.invmgr.registerDispatcher(
                new StationDispatcher(this), false));
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class BuyTicketAction extends FinancialAction
    {
        public BuyTicketAction (PlayerObject user, int scripCost, int coinCost,
                                TrainTicket ticket,
                                StationService.ResultListener listener) {
            super(user, scripCost, coinCost);
            _ticket = ticket;
            _listener = listener;
        }

        protected int getCoinType () {
            return CoinTransaction.TICKET_PURCHASE;
        }
        protected String getCoinDescrip () {
            return MessageBundle.compose(
                "m.ticket_purchase", _ticket.getTownId());
        }

        protected void persistentAction () throws PersistenceException {
            BangServer.itemrepo.insertItem(_ticket);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            BangServer.itemrepo.deleteItem(_ticket, "ticket_rollback");
        }

        protected void actionCompleted () {
            _user.addToInventory(_ticket);
            _listener.requestProcessed(_ticket);
        }
        protected void actionFailed () {
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected TrainTicket _ticket;
        protected StationService.ResultListener _listener;
    }

    protected StationObject _stobj;
}
