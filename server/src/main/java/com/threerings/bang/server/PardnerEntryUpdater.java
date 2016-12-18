//
// $Id$

package com.threerings.bang.server;

import java.util.Date;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.saloon.data.SaloonObject;

import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;

/**
 * Listens to users with pardners, updating their pardner list entries.
 */
public class PardnerEntryUpdater extends SetAdapter<DSet.Entry>
    implements AttributeChangeListener, ObjectDeathListener, ElementUpdateListener
{
    /** The up-to-date entry for the player. */
    public PardnerEntry entry;

    /**
     * Creates a new updater for the given player object and adds it as a listener.
     */
    public PardnerEntryUpdater (PlayerObject player)
    {
        _player = player;
        _player.addListener(this);

        entry = createPardnerEntry(player);
        updateAvatar();
        updateStatus();
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent ace)
    {
        String name = ace.getName();
        if (name.equals(PlayerObject.LOCATION)) {
            updateStatus();
            updateEntries();
        } else if (name.equals(PlayerObject.HANDLE)) {
            // rename the entry in all of our pardners' lists
            PardnerEntry oentry = entry;
            entry = (PardnerEntry)oentry.clone();
            entry.handle = _player.handle;
            for (PardnerEntry pard : _player.pardners) {
                PlayerObject pardner = BangServer.locator.lookupPlayer(pard.handle);
                if (pardner != null) {
                    pardner.startTransaction();
                    try {
                        pardner.removeFromPardners(oentry.handle);
                        pardner.addToPardners(entry);
                    } finally {
                        pardner.commitTransaction();
                    }
                }
            }
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent eue)
    {
        // if they select a new default look, update their avatar
        if (eue.getName().equals(PlayerObject.POSES) &&
            eue.getIndex() == Look.Pose.DEFAULT.ordinal()) {
            updateAvatar();
            updateEntries();
        }
    }

    @Override // documentation inherited
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> eue)
    {
        // if the current look is updated, update their avatar
        String name = eue.getName();
        if (name.equals(PlayerObject.LOOKS)) {
            Look look = (Look)eue.getEntry();
            if (look.name.equals(_player.getLook(Look.Pose.DEFAULT))) {
                updateAvatar();
                updateEntries();
            }
        } else if (name.equals(PlayerObject.PARDNERS) && shouldRemove()) {
            remove();
        }
    }

    @Override // documentation inherited
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> ere)
    {
        // if the last pardner is removed, clear out the updater
        if (ere.getName().equals(PlayerObject.PARDNERS) && shouldRemove()) {
            remove();
        }
    }

    // documentation inherited from interface ObjectDeathListener
    public void objectDestroyed (ObjectDestroyedEvent ode)
    {
        updateStatus();
        updateEntries();
        remove();
    }

    /**
     * Updates the {@link DSet}s that contain this entry.
     */
    public void updateEntries ()
    {
        for (PardnerEntry pard : _player.pardners) {
            PlayerObject pardner = BangServer.locator.lookupPlayer(pard.handle);
            if (pardner != null) {
                // this may be missing temporarily due to peer inconsistency
                if (pardner.pardners.containsKey(entry.handle)) {
                    pardner.updatePardners(entry);
                }
            }
        }
    }

    /**
     * Creates the entry object.
     */
    protected PardnerEntry createPardnerEntry (PlayerObject player)
    {
        return new PardnerEntry(player.handle);
    }

    /**
     * Checks whether this updater should be removed in response to a change in the dobj state.
     */
    protected boolean shouldRemove ()
    {
        // clear the updater when the last pardner is removed
        return (_player.getOnlinePardnerCount() == 0);
    }

    /**
     * Removes this updater from the map to which it was added and stops listening to the player
     * object.
     */
    protected void remove ()
    {
        _player.removeListener(this);
        unmap();
    }

    /**
     * Requests to remove this updater from any external mapping.
     */
    protected void unmap ()
    {
        BangServer.playmgr.clearPardnerEntryUpdater(_player.handle);
    }

    /**
     * Updates the entry avatar in response to a change in the player object.
     */
    protected void updateAvatar ()
    {
        Look look = _player.getLook(Look.Pose.DEFAULT);
        if (look != null) {
            entry.avatar = look.getAvatar(_player);
        }
    }

    /**
     * Updates the entry status in response to a change in the player object.
     */
    protected void updateStatus ()
    {
        if (_player.isActive()) {
            entry.gameOid = 0;
            DObject plobj = BangServer.omgr.getObject(_player.getPlaceOid());
            if (plobj instanceof BangObject) {
                if (((BangObject)plobj).bounty != null) {
                    entry.status = PardnerEntry.IN_BOUNTY;
                } else if (((BangObject)plobj).actionId != -1) {
                    entry.status = PardnerEntry.IN_TUTORIAL;
                } else {
                    entry.status = PardnerEntry.IN_GAME;
                }
                if (entry.status != PardnerEntry.IN_TUTORIAL) {
                    entry.gameOid = plobj.getOid();
                }
            } else if (plobj instanceof SaloonObject) {
                entry.status = PardnerEntry.IN_SALOON;
            } else {
                entry.setOnline(ServerConfig.townIndex);
            }

        } else {
            entry.status = PardnerEntry.OFFLINE;
            entry.avatar = null;
            entry.setLastSession(new Date());
        }
    }

    protected PlayerObject _player;
}
