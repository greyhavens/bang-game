//
// $Id$

package com.threerings.bang.lobby.server;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.server.TableManager;

import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.lobby.data.LobbyObject;

/**
 * Takes care of the server side of a Bang! lobby.
 */
public class LobbyManager extends PlaceManager
{
    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _lobobj = (LobbyObject)_plobj;
        _lobobj.addListener(_emptyListener);
        _lobobj.setTownId(ServerConfig.townId);
        _lobobj.setScenarios(ScenarioInfo.getScenarioIds(ServerConfig.townId, false));

        // create a manager for our tables
        _tablemgr = new TableManager(_omgr, _invmgr, _registry, getPlaceObject());
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // shutdown our table manager
        _tablemgr.shutdown();
    }

    @Override // documentation inherited
    protected void placeBecameEmpty ()
    {
        // we don't want to do the standard "became empty" processing
        // until all of our tables are also empty
        if (_lobobj.tableSet.size() == 0) {
            super.placeBecameEmpty();
        }
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new LobbyObject();
    }

    /** Listens for tables shutting down and reports us as empty if there
     * are no people in the lobby and our last table went away. */
    protected SetAdapter<DSet.Entry> _emptyListener = new SetAdapter<DSet.Entry>() {
        public void entryRemoved (EntryRemovedEvent<DSet.Entry> event) {
            if (event.getName().equals(LobbyObject.TABLE_SET)) {
                if (_lobobj.tableSet.size() == 0 &&
                    _lobobj.occupants.size() == 0) {
                    placeBecameEmpty();
                }
            }
        }
    };

    /** A casted reference to our lobby object. */
    protected LobbyObject _lobobj;

    /** Handles our table-based match making. */
    protected TableManager _tablemgr;
}
