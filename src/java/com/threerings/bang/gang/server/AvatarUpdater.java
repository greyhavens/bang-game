//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.avatar.data.Look;

/**
 * Keeps the avatar of the gang's senior leader up-to-date while he's online.
 */
public class AvatarUpdater extends SetAdapter<DSet.Entry>
    implements ElementUpdateListener
{
    public AvatarUpdater (GangHandler handler)
    {
        _handler = handler;
    }

    public void add (PlayerObject player)
    {
        _player = player;
        _player.addListener(this);
        updateAvatar();
    }

    public void remove ()
    {
        _player.removeListener(this);
        _player = null;
    }

    @Override // documentation inherited
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(PlayerObject.LOOKS) &&
            event.getEntry() == _player.getLook(Look.Pose.WANTED_POSTER)) {
            updateAvatar();
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        if (event.getName().equals(PlayerObject.POSES) &&
            event.getIndex() == Look.Pose.WANTED_POSTER.ordinal()) {
            updateAvatar();
        }
    }

    protected void updateAvatar ()
    {
        Look look = _player.getLook(Look.Pose.WANTED_POSTER);
        _handler.getPeerProvider().setAvatar(
            null, _player.playerId, (look == null) ? null : look.getAvatar(_player));
    }

    protected GangHandler _handler;
    protected PlayerObject _player;
}
