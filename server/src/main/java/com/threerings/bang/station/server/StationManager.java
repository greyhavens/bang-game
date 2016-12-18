//
// $Id$

package com.threerings.bang.station.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;

import com.threerings.util.MessageBundle;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.FreeTicket;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.server.BangInvoker;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;

import com.threerings.bang.station.client.StationService;
import com.threerings.bang.station.data.StationCodes;
import com.threerings.bang.station.data.StationMarshaller;
import com.threerings.bang.station.data.StationObject;

import static com.threerings.bang.Log.log;

/**
 * Implements the server-side of the Train Station services.
 */
@Singleton
public class StationManager extends ShopManager
    implements StationCodes, StationProvider
{
    // documentation inherited from interface StationProvider
    public void buyTicket (PlayerObject caller, StationService.ConfirmListener listener)
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
            log.warning("Player tried to buy ticket but they have them all", "who", user.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the ticket and make sure no funny business is afoot
        TrainTicket ticket = new TrainTicket(user.playerId, ticketTownIdx);
        if (ticket.getCoinCost(user) < 0 && !user.tokens.isAdmin()) {
            log.warning("Rejecting request to buy unavailable ticket", "who", user.who(),
                        "ticket", ticket);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // deliver the ticket to the player; heavy lifting is handled by the financial action
        _invoker.post(new BuyTicketAction(user, ticket, listener));
    }

    // documentation inherited from interface StationProvider
    public void activateTicket (PlayerObject caller, final StationService.ConfirmListener listener)
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
            log.warning("Player tried to activate non-existant free ticket", "who", user.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        if (ticket.isExpired(System.currentTimeMillis())) {
            // remove the expired ticket
            _invoker.postUnit(new PersistingUnit("activateTicket", listener) {
                public void invokePersistent() throws PersistenceException {
                    _itemrepo.deleteItem(finalTicket, "Free Ticket Expired");
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
        _invoker.postUnit(new PersistingUnit("activateTicket", listener) {
            public void invokePersistent() throws PersistenceException {
                // update the ticket record
                finalTicket.activate(System.currentTimeMillis());
                _itemrepo.updateItem(finalTicket);

                // update the player record
                _playrepo.activateNextTown(user.playerId, finalTicket.getExpire());
            }

            public void handleSuccess() {
                user.stats.addToSetStat(StatType.ACTIVATED_TICKETS, finalTicket.getTownId());
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
        _stobj.setService(BangServer.invmgr.registerProvider(this, StationMarshaller.class));
    }

    /** Used to recruit and deliver a big shot to a player. */
    protected static final class BuyTicketAction extends FinancialAction
    {
        public BuyTicketAction (PlayerObject user, TrainTicket ticket,
                                StationService.ConfirmListener listener)
        {
            super(user, ticket.getScripCost(), ticket.getCoinCost(user));
            _ticket = ticket;
            _listener = listener;
        }

        protected String getCoinDescrip () {
            return MessageBundle.compose(
                "m.ticket_purchase", _ticket.getTownId());
        }

        protected String persistentAction () throws PersistenceException {
            _playrepo.grantTownAccess(_user.playerId, _ticket.getTownId());
            _itemrepo.insertItem(_ticket);
            return null;
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            String oldTownId = BangCodes.TOWN_IDS[_ticket.getTownIndex()-1];
            _playrepo.grantTownAccess(_user.playerId, oldTownId);
            if (_ticket.getItemId() > 0) {
                _itemrepo.deleteItem(_ticket, "ticket_rollback");
            }
        }

        protected void actionCompleted () {
            _user.addToInventory(_ticket);
            _listener.requestProcessed();
            super.actionCompleted();
        }
        protected void actionFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected String getPurchaseType () {
            return "station";
        }
        protected String getGoodType () {
            return "Ticket";
        }

        protected TrainTicket _ticket;
        protected StationService.ConfirmListener _listener;

        @Inject protected ItemRepository _itemrepo;
    }

    protected StationObject _stobj;

    // dependencies
    @Inject protected BangInvoker _invoker;
    @Inject protected PlayerRepository _playrepo;
    @Inject protected ItemRepository _itemrepo;
}
