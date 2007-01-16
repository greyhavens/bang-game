//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.AuditLogger;

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
    public BangCoinManager (ConnectionProvider conprov, AccountActionRepository actionrepo)
        throws PersistenceException
    {
        // the bang coin tables are on the bang databases, so we can use the game invoker
        super(conprov, ServerConfig.nodename, coinlog, actionrepo, BangServer.invoker);
    }

    /**
     * Updates the coin count published in the specified player's {@link PlayerObject} with the
     * latest data from the coin database.
     */
    public void updateCoinCount (final PlayerObject user)
    {
        _invoker.postUnit(new RepositoryUnit() {
            public void invokePersist () throws Exception {
                _coins = _coinRepo.getCoinCount(user.username.toString());
            }
            public void handleSuccess () {
                user.setCoins(_coins);
            }
            public void handleFailure (Exception err) {
                log.log(Level.WARNING, "Error updating coin count for " + user.who() + ".", err);
            }
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
        _invoker.postUnit(new RepositoryUnit() {
            public void invokePersist () throws Exception {
                _coinRepo.addCoins(
                    accountName, coins, CoinTransaction.PROMOTIONAL_GRANT, "m.reward_grant");
                _coins = _coinRepo.getCoinCount(accountName);
            }
            public void handleSuccess () {
                user.setCoins(_coins);
            }
            public void handleFailure (Exception err) {
                log.log(Level.WARNING, "Error granting reward coins to " + accountName + ".", err);
            }
            protected int _coins;
        });
    }
}
