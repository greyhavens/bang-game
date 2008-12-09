//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.ToggleSwitchEffectHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.SafeMarker;
import com.threerings.bang.game.data.piece.ToggleSwitch;

import static com.threerings.bang.Log.log;

/**
 * An effect that works on a ToggleSwitch.
 */
public class ToggleSwitchEffect extends Effect
{
    /** The effect generated on the {@link ToggleSwitch} pieces. */
    public static final String SWITCH_TOGGLED = "indian_post/switch_toggled";

    /** Changes to the last acted tick. */
    public short tick;

    /** Piece ID for the affected switch. */
    public int switchId;

    /** Piece IDs for other affected switches. */
    public int[] switchIds;

    /** State to change the toggle switch to. */
    public ToggleSwitch.State state;

    /** Id of the activator or -1 if none. */
    public int activator = -1;

    /** Id of the occupier or -1 if none. */
    public int occupier = -1;

    @Override // documentation inherited
    public int [] getWaitPieces ()
    {
        if (occupier != -1) {
            return new int[] { occupier };
        }
        return super.getWaitPieces();
    }

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (switchIds != null) {
            int[] pieces = switchIds.clone();
            pieces = ArrayUtil.append(pieces, switchId);
            return pieces;
        }
        return new int[] { switchId };
    }

    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (state == null) {
            return;
        }

        ArrayIntSet switches = new ArrayIntSet();
        for (Piece p : bangobj.pieces) {
            if (p instanceof ToggleSwitch && p.pieceId != switchId &&
                    ((ToggleSwitch)p).state != state) {
                switches.add(p.pieceId);
            }
        }
        if (switches.size() > 0) {
            switchIds = switches.toIntArray();
        }
    }

    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        ToggleSwitch ts = (ToggleSwitch)bangobj.pieces.get(switchId);
        if (ts == null) {
            log.warning("Missing target to toggle switch effect", "id", switchId);
            return false;
        }
        if (tick > 0) {
            ts.lastActed = tick;
        }
        ts.activator = activator;
        if (tick == 0) {
            ts.occupier = occupier;
        } else {
            ts.occupier = -1;
        }
        if (state != null) {
            ts.state = state;
            for (Piece p : bangobj.pieces) {
                if (p instanceof SafeMarker) {
                    ((SafeMarker)p).setSquare(
                            state == ToggleSwitch.State.SQUARE);
                }
            }
        }

        reportEffect(obs, ts, SWITCH_TOGGLED);
        if (switchIds == null) {
            return true;
        }

        for (int sid : switchIds) {
            ts = (ToggleSwitch)bangobj.pieces.get(sid);
            if (ts == null) {
                log.warning("Missing target for toggle effect", "id", sid);
                return false;
            }
            ts.state = state;
            reportEffect(obs, ts, SWITCH_TOGGLED);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new ToggleSwitchEffectHandler();
    }
}
