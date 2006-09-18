//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Interface for sprites that can be targetted.
 */
public interface Targetable
{
    public static enum TargetMode { NONE, SURE_SHOT, MAYBE, KILL_SHOT };

    /**
     * Indicates that this sprite is targetted.
     */
    public void setTargeted (
            BangObject bangobj, TargetMode mode, Unit attacker);

    /**
     * Indicates that we have requested to shoot this piece but it is not
     * yet confirmed by the server.
     */
    public void setPendingShot (boolean pending);

    /**
     * Indicates that this target is within shooting range.
     */
    public void setPossibleShot (boolean possible);

    /**
     * Adds or removes an attacker from this sprite.
     */
    public void configureAttacker (int pidx, int delta);
}
