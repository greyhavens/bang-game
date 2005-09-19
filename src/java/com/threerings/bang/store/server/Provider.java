//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.store.data.Good;

/**
 * Creates and delivers a Good when purchased by a player from the General
 * Store. At a minimum, a provider will need to implement {@link
 * #persistentAction} and {@link #rollbackPersistentAction}. It may also
 * override {@link #actionCompleted} and/or {@link #actionFailed} if
 * additional post-persistent actions need to be taken.
 */
public abstract class Provider extends FinancialAction
{
    /**
     * Creates and initializes the goods provider with the receiving user
     * and the good to be created and delivered. The constructor should
     * throw an invocation exception if it is known immediately that the
     * product cannot be created. It is not necessary to check for
     * sufficient funds in the constructor, that will be done elsewhere.
     */
    protected Provider (BangUserObject user, Good good)
        throws InvocationException
    {
        super(user, good.getScripCost(), good.getGoldCost());
        _good = good;
    }

    /**
     * Configures this provider with its listener. This is called shortly
     * after the provider is constructed and would be passed along with
     * the normal constructor except that doing so results in this really
     * long argument being necessary in numerous derived constructors and
     * in the various factory methods that create our providers.
     */
    protected void setListener (InvocationService.ConfirmListener listener)
    {
        _listener = listener;
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _listener.requestProcessed();
    }

    @Override // documentation inherited
    protected void actionFailed ()
    {
        _listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
    }

    protected Good _good;
    protected InvocationService.ConfirmListener _listener;
}
