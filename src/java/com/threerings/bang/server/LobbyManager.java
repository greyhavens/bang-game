//
// $Id$

package com.threerings.bang.server;

import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.server.TableManager;
import com.threerings.parlor.server.TableManagerProvider;

import com.threerings.bang.data.LobbyConfig;
import com.threerings.bang.data.LobbyObject;

/**
 * Takes care of the server side of a Bang! lobby.
 */
public class LobbyManager extends PlaceManager
    implements TableManagerProvider
{
    // documentation inherited from interface
    public TableManager getTableManager ()
    {
        return _tablemgr;
    }

    // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        _lconfig = (LobbyConfig)_config;
    }

    // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _lobobj = (LobbyObject)_plobj;
        _lobobj.addListener(_emptyListener);

        _tablemgr = new TableManager(this);
    }

    // documentation inherited
    protected void placeBecameEmpty ()
    {
        // we don't want to do the standard "became empty" processing
        // until all of our tables are also empty
        if (_lobobj.tableSet.size() == 0) {
            super.placeBecameEmpty();
        }
    }

//     // documentation inherited
//     protected long idleUnloadPeriod ()
//     {
//         // unload our lobbies very quickly after they become empty
//         return 15 * 1000L;
//     }

    // documentation inherited
    protected Class getPlaceObjectClass ()
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

    /** A casted reference to our lobby config. */
    protected LobbyConfig _lconfig;

    /** A casted reference to our lobby object. */
    protected LobbyObject _lobobj;

    /** Handles our table-based match making. */
    protected TableManager _tablemgr;
}
