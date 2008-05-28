//
// $Id$

package com.threerings.bang.server.persist;

import java.util.HashMap;
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
 * Provides a robust framework for doing something in exchange for a player's money.
 */
public abstract class FinancialAction extends Invoker.Unit
{
    /**
     * Starts this financial action. If the method returns, the money will be tied up in the action
     * and immediately removed from the user object, and the action will be posted to the supplied
     * invoker. If the player has insufficient funds, an invocation exception to that effect will
     * be thrown.
     */
    public void start ()
        throws InvocationException
    {
        lockAndDeduct();
        BangServer.invoker.postUnit(this);
    }

    @Override // documentation inherited
    public boolean invoke ()
    {
        try {
            if (_coinCost > 0) {
                _coinres = BangServer.coinmgr.getCoinRepository().reserveCoins(
                    getCoinAccount(), _coinCost);
                if (_coinres == -1) {
                    log.warning("Failed to reserve coins " + this + ".");
                    fail(BangCodes.E_INSUFFICIENT_FUNDS);
                    return true;
                }
            }

            if (shouldSpendCash()) {
                // spend the in-game cash
                spendCash();
                _cashSpent = true;
            }

            // then do our persistent business
            String errmsg = persistentAction();
            if (errmsg != null) {
                fail(errmsg);
                return true;
            }
            _actionTaken = true;

            if (_coinCost > 0) {
                // finally "spend" our reserved coins
                if (!spendCoins(_coinres)) {
                    log.warning("Failed to spend coin reservation " + this +
                                " [resid=" + _coinres + "].");
                    fail(BangCodes.INTERNAL_ERROR);
                    return true;
                }
            }

        } catch (Exception e) {
            log.warning("Financial action failed " + this, e);
            fail(BangCodes.INTERNAL_ERROR);
        }

        return true;
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        try {
            if (_failmsg != null) {
                // return the scrip and coins to the actor
                returnCost();
                actionFailed(_failmsg);
            } else {
                actionCompleted();
            }

        } finally {
            // now it's safe for this actor to start another financial action
            _accountLock.remove(getCoinAccount());
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
        // admins and support get everything for free because they're cool like that
        _scripCost = user.tokens.isSupport() ? 0 : scripCost;
        _coinCost = user.tokens.isSupport() ? 0 : coinCost;
    }

    protected FinancialAction (int scripCost, int coinCost)
    {
        _scripCost = scripCost;
        _coinCost = coinCost;
    }

    /**
     * If a financial action involves coins, this method <em>must</em> be overridden to classify
     * the purchase. See {@link CoinTransaction}.
     */
    protected int getCoinType ()
    {
        return CoinTransaction.PRODUCT_PURCHASE;
    }

    /**
     * If a financial action involves coins, this method <em>must</em> be overridden to provide a
     * translatable string describing the purchase.
     */
    protected String getCoinDescrip ()
    {
        return null;
    }

    /**
     * Here derived classes can take any persistent action needed knowing that necessary coins have
     * been reserved and necessary scrip has been spent.
     *
     * @return null if the action was taken, a translatable error string if the action could not be
     * taken for whatever reason (any action taken in this method must be rolled back before
     * returning as {@link #rollbackPersistentAction} will <em>not</em> be called).
     */
    protected String persistentAction ()
        throws PersistenceException
    {
        return null;
    }

    /**
     * Any action taken in {@link #persistentAction} must be revoked in this method, which is
     * called if we fail to finalize
     */
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
    }

    /**
     * If all persistent processing completed successfully, this method will be called back on the
     * distributed object thread to allow final completion of the action.
     */
    protected void actionCompleted ()
    {
        StringBuffer buf = (new StringBuffer(getPurchaseType())).append("_purchase ");
        buf.append(_user.playerId).append(" t:").append(getGoodType());
        buf.append(" s:").append(_scripCost).append(" c:").append(_coinCost);
        BangServer.itemLog(buf.toString());
    }

    /**
     * If any step of the persistent processing of an action failed, rollbacks will be attempted
     * for all completed parts of the action and this method will be called on the distributed
     * object thread to allow for reporting of the failed action.
     *
     * <p><em>Note:</em> the user's scrip and coins will have been returned (in their user object
     * and in the database) by the time this method is called.
     *
     * @param cause either {@link BangCodes#INTERNAL_ERROR} or the message from an {@link
     * InvocationException} that was thrown during the processing of the action.
     */
    protected void actionFailed (String cause)
    {
    }

