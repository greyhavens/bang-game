//
// $Id$

package com.threerings.bang.gang.server.persist;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangRepository;

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
    protected String checkSufficientFunds ()
    {
        if (_gang.scrip < _scripCost) {
            return BangCodes.E_INSUFFICIENT_SCRIP;
        }
        switch (DeploymentConfig.getPaymentType()) {
        default:
        case COINS:
            if (_gang.coins < _coinCost) {
                return BangCodes.E_INSUFFICIENT_COINS;
            }
            break;
        case ONETIME:
            // we ignore coin costs on gang financial actions as the buyer is always a leader who
            // holds a onetime pass
            break;
        }
        if (_gang.aces < _aceCost) {
            return BangCodes.E_INSUFFICIENT_ACES;
        }
        return null;
    }

    @Override // documentation inherited
    protected void deductCost ()
    {
        _gang.startTransaction();
        try {
            _gang.setScrip(_gang.scrip - _scripCost);
            if (DeploymentConfig.usesCoins()) {
                _gang.setCoins(_gang.coins - _coinCost);
            }
            _gang.setAces(_gang.aces - _aceCost);
        } finally {
            _gang.commitTransaction();
        }
    }

    @Override // documentation inherited
    protected void returnCost ()
    {
        _gang.startTransaction();
        try {
            _gang.setScrip(_gang.scrip + _scripCost);
            if (DeploymentConfig.usesCoins()) {
                _gang.setCoins(_gang.coins + _coinCost);
            }
            _gang.setAces(_gang.aces + _aceCost);
        } finally {
            _gang.commitTransaction();
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
            _gangrepo.spendScrip(_gang.gangId, _scripCost);
        }
        if (_aceCost > 0) {
            _gangrepo.spendAces(_gang.gangId, _aceCost);
        }
    }

    @Override // documentation inherited
    protected void grantCash ()
        throws PersistenceException
    {
        if (_scripCost > 0) {
            _gangrepo.grantScrip(_gang.gangId, _scripCost);
        }
        if (_aceCost > 0) {
            _gangrepo.grantAces(_gang.gangId, _aceCost);
        }
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        StringBuffer buf = (new StringBuffer(getPurchaseType())).append("_purchase ");
        buf.append(_gang.gangId).append(" t:").append(getGoodType());
        buf.append(" s:").append(_scripCost).append(" c:").append(_coinCost);
        BangServer.itemLog(buf.toString());
    }

    @Override // documentation inherited
    protected String getPurchaseType ()
    {
        return "gang";
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

    // dependencies
    @Inject protected GangRepository _gangrepo;
}
