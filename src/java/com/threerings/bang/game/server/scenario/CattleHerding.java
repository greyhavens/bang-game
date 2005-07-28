//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.StringUtil;
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

        // place cattle randomly around the board
        int width = bangobj.board.getWidth(), height = bangobj.board.getHeight();
        int cattle = (int)Math.round(width * height * CATTLE_RATIO);
        for (int ii = 0; ii < cattle; ii++) {
            Cow cow = new Cow();
            int cx = RandomUtil.getInt(width);
            int cy = RandomUtil.getInt(height);
            Point spot = bangobj.board.getOccupiableSpot(cx, cy, 3);
            if (spot == null) {
                log.info("Unable to place cow [game=" + bangobj.which() +
                         ", cx=" + cx + ", cy=" + cy + "].");
                continue;
            }
            cow.assignPieceId();
            cow.position(spot.x, spot.y);
            cow.orientation = (short)RandomUtil.getInt(4);
            bangobj.board.updateShadow(null, cow);
            bangobj.addToPieces(cow);
        }
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        // continue the game while at least one cow remains uncorralled
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if ((pieces[ii] instanceof Cow) && !((Cow)pieces[ii]).corralled) {
                return false;
            }
        }

        return true;
    }

    @Override // documentation inherited
    public Effect pieceMoved (BangObject bangobj, Piece piece)
    {
        if (!(piece instanceof Cow)) {
            return null;
        }

        // check to see if our cow entered a corral
        Cow cow = (Cow)piece;
        for (CorralEntrance ce : _corrals) {
            if (ce.x == piece.x && ce.y == piece.y) {
                // score cash for this player
                int ncash = bangobj.funds[ce.owner] + CASH_PER_COW;
                bangobj.setFundsAt(ncash, ce.owner);

                // return an effect that will corral the cow
                return new CorralledEffect();
            }
        }

        return null;
    }

    @Override // documentation inherited
    protected boolean respawnPieces ()
    {
        return true;
    }

    protected static class CorralEntrance
    {
        public short x, y;
        public int owner;
    }

    protected ArrayList<CorralEntrance> _corrals =
        new ArrayList<CorralEntrance>();
}
