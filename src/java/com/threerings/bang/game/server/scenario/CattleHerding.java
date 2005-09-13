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
        // extract and remove all cattle markers
        _cattleSpots.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.CATTLE)) {
                _cattleSpots.add((Marker)p);
                iter.remove();
            }
        }
    }

    public void init (BangObject bangobj, ArrayList<Piece> markers,
                      PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.init(bangobj, markers, bonusSpots, purchases);

        // determine how many cattle we want to put on the board
        int cattle = 0, cps = 0;
        while (cattle < bangobj.players.length * 3) {
            cattle += _cattleSpots.size();
            cps++;
        }

        // now place the cattle near the cattle starting spots
        for (Marker cspot : _cattleSpots) {
            ArrayList<Point> spots = bangobj.board.getOccupiableSpots(
                cps, cspot.x, cspot.y, 3);
            for (Point spot : spots) {
                Cow cow = new Cow();
                cow.assignPieceId();
                cow.position(spot.x, spot.y);
                cow.orientation = (short)RandomUtil.getInt(4);
                cow.owner = determineOwner(cow);
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
            if (pieces[ii] instanceof Cow && pieces[ii].owner != -1) {
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
            int newOwner = determineOwner(cow);
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

    /**
     * Returns the owner of this cow (the player with the closest starting
     * spot) or -1 if the cow is not close enough to any spot to be owned.
     */
    protected int determineOwner (Cow cow)
    {
        int newOwner = -1;
        int minDist = MAX_OWNER_DISTANCE+1;

        // if the cow currently has an owner, and that owner is within
        // range, start with them as the new owner and only assign another
        // player to be the owner if they are closer than this player
        if (cow.owner != -1) {
            int oldDist = cow.getDistance(_startSpots[cow.owner].x,
                                          _startSpots[cow.owner].y);
            if (oldDist <= MAX_OWNER_DISTANCE) {
                newOwner = cow.owner;
                minDist = oldDist;
            }
        }

        for (int ii = 0; ii < _startSpots.length; ii++) {
            int dist = cow.getDistance(_startSpots[ii].x, _startSpots[ii].y);
            if (dist < minDist) {
                newOwner = ii;
                minDist = dist;
            }
        }
        return newOwner;
    }

    protected ArrayList<Marker> _cattleSpots = new ArrayList<Marker>();

    protected static final int MAX_OWNER_DISTANCE = 5;
}
