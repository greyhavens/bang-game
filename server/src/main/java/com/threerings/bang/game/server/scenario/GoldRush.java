//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Iterator;
import java.util.List;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.GoldLogic;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Each player has a mine shaft, and those mine shafts start empty of gold.
 * <li> Players must travel to the "hills" where there are gold nuggets for the taking and pick up
 * nuggets and carry them back to their carge tank.
 * <li> If the unit carrying the nugget is killed, it drops the nugget in a nearby square and the
 * nugget can then be picked up by any piece that lands on it.
 * <li> Any units that are killed during the round respawn near the player's starting marker.
 * </ul>
 */
public class GoldRush extends Scenario
{
    /**
     * Creates a gold rush scenario and registers its delegates.
     */
    public GoldRush ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new NuggetDelegate(false, 0));
        registerDelegate(new RespawnDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new GoldLogic(false);
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all gold lodes
        _lodes.clear();
        for (Iterator<Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.LODE)) {
                _lodes.add(p.x, p.y);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // start with nuggets at every lode spot
        for (int ii = 0; ii < _lodes.size(); ii++) {
            dropBonus(bangobj, NuggetEffect.NUGGET_BONUS, _lodes.getX(ii), _lodes.getY(ii));
        }
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // count up the nuggets that are "in play"
        int nuggets = 0;
        for (Piece p : pieces) {
            if (Bonus.isBonus(p, NuggetEffect.NUGGET_BONUS) ||
                (p instanceof Unit && NuggetEffect.NUGGET_BONUS.equals(((Unit)p).holding))) {
                nuggets++;
            }
        }

        // if there is not at least one nugget in play for every player in the game, try to spawn
        // another one
        if (nuggets < bangobj.getActivePlayerCount()) {
            return placeBonus(bangobj, pieces, Bonus.createBonus(
                                  BonusConfig.getConfig(NuggetEffect.NUGGET_BONUS)), _lodes);
        } else {
            return super.addBonus(bangobj, pieces);
        }
    }

    @Override // documentation inherited
    public void recordStats (StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the number of nuggets they claimed
        int nuggets = stats[pidx].getIntStat(StatType.NUGGETS_CLAIMED);
        if (nuggets > 0) {
            user.stats.incrementStat(StatType.NUGGETS_CLAIMED, nuggets);
        }
    }

    /** Used to track the locations of all lode spots. */
    protected PointSet _lodes = new PointSet();
}
