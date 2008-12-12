//
// $Id$

package com.threerings.bang.server;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsInvoker;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.bang.server.persist.FinancialAction;

/**
 * Extends the {@link PresentsInvoker} with just a bit more stuff!
 */
@Singleton
public class BangInvoker extends PresentsInvoker
{
    @Inject public BangInvoker (PresentsDObjectMgr omgr, ShutdownManager shutmgr,
                                ReportManager repmgr)
    {
        super(omgr, shutmgr, repmgr);
    }

    /**
     * Posts a financial action for processing.
     */
    public void post (FinancialAction action)
        throws InvocationException
    {
        // resolve this action's dependencies
        _injector.injectMembers(action);
        // prepare the action for operation (which may throw invocation exception to indicate
        // failure or return false to indicate that it should not be posted)
        if (action.checkStart()) {
            // finally post the action to our queue (need super to bypass sanity check)
            super.postUnit(action);
        }
    }

    @Override // from Invoker
    public void postUnit (Unit unit)
    {
        // sanity check; we could just initialize the unit here and make this all automagic, but
        // I'd prefer that developers clearly see what's going on at the call site
        if (unit instanceof FinancialAction) {
            throw new IllegalArgumentException("You must call post(FinancialAction).");
        }
        super.postUnit(unit);
    }

    @Inject protected Injector _injector;
}
