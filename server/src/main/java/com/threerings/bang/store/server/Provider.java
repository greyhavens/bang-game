//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.store.data.Good;

/**
 * Creates and delivers a Good when purchased by a player from the General Store. At a minimum, a
 * provider will need to implement {@link #persistentAction} and {@link #rollbackPersistentAction}.
 * It may also override {@link #actionCompleted} and/or {@link #actionFailed} if additional
 * post-persistent actions need to be taken.
 */
public abstract class Provider extends FinancialAction
{
    /**
     * Configures this provider with its listener. This is called shortly after the provider is
     * constructed and would be passed along with the normal constructor except that doing so
     * results in this really long argument being necessary in numerous derived constructors and in
     * the various factory methods that create our providers.
     */
    public void setListener (InvocationService.ConfirmListener listener)
    {
        _listener = listener;
    }

    /**
     * Creates and initializes the goods provider with the receiving user and the good to be
     * created and delivered. The constructor should throw an invocation exception if it is known
     * immediately that the product cannot be created. It is not necessary to check for sufficient
     * funds in the constructor, that will be done elsewhere.
     */
    protected Provider (PlayerObject user, Good good, Object[] args)
    {
        // admins and support get things for free
        super(user, user.tokens.isSupport() ? 0 : good.getScripCost(),
              user.tokens.isSupport() ? 0 : good.getCoinCost(user));
        _good = good;
        _args = args;
    }

    @Override // documentation inherited
    protected String getCoinDescrip () {
        return MessageBundle.compose("m.good_purchase", _good.getName());
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _listener.requestProcessed();
        super.actionCompleted();
    }

    @Override // documentation inherited
    protected void actionFailed (String cause)
    {
        _listener.requestFailed(cause);
    }

    @Override // documentation inherited
    protected String getPurchaseType ()
    {
        return "store";
    }

    // documentation inherited
    protected String getGoodType ()
    {
        return _good.getClass().getSimpleName();
    }

    protected Good _good;
    protected Object[] _args;
    protected InvocationService.ConfirmListener _listener;
}
