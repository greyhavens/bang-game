//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.server.BangServer;

import com.threerings.bang.bank.data.BankCodes;
import com.threerings.bang.bank.data.BankMarshaller;
import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the Bank.
 */
public class BankManager extends PlaceManager
    implements BankProvider, BankCodes
{
    // documentation inherited from interface
    public void buyCoins (ClientObject caller, int coins, int cost,
                          InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        throw new InvocationException(INTERNAL_ERROR);
    }

    // documentation inherited from interface
    public void sellCoins (ClientObject caller, int coins, int earnings,
                           InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        throw new InvocationException(INTERNAL_ERROR);
    }

    // documentation inherited from interface
    public void postBuyOffer (ClientObject caller, int coins, int pricePerCoin,
                              InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        throw new InvocationException(INTERNAL_ERROR);
    }

    // documentation inherited from interface
    public void postSellOffer (ClientObject caller, int coins, int pricePerCoin,
                               InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        throw new InvocationException(INTERNAL_ERROR);
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

    protected BankObject _bobj;
}
