//
// $Id$

package com.samskivert.bang.data.effect;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;

/**
 * Communicates that a shot was fired from one piece to another.
 */
public class ShotEffect extends Effect
{
    public int shooterId;

    public int targetId;

    public int damage;

    public void prepare (BangObject bangobj)
    {
        // nada
    }

    public void apply (BangObject bangobj, Observer observer)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target != null) {
            target.damage = Math.min(100, target.damage + damage);
        } else {
            log.warning("Missing shot target " + this + ".");
        }
    }
}
