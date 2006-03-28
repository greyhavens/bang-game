//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.CattleRustlingLogic;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Various cattle start scattered randomly around the board.
 * <li> When a player moves their Big Shot unit next to a cow, the Big Shot
 * brands that cow for the player. The cow remains branded by that player until
 * another player's Big Shot rebrands it.
 * <li> Any units that are killed during the round respawn near the
 * player's starting marker.
 * <li> The round ends after a fixed time limit.
 * <li> Players earn money for all cattle with their brand at the end of the
 * round.
 * </ul>
 */
public class CattleRustling extends Scenario
{
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

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new CattleRustlingLogic();
    }
    
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> markers,
                                PointSet bonusSpots, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, markers, bonusSpots, purchases);

        // now place the cattle near the cattle starting spots
        int placed = 0, players = bangobj.players.length, cps = (int)
            Math.ceil(_cattleSpots.size() / (float)players);
      MARKER_LOOP:
        for (Marker cspot : _cattleSpots) {
            ArrayList<Point> spots = bangobj.board.getOccupiableSpots(
                cps, cspot.x, cspot.y, 3);
            for (Point spot : spots) {
                Cow cow = new Cow();
                cow.assignPieceId(bangobj);
                cow.position(spot.x, spot.y);
                cow.orientation = (short)RandomUtil.getInt(4);
//                cow.owner = determineOwner(cow);
                bangobj.board.shadowPiece(cow);
                bangobj.addToPieces(cow);

                // stop when we've placed three cattle per player
                if (++placed >= players * 3) {
                    break MARKER_LOOP;
                }
            }
        }
    }

    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // note rustled counts
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Cow && pieces[ii].owner != -1) {
                bangobj.stats[pieces[ii].owner].incrementStat(
                    Stat.Type.CATTLE_RUSTLED, 1);
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of cattle they rustled
        int rustled = bangobj.stats[pidx].getIntStat(Stat.Type.CATTLE_RUSTLED);
        if (rustled > 0) {
            user.stats.incrementStat(Stat.Type.CATTLE_RUSTLED, rustled);
        }
    }

    @Override // documentation inherited
    protected boolean respawnPieces ()
    {
        return true;
    }

    @Override // documentation inherited
    protected short getBaseDuration ()
    {
        // cattle herding should be a bit shorter than the normal scenario
        return 4 * BASE_SCENARIO_TICKS / 5;
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
