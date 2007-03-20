//
// $Id$

package com.threerings.bang.station.server;

import com.samskivert.io.PersistenceException;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.FreeTicket;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.station.client.StationService;
import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationMarshaller;
import com.threerings.bang.station.data.StationObject;

import static com.threerings.bang.Log.log;

/**
 * Implements the server-side of the Train Station services.
 */
public class StationManager extends ShopManager
    implements StationCodes, StationProvider
{
    // documentation inherited from interface StationProvider
    public void buyTicket (ClientObject caller, StationService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

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

        // create the ticket and make sure no funny business is afoot
        TrainTicket ticket = new TrainTicket(user.playerId, ticketTownIdx);
        if (ticket.getCoinCost() < 0 && !user.tokens.isAdmin()) {
            log.warning("Rejecting request to buy unavailable ticket [who=" + user.who() +
                        ", ticket=" + ticket + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // deliver the ticket to the player; all the heavy lifting is handled by the financial
        // action
        new BuyTicketAction(user, ticket, listener).start();
    }

    // documentation inherited from interface StationProvider
    public void activateTicket (ClientObject caller, final StationService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = requireShopEnabled(caller);

        // find which ticket they wish to activate
        FreeTicket ticket = null;
        for (int ii = 1; ii < BangCodes.TOWN_IDS.length; ii++) {
            if ((ticket = (FreeTicket)user.getEquivalentItem(new FreeTicket(-1, ii))) != null) {
                break;
            }
        }

        final FreeTicket finalTicket = ticket;

        if (ticket == null) {
            log.warning("Player tried to activate non-existant free ticket " +
                    "[who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        if (ticket.isExpired(System.currentTimeMillis())) {
            // remove the expired ticket
            BangServer.invoker.postUnit(new PersistingUnit("activateTicket", listener) {
                public void invokePersistent() throws PersistenceException {
                    BangServer.itemrepo.deleteItem(finalTicket, "Free Ticket Expired");
                }

                public void handleSuccess() {
                    user.removeFromInventory(finalTicket.getKey());
                    listener.requestFailed(getFailureMessage());
                }

                public String getFailureMessage() {
                    return "m.ticket_expired";
                }
            });
            return;
        }

        if (ticket.isActivated()) {
            listener.requestProcessed();
            return;
        }


        // go activate the ticket
        BangServer.invoker.postUnit(new PersistingUnit("activateTicket", listener) {
            public void invokePersistent() throws PersistenceException {
                // update the ticket record
                finalTicket.activate(System.currentTimeMillis());
                BangServer.itemrepo.updateItem(finalTicket);

                // update the player record
                BangServer.playrepo.activateNextTown(user.playerId, finalTicket.getExpire());
            }

            public void handleSuccess() {
                user.stats.addToSetStat(Stat.Type.ACTIVATED_TICKETS, finalTicket.getTownId());
                listener.requestProcessed();
            }

            public String getFailureMessage() {
                return "m.ticket_activate_failed";
            }
        });
    }

    @Override // from ShopManager
    protected boolean allowAnonymous ()
    {
        return false;
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "station";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new StationObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _stobj = (StationObject)_plobj;
        _stobj.setService((StationMarshaller)
                          BangServer.invmgr.registerDispatcher(new StationDispatcher(this)));
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class BuyTicketAction extends FinancialAction
    {
        public BuyTicketAction (PlayerObject user, TrainTicket ticket,
                                StationService.ConfirmListener listener)
        {
            super(user, ticket.getScripCost(), ticket.getCoinCost());
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

        protected String persistentAction () throws PersistenceException {
            BangServer.playrepo.grantTownAccess(
                _user.playerId, _ticket.getTownId());
            BangServer.itemrepo.insertItem(_ticket);
            return null;
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
        protected void actionFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected TrainTicket _ticket;
        protected StationService.ConfirmListener _listener;
    }

    protected StationObject _stobj;
}
