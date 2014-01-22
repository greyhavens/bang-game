//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Iterator;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.TotemLogic;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.TotemEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.parlor.game.data.GameAI;
import com.threerings.presents.server.InvocationException;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li>Fill this in once the scenario is finalized
 * </ul>
 */
public class TotemBuilding extends Scenario
{
    /**
     * Creates a totem building scenario and registers its delegates.
     */
    public TotemBuilding ()
    {
        registerDelegate(new RespawnDelegate());
        registerDelegate(new TotemBaseDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new TotemLogic();
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);

        // extract and remove all totem markers
        _totems.clear();
        for (Iterator <Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.TOTEM)) {
                _totems.add(p.x, p.y);
                iter.remove();
            }
        }
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // start with totem pieces at every totem spot
        for (int ii = 0; ii < _totems.size(); ii++) {
            dropBonus(bangobj, TotemEffect.TOTEM_PIECES[
                          RandomUtil.getInt(TotemEffect.TOTEM_PIECES.length)],
                      _totems.getX(ii), _totems.getY(ii));
        }
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // count up the totem pieces that are "in play"
        int totems = 0;
        int crowns = 0;
        int bases = 0;
        for (Piece p : pieces) {
            String type = null;
            if (p instanceof TotemBonus) {
                totems++;
                type = ((TotemBonus)p).getConfig().type;
            } else if (p instanceof Unit && TotemBonus.isHolding((Unit)p)) {
                totems++;
                type = ((Unit)p).holding;
            } else if (p instanceof TotemBase) {
                bases++;
                if (!((TotemBase)p).canAddPiece()) {
                    crowns++;
                }
            }
            if (TotemEffect.TOTEM_CROWN_BONUS.equals(type)) {
                crowns++;
            }
        }

        // if there is not at least 1.5 totems in play for every player in the game, try to spawn
        // another one
        int numPlayers = bangobj.getActivePlayerCount();
        if (totems < TOTEM_RATIO * numPlayers) {
            String type = TotemEffect.TOTEM_PIECES[
                RandomUtil.getInt(TotemEffect.TOTEM_PIECES.length)];
            float progress = (float)bangobj.tick / bangobj.duration;
            // if we're far enough along in the round and there isn't a crown for each base, maybe
            // spawn one
            if (progress > CROWN_SPAWN && crowns < bases && RandomUtil.getInt(100) > 50) {
                type = TotemEffect.TOTEM_CROWN_BONUS;
            }
            return placeBonus(bangobj, pieces, Bonus.createBonus(BonusConfig.getConfig(type)),
                              _totems);
        } else {
            return super.addBonus(bangobj, pieces);
        }
    }

    @Override // documentation inherited
    public void recordStats (StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the number of totem pieces stacked
        for (TotemBonus.Type type : TotemBonus.Type.values()) {
            int totems = stats[pidx].getIntStat(type.stat());
            if (totems > 0) {
                user.stats.incrementStat(type.stat(), totems);
            }
        }
    }

    /** Used to track the locations of all the totem generating spots. */
    protected PointSet _totems = new PointSet();

    /** How long into the round before crowns will spawn. */
    protected static final float CROWN_SPAWN = 0.7f;

    /** Totem to player ratio. */
    protected static final float TOTEM_RATIO = 1.5f;
}
