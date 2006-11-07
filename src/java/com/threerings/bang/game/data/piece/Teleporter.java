//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.TeleportEffect;

/**
 * A prop that teleports units that land on it to another location (the
 * location of another teleporter of the same type).
 */
public class Teleporter extends Prop
{
    @Override // documentation inherited
    public boolean isOmissible (BangBoard board)
    {
        // although passable, it still has an effect
        return !intersects(board.getPlayableArea()); 
    }
    
    @Override // documentation inherited
    public boolean isPassable ()
    {
        return true; // teleporters are always passable
    }

    @Override // documentation inherited
    public boolean shadowBonus ()
    {
        return true;
    }
    
    /**
     * Called when a piece has landed on this teleporter and is teleporting
     * to a new location.
     */
    public TeleportEffect affect (BangObject bangobj, Piece piece)
    {
        return new TeleportEffect(this, piece);
    }

    /**
     * Returns the lazily initialized array of teleporters with the same prop
     * type, including this one.
     */ 
    public Teleporter[] getGroup (BangObject bangobj)
    {
        if (_group == null) {
            ArrayList<Teleporter> group = new ArrayList<Teleporter>();
            for (Teleporter teleporter : bangobj.getTeleporters().values()) {
                if (teleporter.getType().equals(getType())) {
                    group.add(teleporter);
                }
            }
            _group = group.toArray(new Teleporter[group.size()]);
            for (Teleporter teleporter : _group) {
                teleporter._group = _group;
            }
        }
        return _group;
    }
    
    /**
     * Returns the name of one of this teleporter's effects.
     *
     * @param type the type of effect (e.g., "activate" or "travel")
     */
    public String getEffect (String type)
    {
        String town = _type.substring(0, _type.indexOf('/') + 1),
            name = _type.substring(_type.lastIndexOf('/') + 1);
        return town + name + "/" + type;
    }
    
    /** The teleporters with the same prop type, including this one. */
    protected transient Teleporter[] _group;
}
