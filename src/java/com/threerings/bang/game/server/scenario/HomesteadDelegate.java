//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.HomesteadEffect;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.LandGrabInfo;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.PieceSet;

import static com.threerings.bang.Log.log;

/**
 * Handles Homestead behavior in a scenario.
 */
public class HomesteadDelegate extends ScenarioDelegate
{
    /**
     * Returns the list of homesteads on the board.
     */
    public ArrayList<Homestead> getHomesteads ()
    {
        return _steads;
    }

    /**
     * Determines the starting spot for the specified player based on their
     * most recently claimed homestead. Returns null if they control no
     * homesteads.
     */
    public Point getStartSpot (int pidx)
    {
        // look for the most recently claimed homestead for this player
        for (int ii = _claims.size()-1; ii >= 0; ii--) {
            Homestead stead = _claims.get(ii);
            if (stead.owner == pidx) {
                return new Point(stead.x, stead.y);
            }
        }
        return null;
    }

    @Override // from ScenarioDelegate
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces)
    {
        super.filterPieces(bangobj, starts, pieces);

        // note the location of all homestead pieces
        _steads.clear();
        for (Piece p : pieces) {
            if (p instanceof Homestead) {
                _steads.add((Homestead)p);
            }
        }
    }

    @Override // from Scenario
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        super.roundWillStart(bangobj);

        // assign the homestead nearest each starting spot to the that player
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            Point start = _parent._startSpots[ii];
            Homestead nearest = null;
            int dist = Integer.MAX_VALUE;
            for (Homestead stead : getHomesteads()) {
                int sdist = stead.getDistance(start.x, start.y);
                if (sdist < dist) {
                    dist = sdist;
                    nearest = stead;
                }
            }
            if (nearest == null) {
                log.warning("Unable to find starting homestead for player " +
                            "[board=" + bangobj.boardName +
                            ":" + bangobj.players.length +
                            ", start=" + start + "].");
                continue;
            }
            if (nearest.owner != -1) {
                log.warning("Homestead is nearest to more than one " +
                            "player [board=" + bangobj.boardName +
                            ":" + bangobj.players.length +
                            ", stead=" + nearest.info() + "]");
                continue;
            }

            // assign this homestead to its new owner
            nearest.owner = ii;
            bangobj.updatePieces(nearest);
            bangobj.grantPoints(ii, LandGrabInfo.POINTS_PER_STEAD);
            _claims.add(nearest);
        }
    }

    @Override // from Scenario
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        super.pieceMoved(bangobj, piece);

        if (piece instanceof Unit) {
            checkClaimedHomestead(bangobj, (Unit)piece);
        }
    }

    @Override // from Scenario
    public void pieceWasKilled (BangObject bangobj, Piece piece)
    {
        super.pieceWasKilled(bangobj, piece);

        // if this was a homestead, deduct points from the previous owner
        if (piece instanceof Homestead) {
            Homestead stead = (Homestead)piece;
            bangobj.grantPoints(stead.previousOwner,
                                -LandGrabInfo.POINTS_PER_STEAD);
        }
    }

    protected void checkClaimedHomestead (BangObject bangobj, Unit claimer)
    {
        // only big shots can claim homesteads
        if (claimer.getConfig().rank != UnitConfig.Rank.BIGSHOT) {
            return;
        }

        for (Homestead stead : _steads) {
            if (stead.owner == -1 && stead.getDistance(claimer) == 1) {
                HomesteadEffect effect = new HomesteadEffect();
                effect.init(claimer);
                effect.steadId = stead.pieceId;
                effect.owner = claimer.owner;
                _bangmgr.deployEffect(claimer.owner, effect);

                // grant this player points for the claimed homestead
                bangobj.grantPoints(
                    claimer.owner, LandGrabInfo.POINTS_PER_STEAD);

                // move this homestead to the end of the claims list
                if (_claims != null) {
                    _claims.remove(stead);
                    _claims.add(stead);
                }
            }
        }
    }

    /** Used to track the locations of all homestead spots. */
    protected ArrayList<Homestead> _steads = new ArrayList<Homestead>();

    /** Tracks the order in which players claimed homesteads. */
    protected ArrayList<Homestead> _claims = new ArrayList<Homestead>();
}
