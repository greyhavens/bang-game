//
// $Id$

package com.threerings.bang.server.persist;

import com.samskivert.util.Invoker;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;
import com.threerings.presents.server.InvocationException;

/**
 * Provides a robust framework for doing something in exchange for a
 * player's money.
 */
public abstract class FinancialAction extends Invoker.Unit
{
    /**
     * Creates a financial action. If the constructor completes, the money
     * will be tied up in the action and immediately removed from the user
     * object, and the action will be posted to the supplied invoker. If
     * the player has insufficient funds, an invocation exception to that
     * effect will be thrown.
     */
    protected FinancialAction (Invoker invoker, BangUserObject user,
                               int scripCost, int goldCost)
        throws InvocationException
    {
        _scripCost = scripCost;
        _goldCost = goldCost;
        _user = user;

        // check and immediately deduct the necessary funds
        if (_user.scrip < _scripCost || _user.gold < _goldCost) {
            throw new InvocationException(BangCodes.INSUFFICIENT_FUNDS);
        }
        _user.scrip -= _scripCost;
        _user.gold -= _goldCost;

        invoker.postUnit(this);
    }

    @Override // documentation inherited
    public boolean invoke ()
    {
        // reserve any needed coins

        // then deduct the in-game cash

        // then do our persistent business

        // finally "spend" our reserved coins

        return true;
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        // let derived classes do post-persistent business
    }

    protected void fail ()
    {
        // roll everything back that needs it
    }

    protected BangUserObject _user;
    protected int _scripCost, _goldCost;
}
