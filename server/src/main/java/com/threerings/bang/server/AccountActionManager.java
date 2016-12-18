//
// $Id$

package com.threerings.bang.server;

import java.util.Iterator;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.util.Name;

import com.threerings.presents.annotation.AuthInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.user.AccountAction;
import com.threerings.user.depot.AccountActionRepository;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;

import static com.threerings.bang.Log.log;

/**
 * Handles account actions that are relevant to Bang! Howdy.
 */
@Singleton
public class AccountActionManager
{
    /**
     * Creates the action manager and prepares it for operation.
     */
    @Inject public AccountActionManager (PresentsDObjectMgr omgr,
                                         AccountActionRepository actionrepo)
        throws PersistenceException
    {
        _repo = actionrepo;

        new Interval(omgr) {
            public void expired () {
                handleNewAccountActions();
            }
        }.schedule(NEW_ACCOUNT_ACTION_INTERVAL, true);
    }

    /**
     * @return the AccountActionRepository used herein.
     */
    public AccountActionRepository getAccountActionRepository ()
    {
        return _repo;
    }

    /**
     * Get a list of new unseen accounting actions from the db and process them.
     */
    protected void handleNewAccountActions ()
    {
        if (_processingActions) {
            return; // don't start if the last cycle is still going!
        }
        _processingActions = true;

        _authInvoker.postUnit(new Invoker.Unit("getAccountActions") {
            public boolean invoke () {
                _actions = _repo.getActions(ServerConfig.nodename, MAX_ACTIONS);
                return true;
            }

            public void handleResult () {
                if (_actions != null) {
                    processActions(_actions); // will clear _processingActions
                } else {
                    _processingActions = false;
                }
            }

            protected List<AccountAction> _actions;
        });
    }

    /**
     * Iterate through a list of actions and process them.
     */
    protected void processActions (final List<AccountAction> actions)
    {
        for (Iterator<AccountAction> itr = actions.iterator(); itr.hasNext(); ) {
            AccountAction ba = itr.next();
            try {
                handleAccountAction(ba);
            } catch (Throwable t) {
                itr.remove(); // remove that action from our list
                log.warning("Failure handling account action, skipping.", "action", ba, t);
            }
        }

        // update the actions that were successfully processed.
        String uname = "updateActions:" + actions.size();
        _authInvoker.postUnit(new Invoker.Unit(uname) {
            public boolean invoke () {
                _repo.updateActions(actions, ServerConfig.nodename);
                return true;
            }

            public void handleResult () {
                // we've finally processed them all
                _processingActions = false;
            }
        });
    }

    /**
     * Handles each specific account action.
     */
    protected void handleAccountAction (AccountAction aa)
    {
        switch (aa.action) {
        case AccountAction.ACCOUNT_DELETED:
            disableAccount(aa.accountName, aa.data);
            break;

        default:
            log.warning("Unknown account action", "action", aa);
        }
    }

    /**
     * Note that an account has been deleted. Disables the associated player record.
     */
    protected void disableAccount (final String accountName, final String disabledName)
    {
        BangServer.invoker.postUnit(new Invoker.Unit("disableAccount") {
            public boolean invoke () {
                try {
                    _playrepo.disablePlayer(accountName, disabledName);
                } catch (PersistenceException pe) {
                    log.warning("Error disabling account", "oname", accountName,
                                "dname", disabledName, "cause", pe);
                }
                return false;
            }
        });
    }

    /** Access to our accounting actions database. */
    protected AccountActionRepository _repo;

    /** True if we're currently processing actions and should not start more. */
    protected boolean _processingActions = false;

    // dependencies
    @Inject protected @AuthInvoker Invoker _authInvoker;
    @Inject protected PlayerRepository _playrepo;

    /** Interval with which we check for new accounting actions. */
    protected static final int NEW_ACCOUNT_ACTION_INTERVAL = 30 * 1000;

    /** The maximum number of account actions we process each interval. */
    protected static final int MAX_ACTIONS = 50;
}
