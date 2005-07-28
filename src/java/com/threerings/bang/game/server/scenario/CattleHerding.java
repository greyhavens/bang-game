//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.threerings.presents.server.InvocationException;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Cow;
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
    public static final float CATTLE_RATIO = 0.06f;

    @Override // documentation inherited
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

        return false;
    }

    @Override // documentation inherited
    public void unitMoved (BangObject bangobj, Unit unit)
    {
    }

    @Override // documentation inherited
    protected boolean respawnPieces ()
    {
        return true;
    }
}
