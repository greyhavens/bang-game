//
// $Id$

package com.threerings.bang.server;

import java.sql.Date;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.presents.server.Authenticator;

/**
 * Extends the standard authenticator with some extra bits.
 */
public abstract class BangAuthenticator extends Authenticator
{
    /**
     * Called to indicate that an account has become an active Bang! player (played for the first
     * time) or is no longer an active Bang! player (their Bang! data has been purged).
     */
    public abstract void setAccountIsActive (String username, boolean isActive)
        throws PersistenceException;

    /**
     * Creates an account in this authenticator's user database.
     */
    public abstract String createAccount (String username, String password, String email,
                                          String affiliate, String machIdent, Date birthdate)
        throws PersistenceException;

    /**
     * Checks whether the specified player has any pending rewards, redeeming them in the process.
     */
    public abstract ArrayList<String> redeemRewards (String username, String ident);

    @Override // from Authenticator
    protected Invoker getInvoker ()
    {
        return BangServer.authInvoker;
    }
}
