//
// $Id$

package com.threerings.bang.bank.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.coin.data.CoinExOfferInfo;
import com.threerings.coin.server.CoinExOffer;

import com.threerings.bang.data.ConsolidatedOffer;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangCoinExchangeManager;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.bank.client.BankService;
import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the Bank.
 */
@Singleton
public class BankManager extends ShopManager
    implements BankProvider, BankCodes, BangCoinExchangeManager.OfferPublisher
{
    // documentation inherited from interface BankProvider
    public void getMyOffers (ClientObject caller, BankService.OfferListener ol)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);
        CoinExOfferInfo[][] offers = _coinexmgr.getPlayerOffers(user);
        ol.gotOffers(offers[0], offers[1]);
    }

    // documentation inherited from interface BankProvider
    public void postOffer (ClientObject caller, int coins, int pricePerCoin, boolean buying,
                           boolean immediate, BankService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = requireShopEnabled(caller);
        CoinExOffer offer = new CoinExOffer();
        offer.accountName = player.username.toString();
        offer.gameName = offer.accountName;
        offer.buy = buying;
        offer.volume = (short)Math.min(coins, Short.MAX_VALUE);
        offer.price = (short)Math.min(pricePerCoin, Short.MAX_VALUE);
        _coinexmgr.postOffer(caller, offer, immediate, new ResultAdapter<CoinExOfferInfo>(listener));
    }

    // documentation inherited from interface BankProvider
    public void cancelOffer (ClientObject caller, int offerId, BankService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject player = requireShopEnabled(caller);
        if (_coinexmgr.cancelOffer(player.username.toString(), offerId)) {
            cl.requestProcessed();
        } else {
            cl.requestFailed(NO_SUCH_OFFER);
        }
    }

    // documentation inherited from interface OfferPublisher
    public void updateOffers (ConsolidatedOffer[] buys, ConsolidatedOffer[] sells, int lastPrice)
    {
        if (buys != null) {
            _bankobj.setBuyOffers(buys);
        }
        if (sells != null) {
            _bankobj.setSellOffers(sells);
        }
        if (lastPrice != -1) {
            _bankobj.setLastTrade(lastPrice);
        }
    }

    // documentation inherited from interface OfferPublisher
    public void offerModified (int offerId)
    {
        _omgr.postEvent(new MessageEvent(
                    _bankobj.getOid(), OFFER_MODIFIED, new Integer[] { offerId }));
    }

    // documentation inherited from interface OfferPublisher
    public void offersDestroyed (int[] offerIds)
    {
        _omgr.postEvent(new MessageEvent(
                    _bankobj.getOid(), OFFERS_DESTROYED, new Object[] { offerIds }));
    }

    @Override // from ShopManager
    protected boolean allowAnonymous ()
    {
        return false;
    }

    @Override // from ShopManager
    protected boolean requireHandle ()
    {
        return true;
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "bank";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new BankObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _bankobj = (BankObject)_plobj;
        _bankobj.setService(BangServer.invmgr.registerDispatcher(new BankDispatcher(this)));

        // register with the coin exchange manager
        _coinexmgr.registerPublisher(this);
    }

    @Override // from ShopManager
    protected PlayerObject requireShopEnabled (ClientObject caller)
        throws InvocationException
    {
        PlayerObject user = super.requireShopEnabled(caller);
        if (!DeploymentConfig.usesCoins()) {
            log.warning("Rejecting bank operation on non-coin deployment", "who", user.who());
            throw new InvocationException(BankCodes.E_INTERNAL_ERROR);
        }
        if (!user.canExchange()) {
            throw new InvocationException(BankCodes.BANK_MSGS, "e.require_exchange_pass");
        }
        return user;
    }

    protected BankObject _bankobj;

    // dependencies
    @Inject BangCoinExchangeManager _coinexmgr;
}
