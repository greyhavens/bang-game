//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.AuditLogger;
import com.samskivert.util.Invoker;

import com.threerings.coin.server.CoinManager;
import com.threerings.coin.server.persist.CoinTransaction;
import com.threerings.user.AccountActionRepository;

import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Customizes the (microcurrency) coin manager for Bang! Howdy.
 */
public class BangCoinManager extends CoinManager
{
    /** An audit log for coin related information. */
    public static AuditLogger coinlog = BangServer.createAuditLog("coin");

    /**
     * Creates the coin manager and its associated repository.
     */
    public BangCoinManager (ConnectionProvider conprov,
                            AccountActionRepository actionrepo)
        throws PersistenceException
    {
        super(conprov, ServerConfig.nodename, coinlog, actionrepo,
              // the bang coin tables are on the bang databases, so we can use the game invoker
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
                    _coins = _coinRepo.getCoinCount(user.username.toString());
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

    /**
     * Grants coins to the specified player. This is giving away free money, so don't do this
     * unless you know what you are doing.
     */
    public void grantRewardCoins (final PlayerObject user, final int coins)
    {
        final String accountName = user.username.toString();
        _invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _coinRepo.addCoins(accountName, coins, CoinTransaction.PROMOTIONAL_GRANT,
                                       "m.reward_grant");
                    _coins = _coinRepo.getCoinCount(accountName);
                } catch (PersistenceException pe) {
                    _err = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_err != null) {
                    log.log(Level.WARNING, "Error granting reward coins to " +
                            accountName + ".", _err);
                } else {
                    user.setCoins(_coins);
                }
            }

            protected PersistenceException _err;
            protected int _coins;
        });
    }
}
