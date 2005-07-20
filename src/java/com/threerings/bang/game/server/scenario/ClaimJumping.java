//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;

import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.media.util.MathUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Each player has a mine shaft, and those mine shafts start with a
 * particular quantity of gold.
 * <li> When another player's unit lands on (or in front of) the mine
 * shaft, they steal a nugget of gold from the shaft and must return that
 * nugget to their own shaft to deposit it.
 * <li> If the unit carrying the nugget is shot, it drops the nugget in a
 * nearby square and the nugget can then be picked up by any piece that
 * lands on it.
 * <li> When one player's mine is completely depleted of nuggets, the
 * round ends.
 * <li> Any units that are killed during the round respawn near the
 * player's starting marker.
 * </ul>
 */
public class ClaimJumping extends Scenario
    implements GameCodes
{
    /** The number of nuggets in each claim. TODO: put in BangConfig. */
    public static final int NUGGET_COUNT = 1;

    @Override // documentation inherited
    public String init (BangObject bangobj, ArrayList<Piece> markers)
    {
        _claims = new ArrayList<Claim>();
        _gameOverTick = -1;

        // locate all the claims, assign them to players and fill them
        // with nuggets
        ArrayIntSet assigned = new ArrayIntSet();
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof Claim) {
                Claim claim = (Claim)pieces[ii];
                // determine which marker to which it is nearest
                int midx = getNearestMarker(claim, markers);
                if (midx == -1 || assigned.contains(midx)) {
                    return "m.no_start_marker_for_claim";
                }
                // if we have a player in the game associated with this
                // start marker, configure this claim for play
                if (midx < bangobj.players.length) {
                    claim.owner = midx;
                    claim.nuggets = NUGGET_COUNT;
                    bangobj.updatePieces(claim);
                    _claims.add(claim);
                    assigned.add(midx);
                }
            }
        }

        return null;
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        // check to see if there are empty claims
        boolean empty = false;
        for (Claim claim : _claims) {
            if (claim.nuggets == 0) {
                empty = true;
                break;
            }
        }

        // if we have no timer and one or more claims are empty; start it
        if (_gameOverTick < 0 && empty) {
            _gameOverTick = (short)(tick + END_GAME_TIMER);
            String msg = MessageBundle.tcompose(
                "m.starting_timer", "" + END_GAME_TIMER);
            SpeakProvider.sendInfo(bangobj, GAME_MSGS, msg);
        }

        // if we have a timer started...
        if (_gameOverTick > 0) {
            // ...and no empty claims, stop it
            if (!empty) {
                _gameOverTick = -1;
                SpeakProvider.sendInfo(bangobj, GAME_MSGS, "m.timer_stopped");

            } else if (tick >= _gameOverTick) {
                // end the game if the timer expires
                SpeakProvider.sendInfo(bangobj, GAME_MSGS, "m.timer_expired");

                // score cash for all nuggets in each player's claim
                for (Claim claim : _claims) {
                    if (claim.nuggets <= 0) {
                        continue;
                    }
                    int ncash = bangobj.funds[claim.owner] +
                        CASH_PER_NUGGET * (claim.nuggets);
                    bangobj.setFundsAt(ncash, claim.owner);
                }
                return true;

            } else if ((_gameOverTick - tick) % 10 == 5) {
                int ticks = _gameOverTick - tick;
                String msg = MessageBundle.tcompose(
                    "m.ticking_timer", "" + ticks);
                SpeakProvider.sendInfo(bangobj, GAME_MSGS, msg);
            }
        }

        return false;
    }

    @Override // documentation inherited
    public void unitMoved (BangObject bangobj, Unit unit)
    {
        // if this unit landed next to one of the claims, do some stuff
        Claim claim = null;
        for (Claim c : _claims) {
            if (c.getDistance(unit) <= 1) {
                claim = c;
                break;
            }
        }
        if (claim == null) {
            return;
        }

        // deposit or withdraw a nugget as appropriate
        if (claim.owner == unit.owner && unit.benuggeted) {
            // TODO: create an effect to animate the nugget
            claim.nuggets++;
            unit.benuggeted = false;
            bangobj.updatePieces(claim);

        } else if (claim.owner != unit.owner && claim.nuggets > 0 &&
                   unit.canActivateBonus(_nuggetBonus)) {
            claim.nuggets--;
            unit.benuggeted = true;
            bangobj.updatePieces(claim);
        }
    }

    protected int getNearestMarker (Claim claim, ArrayList<Piece> markers)
    {
        int mindist2 = Integer.MAX_VALUE, idx = -1;
        for (int ii = 0, ll = markers.size(); ii < ll; ii++) {
            Piece marker = markers.get(ii);
            int dist2 = MathUtil.distanceSq(
                claim.x, claim.y, marker.x, marker.y);
            if (dist2 < mindist2) {
                mindist2 = dist2;
                idx = ii;
            }
        }
        return idx;
    }

    /** A list of the active claims. */
    protected ArrayList<Claim> _claims;

    /** Indicates the tick on whcih we will end the game. */
    protected short _gameOverTick = -1;

    /** A prototype nugget bonus used to ensure that pieces can be
     * benuggeted. */
    protected Bonus _nuggetBonus =
        Bonus.createBonus(BonusConfig.getConfig("nugget"));

    /** The number of ticks after which we end the game while at least one
     * claim is empty. */
    protected static final int END_GAME_TIMER = 28;

    /** The amount of cash earned per nugget at the end of the game. */
    protected static final int CASH_PER_NUGGET = 50;
}
