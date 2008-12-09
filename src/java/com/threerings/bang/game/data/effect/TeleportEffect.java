//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.TeleportHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * An effect deployed when a unit steps on a teleporter.
 */
public class TeleportEffect extends Effect
{
    /** Indicates that a piece was teleported. */
    public static final String TELEPORTED = "indian_post/teleported";

    /** The id of the teleported piece. */
    public int pieceId;

    /** The coordinates to which the piece will be moved. */
    public short[] dest;

    /** The id of the source teleporter. */
    public int sourceId;

    /** The damage effect if we're looping. */
    public DamageEffect damageEffect;

    public TeleportEffect ()
    {
    }

    public TeleportEffect (Teleporter teleporter, Piece piece)
    {
        sourceId = teleporter.pieceId;
        pieceId = piece.pieceId;
    }

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (damageEffect == null) {
            return new int[] { pieceId };
        }
        return damageEffect.getAffectedPieces();
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Teleporter source = (Teleporter)bangobj.pieces.get(sourceId);
        if (source == null) {
            log.warning("Missing source teleporter for teleport effect", "id", sourceId);
            return;
        }
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing piece for teleporter effect", "id", pieceId);
            return;
        }

        // select a random destination teleporter
        Teleporter[] group = source.getGroup(bangobj);
        ArrayList<Teleporter> dests = new ArrayList<Teleporter>();
        for (Teleporter tport : group) {
            if (!tport.equals(source)) {
                dests.add(tport);
            }
        }
        Collections.shuffle(dests);
        Point spot = null;

        // find a destination
        for (Teleporter dest : dests) {
            spot = bangobj.board.getOccupiableSpot(dest.x, dest.y, 2);
            if (spot != null) {
                piece.teleMoves = null;
                break;
            }
        }
        // if no destination found, see if we can land on another teleporter
        if (spot == null) {
            PointSet teleporters = new PointSet();
            for (Piece p : bangobj.pieces) {
                if (p instanceof Teleporter &&
                        bangobj.board.isGroundOccupiable(p.x, p.y)) {
                    for (Teleporter dest : dests) {
                        int dist = dest.getDistance(p);
                        if (dist > 0 && dist <= 2) {
                            teleporters.add(p.x, p.y);
                            break;
                        }
                    }
                }
            }
            if (teleporters.size() > 0) {
                int idx = RandomUtil.getInt(teleporters.size());
                spot = new Point(teleporters.getX(idx), teleporters.getY(idx));
                if (piece.teleMoves == null) {
                    piece.teleMoves = new PointSet();
                }
                // if we're looping, the piece dies
                if (piece.teleMoves.contains(spot.x, spot.y)) {
                    damageEffect = new DamageEffect(piece, 100);
                    damageEffect.prepare(bangobj, dammap);
                    piece.teleMoves = null;
                } else {
                    piece.teleMoves.add(spot.x, spot.y);
                }
            }
        }

        if (spot != null) {
            dest = new short[] { (short)spot.x, (short)spot.y };
        } else {
            piece.teleMoves = null;
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return dest != null;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        _piece = bangobj.pieces.get(pieceId);
        if (_piece == null) {
            log.warning("Missing teleported piece for teleport effect", "id", pieceId);
            return false;
        }

        if (damageEffect != null) {
            damageEffect.apply(bangobj, obs);
        } else {
            // move the piece and report the effect
            moveAndReport(bangobj, _piece, dest[0], dest[1], obs);
            reportEffect(obs, _piece, TELEPORTED);
        }

        // Make sure the teleporter maintains the proper board state
        Teleporter source = (Teleporter)bangobj.pieces.get(sourceId);
        if (source != null) {
            bangobj.board.shadowPiece(source);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new TeleportHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return 100;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (damageEffect == null || _piece == null || _piece.owner != pidx ||
                pidx == -1) {
            return null;
        }
        return MessageBundle.compose(
               "m.effect_teleport_death", _piece.getName());
    }

    protected transient Piece _piece;
}
