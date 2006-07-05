//
// $Id$

package com.threerings.bang.lobby.server;

import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.server.TableManager;
import com.threerings.parlor.server.TableManagerProvider;

import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.game.server.scenario.ScenarioFactory;

import com.threerings.bang.lobby.data.LobbyObject;

/**
 * Takes care of the server side of a Bang! lobby.
 */
public class LobbyManager extends PlaceManager
    implements TableManagerProvider
{
    // documentation inherited from interface TableManagerProvider
    public TableManager getTableManager ()
    {
        return _tablemgr;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _lobobj = (LobbyObject)_plobj;
        _lobobj.addListener(_emptyListener);
        _lobobj.setTownId(ServerConfig.townId);
        _lobobj.setScenarios(ScenarioFactory.getScenarios(ServerConfig.townId));
        _tablemgr = new TableManager(this);
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
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return LobbyObject.class;
    }

    /** Listens for tables shutting down and reports us as empty if there
     * are no people in the lobby and our last table went away. */
    protected SetAdapter _emptyListener = new SetAdapter() {
        public void entryRemoved (EntryRemovedEvent event) {
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
