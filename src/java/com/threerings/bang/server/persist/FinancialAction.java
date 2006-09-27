//
// $Id$

package com.threerings.bang.server.persist;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.presents.server.InvocationException;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;

import static com.threerings.bang.Log.log;

/**
 * Provides a robust framework for doing something in exchange for a
 * player's money.
 */
public abstract class FinancialAction extends Invoker.Unit
{
    /**
     * Starts this financial action. If the method returns, the money will
     * be tied up in the action and immediately removed from the user
     * object, and the action will be posted to the supplied invoker. If
     * the player has insufficient funds, an invocation exception to that
     * effect will be thrown.
     */
    public void start ()
        throws InvocationException
    {
        // check and immediately deduct the necessary funds
        if (_user.scrip < _scripCost || _user.coins < _coinCost) {
            throw new InvocationException(BangCodes.INSUFFICIENT_FUNDS);
        }
        _user.setScrip(_user.scrip - _scripCost);
        _user.setCoins(_user.coins - _coinCost);

        BangServer.invoker.postUnit(this);
    }

    @Override // documentation inherited
    public boolean invoke ()
    {
        try {
            if (_coinCost > 0) {
                _coinres = BangServer.coinmgr.getCoinRepository().reserveCoins(
                    _user.username.toString(), _coinCost);
                if (_coinres == -1) {
                    log.warning("Failed to reserve coins " + this + ".");
                    fail();
                    return true;
                }
            }

            if (_scripCost > 0) {
                // then deduct the in-game cash
                BangServer.playrepo.spendScrip(_user.playerId, _scripCost);
                _scripSpent = true;
            }

            // then do our persistent business
            persistentAction();
            _actionTaken = true;

            if (_coinCost > 0) {
                // finally "spend" our reserved coins
                if (!BangServer.coinmgr.getCoinRepository().spendCoins(
                        _coinres, getCoinType(), getCoinDescrip())) {
                    log.warning("Failed to spend coin reservation " + this +
                                " [resid=" + _coinres + "].");
                    fail();
                    return true;
                }
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Financial action failed " + this, e);
            fail();
        }

        return true;
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        if (_failed) {
            // return the scrip and coins to the user
            _user.setScrip(_user.scrip + _scripCost);
            _user.setCoins(_user.coins + _coinCost);
            actionFailed();
        } else {
            actionCompleted();
        }
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer("[");
        toString(buf);
        return buf.append("]").toString();
    }

    protected FinancialAction (PlayerObject user, int scripCost, int coinCost)
    {
        _user = user;
        // admins get everything for free because they're cool like that
        _scripCost = user.tokens.isAdmin() ? 0 : scripCost;
        _coinCost = user.tokens.isAdmin() ? 0 : coinCost;
    }

    /**
     * If a financial action involves coins, this method <em>must</em> be
     * overridden to classify the purchase. See {@link CoinTransaction}.
     */
    protected int getCoinType ()
    {
        return CoinTransaction.PRODUCT_PURCHASE;
    }

    /**
     * If a financial action involves coins, this method <em>must</em> be
     * overridden to provide a translatable string describing the purchase.
     */
    protected String getCoinDescrip ()
    {
        return null;
    }

    /**
     * Here derived classes can take any persistent action needed knowing that
     * necessary coins have been reserved and necessary scrip has been spent.
     */
    protected void persistentAction ()
        throws PersistenceException
    {
    }

    /**
     * Any action taken in {@link #persistentAction} must be revoked in
     * this method, which is called if we fail to finalize
     */
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
    }

    /**
     * If all persistent processing completed successfully, this method
     * will be called back on the distributed object thread to allow final
     * completion of the action.
     */
    protected void actionCompleted ()
    {
    }

    /**
     * If any step of the persistent processing of an action failed, rollbacks
     * will be attempted for all completed parts of the action and this method
     * will be called on the distributed object thread to allow for reporting
     * of the failed action.
     *
     * <p><em>Note:</em> the user's scrip and coins will have been returned (in
     * their user object and in the database) by the time this method is
     * called.
     */
    protected void actionFailed ()
    {
    }

    /**
     * Called if something goes wrong during any step of this financial
     * action. Everything that completed successfully will be rolled back.
     */
    protected void fail ()
    {
        _failed = true;

        // roll everything back that needs it
        if (_coinres != -1) {
            // return the coin reservation
            try {
                if (!BangServer.coinmgr.getCoinRepository().
                    returnReservation(_coinres)) {
                    log.warning("Failed to return coins " + this + ".");
                }
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to return coins " + this, pe);
            }
        }

        if (_scripSpent) {
            try {
                BangServer.playrepo.grantScrip(_user.playerId, _scripCost);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to return scrip " + this, pe);
            }
        }

        if (_actionTaken) {
            try {
                rollbackPersistentAction();
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Failed to rollback action " + this, pe);
            }
        }
    }

    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", who=").append(_user.who());
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
    }

    protected PlayerObject _user;
    protected int _scripCost, _coinCost;
    protected boolean _scripSpent, _actionTaken, _failed;
    protected int _coinres = -1;
}
