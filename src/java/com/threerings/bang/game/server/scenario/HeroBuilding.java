//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import java.awt.Point;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.HealHeroEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

import com.threerings.bang.game.util.PieceSet;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li>You bigshot is your "hero" which you try to level.
 * <li>Units landing on bonus tiles will heal the hero.
 * <li>The hero killing opponent units will gain experience/level
 * <li>Each level gained by the hero will give them some bonus influence.
 * <li>Units respawn next to their hero.
 * <li>The higher level the hero is, the slower units will respawn.
 * </ul>
 */
public class HeroBuilding extends Scenario
    implements PieceCodes
{
    /**
     * Creates a hero building scenario and registers its delegates.
     */
    public HeroBuilding ()
    {
        registerDelegate(new HeroDelegate());
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        for (int ii = 0; ii < _startSpots.length; ii++) {
            ArrayList<Point> heals = bangobj.board.getRandomOccupiableSpots(
                    _bangmgr.getTeamSize(ii), _startSpots[ii].x, _startSpots[ii].y, 2, 5);
            for (Point heal : heals) {
                dropBonus(bangobj, HealHeroEffect.HEAL_HERO, heal.x, heal.y);
            }
        }
    }

    @Override // documentation inherited
    public void recordStats (BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the hero level
        int level = bangobj.stats[pidx].getIntStat(StatType.HERO_LEVEL);
        if (level > 0) {
            user.stats.incrementStat(StatType.HERO_LEVEL, level);
        }
    }
}
