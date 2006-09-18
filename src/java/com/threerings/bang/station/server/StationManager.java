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

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.station.client.StationService;
import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationMarshaller;
import com.threerings.bang.station.data.StationObject;

import static com.threerings.bang.Log.log;

/**
 * Implements the server-side of the Train Station services.
 */
public class StationManager extends PlaceManager
    implements StationCodes, StationProvider
{
    // documentation inherited from interface StationProvider
    public void buyTicket (ClientObject caller,
                           StationService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        // determine the town to which this player will be traveling
        int ticketTownIdx = -1;
        for (int ii = 1; ii < BangCodes.TOWN_IDS.length; ii++) {
            if (!user.holdsTicket(BangCodes.TOWN_IDS[ii])) {
                ticketTownIdx = ii;
                break;
            }
        }

        // sanity check
        if (ticketTownIdx == -1) {
            log.warning("Player tried to buy ticket but they have them all " +
                        "[who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create and deliver the ticket to the player; all the heavy lifting
        // is handled by the financial action
        TrainTicket ticket = new TrainTicket(user.playerId, ticketTownIdx);
        new BuyTicketAction(user, ticket, listener).start();
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new StationObject();
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
        public BuyTicketAction (PlayerObject user, TrainTicket ticket,
                                StationService.ConfirmListener listener) {
            // admins get things for free
            super(user, user.tokens.isAdmin() ? 0 : ticket.getScripCost(),
                user.tokens.isAdmin() ? 0 : ticket.getCoinCost());
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
            BangServer.playrepo.grantTownAccess(
                _user.playerId, _ticket.getTownId());
            BangServer.itemrepo.insertItem(_ticket);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            String oldTownId = BangCodes.TOWN_IDS[_ticket.getTownIndex()-1];
            BangServer.playrepo.grantTownAccess(_user.playerId, oldTownId);
            if (_ticket.getItemId() > 0) {
                BangServer.itemrepo.deleteItem(_ticket, "ticket_rollback");
            }
        }

        protected void actionCompleted () {
            _user.addToInventory(_ticket);
            _listener.requestProcessed();
        }
        protected void actionFailed () {
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected TrainTicket _ticket;
        protected StationService.ConfirmListener _listener;
    }

    protected StationObject _stobj;
}
