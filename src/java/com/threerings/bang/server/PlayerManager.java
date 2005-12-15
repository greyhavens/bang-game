//
// $Id$

package com.threerings.bang.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ListUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.util.Name;

import com.threerings.bang.ranch.data.RanchCodes;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;

import static com.threerings.bang.Log.log;

/**
 * Handles general player business, implements {@link PlayerProvider}.
 */
public class PlayerManager
    implements PlayerProvider
{
    /**
     * Initializes the player manager, and registers its invocation service.
     */
    public void init ()
    {
        // register ourselves as the provider of the (bootstrap) PlayerService
        BangServer.invmgr.registerDispatcher(new PlayerDispatcher(this), true);
    }

    // documentation inherited from interface PlayerProvider
    public void pickFirstBigShot (ClientObject caller, String type, Name name,
                                  final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // sanity check: make sure they don't already have a big shot
        if (user.hasBigShot()) {
            log.warning("Player requested free big shot but already has one " +
                        "[who=" + user.who() + ", inventory=" + user.inventory +
                        ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // sanity check: make sure the big shot is valid
        UnitConfig config = UnitConfig.getConfig(type);
        if (config == null ||
            ListUtil.indexOf(RanchCodes.STARTER_BIGSHOTS, config.type) == -1) {
            log.warning("Player requested invalid free big shot " +
                        "[who=" + user.who() + ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // create the BigShot item and stuff it on into their inventory
        final BigShotItem bsitem = new BigShotItem(user.playerId, config.type);
        bsitem.setName(name);

        // stick the new item in the database and in their inventory
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                BangServer.itemrepo.insertItem(bsitem);
            }
            public void handleSuccess () {
                user.addToInventory(bsitem);
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to add first big shot to repository " +
                    "[who=" + user.who() + ", item=" + bsitem + "]";
            }
        });
    }
}
