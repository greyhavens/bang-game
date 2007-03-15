//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangFinancialAction;

/**
 * Creates and delivers a GangGood purchased by a gang.
 */
public abstract class GangGoodProvider extends GangFinancialAction
{
    /**
     * Creates and initializes the goods provider with the receiving gang
     * and the good to be created and delivered.
     */
    protected GangGoodProvider (GangObject gang, boolean admin, GangGood good, Object[] args)
    {
        super(gang, admin, good.getScripCost(), good.getCoinCost(), good.getAceCost());
        _good = good;
        _args = args;
    }

    /**
     * Configures this provider with its listener.
     */
    protected void setListener (InvocationService.ConfirmListener listener)
    {
        _listener = listener;
    }

    @Override // documentation inherited
    protected int getCoinType ()
    {
        return _good.getCoinType();
    }

    @Override // documentation inherited
    protected String getCoinDescrip ()
    {
        return MessageBundle.compose("m.good_purchase", _good.getName());
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

    protected GangGood _good;
    protected Object[] _args;
    protected InvocationService.ConfirmListener _listener;
}
