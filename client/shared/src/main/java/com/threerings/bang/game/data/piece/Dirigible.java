//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CrashEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Handles some special behavior needed for the Dirigible.
 */
public class Dirigible extends Unit
{
    @Override // documentation inherited
    public Point maybeCrash (BangObject bangobj, int shooter)
    {
        // prefer crashing into units on the same team as the shooter
        ArrayList<Piece> shooterPieces = new ArrayList<Piece>();
        // however crash into someone if we can
        ArrayList<Piece> otherPieces = new ArrayList<Piece>();
        for (Piece piece : bangobj.pieces) {
            if (piece.isAlive() && piece.isTargetable() &&
                getDistance(piece) <= getMoveDistance()) {
                if (piece.owner == shooter) {
                    shooterPieces.add(piece);
                } else if (!piece.isSameTeam(bangobj, this)) {
                    otherPieces.add(piece);
                }
            }
        }

        Random rand = new Random(bangobj.tick);
        if (!shooterPieces.isEmpty()) {
            _deathTarget = shooterPieces.get(rand.nextInt(shooterPieces.size()));
        } else if (!otherPieces.isEmpty()) {
            _deathTarget = otherPieces.get(rand.nextInt(otherPieces.size()));
        }
        if (_deathTarget != null) {
            return new Point (_deathTarget.x, _deathTarget.y);
        }

        return bangobj.board.getOccupiableSpot(x, y, 1, 5, rand);
    }

    @Override // documentation inherited
    public Effect didDie (BangObject bangobj)
    {
        CrashEffect effect = null;
        if (_deathTarget != null) {
            effect = new CrashEffect(_deathTarget, _deathTarget.adjustDefend(this, 25), this);
        }
        return effect;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return false;
    }

    @Override // documentation inherited
    public boolean rebuildShadow ()
    {
        return true;
    }

    protected transient Piece _deathTarget;
}
