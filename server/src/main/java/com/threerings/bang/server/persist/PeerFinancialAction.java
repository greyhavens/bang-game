//
// $Id$

package com.threerings.bang.server.persist;

import com.google.inject.Inject;
import com.samskivert.io.PersistenceException;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.server.persist.PlayerRepository;

/**
 * Handles a financial action undertaken on behalf of another peer node.
 */
public abstract class PeerFinancialAction extends FinancialAction
{
    @Override // from FinancialAction
    public boolean checkStart ()
    {
        // we don't call lockAndDeduct(), that was called on the originating peer
        return true;
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        if (_failmsg != null) {
            actionFailed(_failmsg);
        } else {
            actionCompleted();
        }
    }

    protected PeerFinancialAction (
        String coinAccount, int playerId, int scripCost, int coinCost,
        InvocationService.ConfirmListener listener)
    {
        super(scripCost, coinCost);
        _coinAccount = coinAccount;
        _playerId = playerId;
        _listener = listener;
    }

    @Override // documentation inherited
    protected String getCoinAccount ()
    {
        return _coinAccount;
    }

    @Override // documentation inherited
    protected void spendCash ()
        throws PersistenceException
    {
        _playrepo.spendScrip(_playerId, _scripCost);
    }

    /**
     * Updates the database to grant the cost in non-coin currency to the actor.
     */
    protected void grantCash ()
        throws PersistenceException
    {
        _playrepo.grantScrip(_playerId, _scripCost);
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _listener.requestProcessed();
    }

    @Override // documentation inherited
    protected void actionFailed (String cause)
    {
        _listener.requestFailed(cause);
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", coinAccount=").append(_coinAccount);
        buf.append(", playerId=").append(_playerId);
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
    }

    protected String getGoodType ()
    {
        return null;
    }

    protected String _coinAccount;
    protected int _playerId;
    protected InvocationService.ConfirmListener _listener;

    // dependencies
    @Inject protected PlayerRepository _playrepo;
}
