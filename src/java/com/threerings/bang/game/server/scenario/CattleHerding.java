//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.StringUtil;
import com.threerings.media.util.MathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CorralledEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Various cattle being standing randomly around the board.
 * <li> Each player has a corral on one side of the board which protrudes
 * into the playable area.
 * <li> When a player moves a unit next to a cattle piece, the cattle
 * moves in a direction opposite the unit.
 * <li> Players earn money for all cattle herded into their corral.
 * <li> Any units that are killed during the round respawn near the
 * player's starting marker.
 * <li> The round ends when all cattle are herded into a corral.
 * </ul>
 */
public class CattleHerding extends Scenario
{
    /** The identifier for this scenario. */
    public static final String IDENT = "ch";

    /** The ratio of cattle to the size of the board (width x height). Can
     * also be considered the probability that a cow will be spawned in a
     * particular square. */
    public static final float CATTLE_RATIO = 0.04f;

    /** Cash earned for each corralled cow. */
    public static final int CASH_PER_COW = 50;

    @Override // documentation inherited
    public void filterMarkers (BangObject bangobj, ArrayList<Piece> starts,
                               ArrayList<Piece> pieces)
    {
        // extract and remove all corral markers
        _corrals.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.CORRAL)) {
                CorralEntrance ce = new CorralEntrance();
                ce.x = p.x;
                ce.y = p.y;
                ce.owner = getOwner(p, starts);
                if (ce.owner == -1) {
                    log.warning("No owner for corral entrance!? " +
                                "[entrance=" + p + ", starts=" +
                                StringUtil.toString(starts) + "].");
                } else {
                    _corrals.add(ce);
                }
                iter.remove();
            }
        }
    }

    public void init (BangObject bangobj, ArrayList<Piece> markers,
                      PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.init(bangobj, markers, bonusSpots, purchases);

        // determine how many cows we'll have per player (3-5)
        int cpp = RandomUtil.getInt(2) + 3;

        // now place the cattle near the players' starting locations
        for (int ii = 0; ii < _startSpots.length; ii++) {
            ArrayList<Point> spots = bangobj.board.getOccupiableSpots(
                cpp, _startSpots[ii].x, _startSpots[ii].y, 5);
            for (Point spot : spots) {
                Cow cow = new Cow();
                cow.owner = (short)ii;
                cow.assignPieceId();
                cow.position(spot.x, spot.y);
                cow.orientation = (short)RandomUtil.getInt(4);
                bangobj.board.updateShadow(null, cow);
                bangobj.addToPieces(cow);
            }
        }
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        if (!super.tick(bangobj, tick)) {
            return false;
        }

        // score cash for each cow
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Cow) {
                bangobj.grantCash(pieces[ii].owner, CASH_PER_COW);
            }
        }

        return true;
    }

    @Override // documentation inherited
    public Effect pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Cow) {
            // recompute this cow's owner
            Cow cow = (Cow)piece;
            int newOwner = -1;
            int minDistSq = Integer.MAX_VALUE;
            for (int ii = 0; ii < _startSpots.length; ii++) {
                int distSq = MathUtil.distanceSq(
                    cow.x, cow.y, _startSpots[ii].x, _startSpots[ii].y);
                if (distSq < minDistSq) {
                    newOwner = ii;
                    minDistSq = distSq;
                }
            }

            if (newOwner != cow.owner) {
                cow.owner = newOwner;
                log.info("Cow changed owner " + cow.info());
            }

        } else if (piece instanceof Unit) {
            // check to see if this unit spooked any cattle
            Piece[] pieces = bangobj.getPieceArray();
            for (int ii = 0; ii < pieces.length; ii++) {
                if (pieces[ii] instanceof Cow &&
                    piece.getDistance(pieces[ii]) == 1) {
                    ((Cow)pieces[ii]).spook((Unit)piece);
                }
            }
        }

        return null;
    }

    @Override // documentation inherited
    protected boolean respawnPieces ()
    {
        return true;
    }

    @Override // documentation inherited
    protected long getMaxScenarioTime ()
    {
        return 5 * 60 * 1000L;
    }

    protected static class CorralEntrance
    {
        public short x, y;
        public int owner;
    }

    protected ArrayList<CorralEntrance> _corrals =
        new ArrayList<CorralEntrance>();
}
