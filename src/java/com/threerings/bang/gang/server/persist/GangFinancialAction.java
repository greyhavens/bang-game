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
    protected GangFinancialAction (GangObject gang, boolean admin, int scripCost, int coinCost)
    {
        super(admin ? 0 : scripCost, admin ? 0 : coinCost);
        _gang = gang;
    }

    @Override // documentation inherited
    protected int getScrip ()
    {
        return _gang.scrip;
    }

    @Override // documentation inherited
    protected int getCoins ()
    {
        return _gang.coins;
    }

    @Override // documentation inherited
    protected void setCash (int scrip, int coins)
    {
        _gang.startTransaction();
        try {
            _gang.setScrip(scrip);
            _gang.setCoins(coins);
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
    protected void spendScrip (int scrip)
        throws PersistenceException
    {
        BangServer.gangrepo.spendScrip(_gang.gangId, scrip);
    }

    @Override // documentation inherited
    protected void grantScrip (int scrip)
        throws PersistenceException
    {
        BangServer.gangrepo.grantScrip(_gang.gangId, scrip);
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", gang=").append(_gang.name);
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
    }

    protected GangObject _gang;
}
