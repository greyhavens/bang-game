//
// $Id$

package com.threerings.bang.server;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Interval;
import com.samskivert.util.ObserverList;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.BodyLocator;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.admin.server.BangAdminManager;

/**
 * Customizes the {@link BodyLocator} and provides a means to lookup a member by id.
 */
@Singleton @EventThread
public class PlayerLocator extends BodyLocator
{
    /**
     * Implemented by objects that wish to be notified when players log on and off, or changes
     * their handle.
     */
    public static interface PlayerObserver
    {
        /** Called when a player logs on. */
        public void playerLoggedOn (PlayerObject user);

        /** Called when a player logs off. */
        public void playerLoggedOff (PlayerObject user);

        /** Called when a player changes their handle. */
        public void playerChangedHandle (PlayerObject user, Handle oldHandle);
    }

    /**
     * Initializes the player locator.
     */
    public void init ()
    {
        // creates interval that updates the town object's population once every thirty seconds
        new Interval(_omgr) {
            public void expired () {
                int npop = _players.size();
                if (npop != BangServer.townobj.population) {
                    BangServer.townobj.setPopulation(npop);
                }
            }
        }.schedule(30000L, true);
    }

    /**
     * Registers a player observer.
     */
    public void addPlayerObserver (PlayerObserver observer)
    {
        _playobs.add(observer);
    }

    /**
     * Removes a player observer registration.
     */
    public void removePlayerObserver (PlayerObserver observer)
    {
        _playobs.remove(observer);
    }

    /**
     * Returns the player object for the specified user if they are online currently, null
     * otherwise.
     */
    public PlayerObject lookupPlayer (Handle handle)
    {
        _omgr.requireEventThread();
        return _players.get(handle);
    }

    /**
     * Returns the player object for the specified id if they are online currently, null
     * otherwise.
     */
    public PlayerObject lookupPlayer (int playerId)
    {
        _omgr.requireEventThread();
        return _playerIds.get(playerId);
    }

    /**
     * Returns the player object for the specified user if they are online currently, null
     * otherwise.
     */
    public PlayerObject lookupByAccountName (Name accountName)
    {
        return (PlayerObject)_clmgr.getClientObject(accountName);
    }

    /**
     * Called when a player starts their session to associate the handle with the player's
     * distributed object.
     */
    public void registerPlayer (final PlayerObject player)
    {
        _players.put(player.handle, player);
        _playerIds.put(player.playerId, player);

        // update our players online count in the status object
        _adminmgr.statobj.updatePlayersOnline(_clmgr.getClientCount());

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerLoggedOn(player);
                return true;
            }
        });
    }

    /**
     * Called when a player sets their handle for the first time, or changes it later on,
     * to change the handle association.
     */
    public void updatePlayer (final PlayerObject player, final Handle oldHandle)
    {
        _players.remove(oldHandle);
        _players.put(player.handle, player);

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerChangedHandle(player, oldHandle);
                return true;
            }
        });
    }

    /**
     * Called when a player ends their session to clear their handle to player object mapping.
     */
    public void clearPlayer (final PlayerObject player)
    {
        _players.remove(player.handle);
        _playerIds.remove(player.playerId);

        // update our players online count in the status object
        _adminmgr.statobj.updatePlayersOnline(_clmgr.getClientCount());

        // notify our player observers
        _playobs.apply(new ObserverList.ObserverOp<PlayerObserver>() {
            public boolean apply (PlayerObserver observer) {
                observer.playerLoggedOff(player);
                return true;
            }
        });
    }

    @Override // from BodyLocator
    public BodyObject lookupBody (Name visibleName)
    {
        _omgr.requireEventThread();
        return _players.get(visibleName);
    }

    protected Map<Handle,PlayerObject> _players = Maps.newHashMap();
    protected IntMap<PlayerObject> _playerIds = IntMaps.newHashIntMap();

    protected ObserverList<PlayerObserver> _playobs = ObserverList.newFastUnsafe();

    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ClientManager _clmgr;
    @Inject protected BangAdminManager _adminmgr;
}
