//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ConfirmAdapter;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.coin.server.CoinExOffer;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankMarshaller;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the Bank.
 */
public class BankManager extends PlaceManager
    implements BankProvider, BankCodes, BangCoinExchangeManager.OfferPublisher
{
    // documentation inherited from interface BankProvider
    public void buyCoins (ClientObject caller, int coins, int pricePerCoin,
                          InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        CoinExOffer offer = createOffer(caller, true, coins, pricePerCoin);
        BangServer.coinexmgr.postOffer(caller, offer, true,
                                       new ConfirmAdapter(listener));
    }

    // documentation inherited from interface BankProvider
    public void sellCoins (ClientObject caller, int coins, int pricePerCoin,
                           InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        CoinExOffer offer = createOffer(caller, false, coins, pricePerCoin);
        BangServer.coinexmgr.postOffer(caller, offer, true,
                                       new ConfirmAdapter(listener));
    }

    // documentation inherited from interface BankProvider
    public void postBuyOffer (ClientObject caller, int coins, int pricePerCoin,
                              InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        CoinExOffer offer = createOffer(caller, true, coins, pricePerCoin);
        BangServer.coinexmgr.postOffer(caller, offer, false,
                                       new ConfirmAdapter(listener));
    }

    // documentation inherited from interface BankProvider
    public void postSellOffer (ClientObject caller, int coins, int pricePerCoin,
                               InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        CoinExOffer offer = createOffer(caller, false, coins, pricePerCoin);
        BangServer.coinexmgr.postOffer(caller, offer, false,
                                       new ConfirmAdapter(listener));
    }

    // documentation inherited from interface OfferPublisher
    public void updateOffers (boolean buy, boolean sell, int lastPrice)
    {
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BankObject.class;
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
        _bobj = (BankObject)_plobj;
        _bobj.setService((BankMarshaller)BangServer.invmgr.registerDispatcher(
                             new BankDispatcher(this), false));
    }

    /**
     * Creates a coin exchange offer for our various buying, selling and offer
     * posting needs.
     */
    protected CoinExOffer createOffer (
        ClientObject caller, boolean buy, int coins, int pricePerCoin)
    {
        PlayerObject player = (PlayerObject)caller;
        CoinExOffer offer = new CoinExOffer();
        offer.accountName = player.accountName.toString();
        offer.gameName = offer.accountName;
        offer.buy = buy;
        offer.volume = (short)Math.min(coins, Short.MAX_VALUE);
        offer.price = (short)Math.min(pricePerCoin, Short.MAX_VALUE);
        return offer;
    }

    protected BankObject _bobj;
}
