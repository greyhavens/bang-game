//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.effect.HomesteadEffect;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.RandomLogic;
import com.threerings.bang.game.util.PieceSet;

import static com.threerings.bang.Log.log;

/**
 * Implements the server side of the Land Grab gameplay scenario.
 */
public class LandGrab extends Scenario
{
    public LandGrab ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new RespawnDelegate());
    }

    @Override // from Scenario
    public AILogic createAILogic (GameAI ai)
    {
        return new RandomLogic();
    }

    @Override // from Scenario
    public void filterPieces (BangObject bangobj, ArrayList<Piece> starts,
                              ArrayList<Piece> pieces)
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
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // assign the homestead nearest each starting spot to the that player
        for (int ii = 0; ii < _startSpots.length; ii++) {
            Homestead nearest = null;
            int dist = Integer.MAX_VALUE;
            for (Homestead stead : _steads) {
                int sdist = stead.getDistance(
                    _startSpots[ii].x, _startSpots[ii].y);
                if (sdist < dist) {
                    dist = sdist;
                    nearest = stead;
                }
            }
            if (nearest == null) {
                log.warning("Unable to find starting homestead for player " +
                            "[board=" + bangobj.boardName +
                            ":" + bangobj.players.length +
                            ", start=" + _startSpots[ii] + "].");
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
            bangobj.grantPoints(ii, ScenarioCodes.POINTS_PER_STEAD);
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
                                -ScenarioCodes.POINTS_PER_STEAD);
            // TODO: update stats?
        }
    }

    @Override // from Scenario
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // increment each players' homestead related stats
        int[] steads = new int[bangobj.players.length];
        for (Homestead stead : _steads) {
            if (stead.owner >= 0) {
                steads[stead.owner]++;
            }
        }
        for (int ii = 0; ii < steads.length; ii++) {
            bangobj.stats[ii].incrementStat(
                Stat.Type.STEADS_CLAIMED, steads[ii]);
        }
    }

    @Override // from Scenario
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // persist the number of homesteads they claimed
        int nuggets = bangobj.stats[pidx].getIntStat(Stat.Type.STEADS_CLAIMED);
        if (nuggets > 0) {
            user.stats.incrementStat(Stat.Type.STEADS_CLAIMED, nuggets);
        }
    }

    @Override // from Scenario
    protected Point getStartSpot (int pidx)
    {
        // look for the most recently claimed homestead for this player
        for (int ii = _claims.size()-1; ii >= 0; ii--) {
            Homestead stead = _claims.get(ii);
            if (stead.owner == pidx) {
                return new Point(stead.x, stead.y);
            }
        }
        // fall back to their default starting location
        return super.getStartSpot(pidx);
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
                    claimer.owner, ScenarioCodes.POINTS_PER_STEAD);
                // TODO: update stats?

                // move this homestead to the end of the claims list
                _claims.remove(stead);
                _claims.add(stead);
            }
        }
    }

    /** Used to track the locations of all homestead spots. */
    protected ArrayList<Homestead> _steads = new ArrayList<Homestead>();

    /** Tracks the order in which players claimed homesteads. */
    protected ArrayList<Homestead> _claims = new ArrayList<Homestead>();
}
