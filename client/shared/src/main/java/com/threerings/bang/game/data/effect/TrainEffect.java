//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.List;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Train;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.CollisionHandler;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a train moved, and possibly hit a unit.
 */
public class TrainEffect extends Effect
{
    /** The ids of the train pieces. */
    public int[] pieceIds;

    /** The new location of the head of the train. */
    public short nx, ny;

    /** The old location of the head of the train. */
    public short ox, oy;

    /** A new car to add at the end of the train, or <code>null</code> for none. */
    public Train ntail;

    /** The piece id of the target. */
    public int targetId;

    /** The x and y coordinates to which the target was pushed. */
    public short pushX, pushY;

    /** The target's death effect, if it died. */
    public Effect deathEffect;

    /** Constructor used when unserializing. */
    public TrainEffect ()
    {
    }

    /**
     * Constructor used when moving a train without a collision.
     */
    public TrainEffect (List<Train> pieces, int nx, int ny)
    {
        this(pieces, nx, ny, null, null);
    }

    /**
     * Constructor used when moving a train and colliding with a unit.
     */
    public TrainEffect (List<Train> pieces, int nx, int ny, Piece target, Point push)
    {
        pieceIds = new int[pieces.size()];
        for (int ii = 0; ii < pieceIds.length; ii++) {
            pieceIds[ii] = pieces.get(ii).pieceId;
        }
        this.nx = (short)nx;
        this.ny = (short)ny;
        if (target != null) {
            targetId = target.pieceId;
            this.pushX = (short)push.x;
            this.pushY = (short)push.y;
        }
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (targetId > 0) {
            if (deathEffect == null) {
                return ArrayUtil.append(pieceIds, targetId);
            } else {
                int[] deathIds = deathEffect.getAffectedPieces();
                int[] ids = new int[pieceIds.length + 1 + deathIds.length];
                System.arraycopy(pieceIds, 0, ids, 0, pieceIds.length);
                System.arraycopy(deathIds, 0, ids, pieceIds.length, deathIds.length);
                ids[pieceIds.length + deathIds.length] = targetId;
                return ids;
            }
        } else {
            return pieceIds;
        }
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return (deathEffect == null) ? NO_PIECES : deathEffect.getWaitPieces();
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Rectangle[] rects = new Rectangle[] {
            new Rectangle(ox, oy, 1, 1)
        };
        if (targetId > 0) {
            rects = ArrayUtil.append(rects, new Rectangle(pushX, pushY));
        }
        return rects;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // prepare the target and its death effect
        Piece target = bangobj.pieces.get(targetId);
        if (target != null) {
            int damage = Math.min(100, target.damage + COLLISION_DAMAGE);
            dammap.increment(target.owner, damage - target.damage);
            if (damage == 100 && target.damage < 100) {
                deathEffect = target.willDie(bangobj, -1);
                if (deathEffect != null) {
                    deathEffect.prepare(bangobj, dammap);
                }
            }
        }

        // find the current location of the last piece
        Train last = (Train)bangobj.pieces.get(pieceIds[0]);
        if (last != null) {
            ox = last.x;
            oy = last.y;
        } else {
            log.warning("Missing first train piece", "pieceId", pieceIds[0]);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // handle the collision, if any
        if (targetId > 0) {
            if (deathEffect != null) {
                deathEffect.apply(bangobj, obs);
            }
            Piece target = bangobj.pieces.get(targetId);
            _wasAlive = (target != null) && target.isAlive();
            collide(bangobj, obs, -1, -1, targetId, COLLISION_DAMAGE, pushX, pushY,
                    ShotEffect.DAMAGED);
        }

        // move the first train, if it has anywhere to go
        Train first = (Train)bangobj.pieces.get(pieceIds[0]);
        if (first == null) {
            log.warning("Missing first train piece", "pieceId", pieceIds[0]);
            return false;
        }
        if (nx == Train.UNSET) {
            removeAndReport(bangobj, first, obs);
            first.position(Train.UNSET, Train.UNSET); // suck the rest in
        } else if (first.nextX == nx && first.nextY == ny) {
            return true; // not going anywhere
        } else {
            moveAndReport(bangobj, first, nx, ny, obs, true);
        }

        // the rest of the train simply follows the first
        Train last = first;
        for (int ii = 1; ii < pieceIds.length; ii++) {
            Train train = (Train)bangobj.pieces.get(pieceIds[ii]);
            if (train == null) {
                log.warning("Missing train piece", "pieceId", pieceIds[ii]);
                return false;
            }
            moveAndReport(bangobj, train, last.x, last.y, obs, true);
            last = train;
        }

        // add the new tail piece, if there is one
        if (ntail != null) {
            addAndReport(bangobj, ntail, obs);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return (targetId > 0) ? new CollisionHandler() : new EffectHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return (targetId > 0) ? COLLISION_DAMAGE : 0;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(targetId);
        if (piece == null || piece.owner != pidx || !_wasAlive || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_train", piece.getName());
    }

    /** Whether or not the piece was alive before the train hit it. */
    protected boolean _wasAlive;

    /** The amount of damage taken by units hit by the train. */
    protected static final int COLLISION_DAMAGE = 50;
}
