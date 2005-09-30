//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.Invoker;

import com.threerings.coin.server.CoinManager;

import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

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

    /**
     * Updates the coin count published in the specified player's {@link
     * PlayerObject} with the latest data from the coin database.
     */
    public void updateCoinCount (final PlayerObject user)
    {
        _invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _coins = _coinRepo.getCoinCount(user.accountName.toString());
                } catch (PersistenceException pe) {
                    _err = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_err != null) {
                    log.log(Level.WARNING, "Error updating coin count for " +
                            user.who() + ".", _err);
                } else {
                    user.setCoins(_coins);
                }
            }

            protected PersistenceException _err;
            protected int _coins;
        });
    }
}
