//
// $Id$

package com.threerings.bang.gang.server.persist;

import com.samskivert.io.PersistenceException;

import com.threerings.util.Name;

import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.gang.data.GangObject;

/**
 * A financial action that uses gang funds instead of player funds.
 */
public abstract class GangFinancialAction extends FinancialAction
{
    protected GangFinancialAction (
        GangObject gang, boolean admin, int scripCost, int coinCost, int aceCost)
    {
        super(admin ? 0 : scripCost, admin ? 0 : coinCost);
        _gang = gang;
        _aceCost = (admin ? 0 : aceCost);
    }

    @Override // documentation inherited
    protected boolean checkSufficientFunds ()
    {
        return (_gang.scrip >= _scripCost && _gang.coins >= _coinCost && _gang.aces >= _aceCost);
    }

    @Override // documentation inherited
    protected void deductCost ()
    {
        _gang.startTransaction();
        try {
            _gang.setScrip(_gang.scrip - _scripCost);
            _gang.setCoins(_gang.coins - _coinCost);
            _gang.setAces(_gang.aces - _aceCost);
        } finally {
            _user.commitTransaction();
        }
    }

    @Override // documentation inherited
    protected void returnCost ()
    {
        _gang.startTransaction();
        try {
            _gang.setScrip(_gang.scrip + _scripCost);
            _gang.setCoins(_gang.coins + _coinCost);
            _gang.setAces(_gang.aces + _aceCost);
        } finally {
            _user.commitTransaction();
        }
    }

    @Override // documentation inherited
    protected String getCoinAccount ()
    {
        return _gang.getCoinAccount();
    }

    @Override // documentation inherited
    protected boolean shouldSpendCash ()
    {
        return (_scripCost > 0 || _aceCost > 0);
    }

    @Override // documentation inherited
    protected void spendCash ()
        throws PersistenceException
    {
        if (_scripCost > 0) {
            BangServer.gangrepo.spendScrip(_gang.gangId, _scripCost);
        }
        if (_aceCost > 0) {
            BangServer.gangrepo.spendAces(_gang.gangId, _aceCost);
        }
    }

    @Override // documentation inherited
    protected void grantCash ()
        throws PersistenceException
    {
        if (_scripCost > 0) {
            BangServer.gangrepo.grantScrip(_gang.gangId, _scripCost);
        }
        if (_aceCost > 0) {
            BangServer.gangrepo.grantAces(_gang.gangId, _aceCost);
        }
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", gang=").append(_gang.name);
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
        buf.append(", aces=").append(_aceCost);
    }

    protected GangObject _gang;
    protected int _aceCost;
}
