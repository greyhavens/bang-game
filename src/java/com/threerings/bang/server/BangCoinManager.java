//
// $Id$

package com.threerings.bang.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.AuditLogger;

import com.threerings.coin.server.CoinManager;

/**
 * Customizes the (microcurrency) coin manager for Bang! Howdy.
 */
public class BangCoinManager extends CoinManager
{
    /** An audit log for coin related information. */
    public static AuditLogger coinlog = BangServer.createAuditLog("coin.log");

    /**
     * Creates the coin manager and its associated repository.
     */
    public BangCoinManager (ConnectionProvider conprov)
        throws PersistenceException
    {
        super(conprov, ServerConfig.serverName, coinlog, BangServer.actionrepo,
              BangServer.invoker);
    }
}
