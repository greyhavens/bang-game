//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.QuickSort;

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

        // create mappings from homestead "colors" (stored owners) to
        // player indices.  the closest colored homestead to a player
        // determines his color; if there are no colored homesteads,
        // the closest unclaimed homestead is colored.
        int[] owners = new int[] { -1, -1, -1, -1 };
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            Point start = _parent._startSpots[ii];
            Homestead unclaimed = null, colored = null;
            int udist = Integer.MAX_VALUE, cdist = Integer.MAX_VALUE;
            for (Homestead stead : getHomesteads()) {
                int sdist = stead.getDistance(start.x, start.y);
                if (stead.owner == -1 && sdist < udist) {
                    unclaimed = stead;
                    udist = sdist;
                } else if (stead.owner >= 0 && owners[stead.owner] == -1 &&
                    sdist < cdist) {
                    colored = stead;
                    cdist = sdist;
                }
            }
            if (colored != null) {
                owners[colored.owner] = ii;
                  
            } else if (unclaimed != null) {
                unclaimed.owner = IntListUtil.indexOf(owners, -1);
                owners[unclaimed.owner] = ii;
                
            } else {
                log.warning("Unable to find starting homestead for player " +
                            "[board=" + bangobj.boardName +
                            ":" + bangobj.players.length +
                            ", start=" + start + "].");
                break;
            }
        }
        
        // now assign the homesteads according to the mappings we created
        for (Homestead stead : getHomesteads()) {
            if (stead.owner == -1) {
                continue;
            }
            stead.owner = owners[stead.owner];
            bangobj.updatePieces(stead);
            if (stead.owner != -1) {
                bangobj.grantPoints(stead.owner,
                    LandGrabInfo.POINTS_PER_STEAD);
                _claims.add(stead);
            }
        }
        
        // finally, sort the claims by decreasing distance from the player
        // starts
        QuickSort.rsort(_claims, new Comparator<Homestead>() {
            public int compare (Homestead h1, Homestead h2) {
                Point p1 = _parent._startSpots[h1.owner],
                    p2 = _parent._startSpots[h2.owner];
                return h1.getDistance(p1.x, p1.y) - h2.getDistance(p2.x, p2.y);
            }
        });
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
