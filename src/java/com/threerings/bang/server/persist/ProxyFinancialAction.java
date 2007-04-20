//
// $Id$

package com.threerings.bang.server.persist;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;

/**
 * Handles a financial action undertaken on a peer node.
 */
public abstract class ProxyFinancialAction extends FinancialAction
    implements InvocationService.ConfirmListener
{
    @Override // documentation inherited
    public void start ()
        throws InvocationException
    {
        lockAndDeduct();
        forwardRequest();
    }

    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestProcessed ()
    {
        handleResult();
    }

    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestFailed (String cause)
    {
        _failmsg = cause;
        handleResult();
    }

    protected ProxyFinancialAction (PlayerObject user, int scripCost, int coinCost)
    {
        super(user, scripCost, coinCost);
    }

    @Override // documentation inherited
    protected void actionCompleted () {
        // we'll log this on the server which performs the action
    }

    protected String getGoodType ()
    {
        return null;
    }

    /**
     * Forwards the request to the peer with this object as its listener.
     */
    protected abstract void forwardRequest ()
        throws InvocationException;
}