    /**
     * Called if something goes wrong during any step of this financial action. Everything that
     * completed successfully will be rolled back.
     */
    protected void fail (String failureMessage)
    {
        _failmsg = failureMessage;

        // roll everything back that needs it
        if (_coinres != -1) {
            // return the coin reservation
            try {
                if (!BangServer.coinmgr.getCoinRepository().
                    returnReservation(_coinres)) {
                    log.warning("Failed to return coins " + this + ".");
                }
            } catch (PersistenceException pe) {
                log.warning("Failed to return coins " + this, pe);
            }
        }

        if (_cashSpent) {
            try {
                grantCash();
            } catch (PersistenceException pe) {
                log.warning("Failed to return cash " + this, pe);
            }
        }

        if (_actionTaken) {
            try {
                rollbackPersistentAction();
            } catch (PersistenceException pe) {
                log.warning("Failed to rollback action " + this, pe);
            }
        }
    }

    /**
     * Locks (or attempts to lock) the coin account and deducts the required funds from the dobj.
     */
    protected void lockAndDeduct ()
        throws InvocationException
    {
        String account = getCoinAccount();
        String ntype = getClass().getName(), otype = _accountLock.get(account);
        if (otype != null) {
            log.info("Preventing overlapping financial action [who=" + account +
                     ", new=" + ntype + ", old=" + otype + "].");
            throw new InvocationException(BangCodes.BANG_MSGS, "e.processing_purchase");
        }
        _accountLock.put(account, ntype);

        // check and immediately deduct the necessary funds
        if (!checkSufficientFunds()) {
            _accountLock.remove(account); // release our lock
            throw new InvocationException(BangCodes.E_INSUFFICIENT_FUNDS);
        }
        deductCost();
    }

    /**
     * Returns the actor's account in the coin database.
     */
    protected String getCoinAccount ()
    {
        return _user.username.toString();
    }

    /**
     * Checks whether the account has sufficient funds to complete the transaction.
     */
    protected boolean checkSufficientFunds ()
    {
        return (_user.scrip >= _scripCost && _user.coins >= _coinCost);
    }

    /**
     * Deducts the cost of the transaction from the fields in the dobj.
     */
    protected void deductCost ()
    {
        _user.startTransaction();
        try {
            _user.setScrip(_user.scrip - _scripCost);
            _user.setCoins(_user.coins - _coinCost);
        } finally {
            _user.commitTransaction();
        }
    }

    /**
     * Returns the cost of the transaction to the fields in the dobj.
     */
    protected void returnCost ()
    {
        _user.startTransaction();
        try {
            _user.setScrip(_user.scrip + _scripCost);
            _user.setCoins(_user.coins + _coinCost);
        } finally {
            _user.commitTransaction();
        }
    }

    /**
     * Checks whether we are spending non-coin currency.
     */
    protected boolean shouldSpendCash ()
    {
        return (_scripCost > 0);
    }

    /**
     * Updates the database to spend the actor's non-coin currency.
     */
    protected void spendCash ()
        throws PersistenceException
    {
        BangServer.playrepo.spendScrip(_user.playerId, _scripCost);
    }

    /**
     * Updates the database to grant the cost in non-coin currency to the actor.
     */
    protected void grantCash ()
        throws PersistenceException
    {
        BangServer.playrepo.grantScrip(_user.playerId, _scripCost);
    }

    /**
     * Updates the database to spend the actor's coins.
     */
    protected boolean spendCoins (int reservationId)
        throws PersistenceException
    {
        return BangServer.coinmgr.getCoinRepository().spendCoins(
            reservationId, getCoinType(), getCoinDescrip());
    }

    /**
     * The type of purchases being made.
     */
    protected String getPurchaseType ()
    {
        return "generic";
    }

    /**
     * The type of good being purchased.
     */
    protected abstract String getGoodType ();

    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", who=").append(_user.who());
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
    }

    protected PlayerObject _user;
    protected int _scripCost, _coinCost;
    protected boolean _cashSpent, _actionTaken;
    protected String _failmsg;
    protected int _coinres = -1;

    protected static HashMap<String,String> _accountLock = new HashMap<String,String>();
}
