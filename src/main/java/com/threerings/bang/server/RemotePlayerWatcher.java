//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.EntryReplacedEvent;
import com.threerings.bang.data.Handle;

import static com.threerings.bang.Log.log;

/**
 * Handles the process of watching for remote player logons and logoffs and propagating online
 * information for those players into local objects. This is used by the Pardner and Gang systems.
 */
public abstract class RemotePlayerWatcher<T extends DSet.Entry>
    implements BangPeerManager.RemotePlayerObserver
{
    public interface Container<T>
    {
        public DObject getObject ();

        public Iterable<T> getEntries ();

        public T getEntry (Handle key);

        public void updateEntry (T entry);

        public void renameEntry (T entry, Handle newHandle);
    }

    /**
     * Registers a listener on the distributed object provided by the supplied container. The
     * listener persists for the lifetime of the object. When the object is destroyed the entries
     * it contains will be unmapped.
     */
    public void registerListener (Container<T> container)
    {
        container.getObject().addListener(new Mapper(container));
    }

    // from interface BangPeerManager.RemotePlayerObserver
    public void remotePlayerLoggedOn (int townIndex, BangClientInfo info)
    {
        updateRemotePlayer(info, townIndex, "on");
    }

    // from interface BangPeerManager.RemotePlayerObserver
    public void remotePlayerLoggedOff (int townIndex, BangClientInfo info)
    {
        updateRemotePlayer(info, -1, "off");
    }

    // from interface BangPeerManager.RemotePlayerObserver
    public void remotePlayerChangedHandle (int townIndex, Handle oldHandle, Handle newHandle)
    {
        List<Container<T>> containers = _mapping.remove(oldHandle);
        if (containers == null) {
            return;
        }
        _mapping.put(newHandle, containers);

        for (Container<T> cont : containers) {
            T entry = cont.getEntry(oldHandle);
            if (entry == null) {
                log.warning("Player registered but missing entry", "cont", cont,
                            "oldHandle", oldHandle, "newHandle", newHandle);
                continue; // weirdness?
            }
            cont.renameEntry(entry, newHandle);
        }
    }

    /**
     * Called when a player logs onto or off of a remote server. Updates that player's pardner
     * entry for any player online on this server that has the remote player as a pardner.
     */
    protected void updateRemotePlayer (BangClientInfo info, int townIndex, String where)
    {
        if (!(info.visibleName instanceof Handle)) {
            // they haven't yet created their character
            return;
        }

        Handle handle = (Handle)info.visibleName;
        List<Container<T>> containers = _mapping.get(handle);
        if (containers == null) {
            return;
        }

        for (Container<T> cont : containers) {
            T entry = cont.getEntry(handle);
            if (entry == null) {
                log.warning("Player registered but missing entry", "cont", cont, "player", handle,
                            "where", where);
                continue; // weirdness?
            }
            updateEntry(info, townIndex, entry);
            cont.updateEntry(entry);
        }
    }

    /**
     * Returns the name of the {@link DSet} in which the entries are contained.
     */
    protected abstract String getSetName ();

    /**
     * Updates the supplied entry based on the supplied remote player information.
     *
     * @param townIndex the index of the town to which the player just logged on or -1 if the
     * player just logged off of a remote town.
     */
    protected abstract void updateEntry (BangClientInfo info, int townIndex, T entry);

    /** Used to keep the {@link #_mapping} mapping up to date. */
    protected class Mapper
        implements SetListener<DSet.Entry>, ObjectDeathListener
    {
        public Mapper (Container<T> container) {
            _container = container;
            for (T entry : _container.getEntries()) {
                mapPlayer(entry);
            }
        }

        public void entryAdded (EntryAddedEvent<DSet.Entry> event) {
            // replacement handles will have already been mapped
            if (event.getName().equals(getSetName()) &&
                    !(event instanceof EntryReplacedEvent.ReplacementAddedEvent)) {
                @SuppressWarnings("unchecked") T entry = (T)event.getEntry();
                mapPlayer(entry);
            }
        }
        public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event) {
            // nothing doing
        }
        public void entryRemoved (EntryRemovedEvent<DSet.Entry> event) {
            // replaced handles will have already been cleared
            if (event.getName().equals(getSetName()) &&
                    !(event instanceof EntryReplacedEvent)) {
                @SuppressWarnings("unchecked") T entry = (T)event.getOldEntry();
                if (entry == null) { // sanity check
                    log.warning("Missing old entry", "event", event, "container", _container);
                } else {
                    clearPlayer(entry);
                }
            }
        }

        public void objectDestroyed (ObjectDestroyedEvent event) {
            for (T entry : _container.getEntries()) {
                clearPlayer(entry);
            }
        }

        protected void mapPlayer (T entry) {
            List<Container<T>> list = _mapping.get(entry.getKey());
            if (list == null) {
                _mapping.put(entry.getKey(), list = new ArrayList<Container<T>>());
            }
            if (list.remove(_container)) { // sanity check
                log.warning("Found stale mapping", "entry", entry.getKey(), "container", _container);
            }
            list.add(_container);
        }
        protected void clearPlayer (T entry) {
            List<Container<T>> list = _mapping.get(entry.getKey());
            if (list == null) { // sanity check
                log.warning("Missing list when clearing mapping", "entry", entry.getKey(),
                            "container", _container);
            } else if (!list.remove(_container)) { // remove and sanity check
                log.warning("Missing player when clearing mapping", "entry", entry.getKey(),
                            "container", _container);
            } else if (list.isEmpty()) {
                _mapping.remove(entry.getKey());
            }
        }

        protected Container<T> _container;
    }

    /** Maps handles to a list of all resolved objects that contain a reference to it. */
    protected Map<Comparable<?>, List<Container<T>>> _mapping = Maps.newHashMap();
}
