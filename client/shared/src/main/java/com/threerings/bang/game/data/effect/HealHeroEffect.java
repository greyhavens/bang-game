//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.HealHeroHandler;

import static com.threerings.bang.Log.log;

/**
 * Heals a teams hero.
 */
public class HealHeroEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String HEAL_HERO = "indian_post/heal_hero";

    /** The base amount by which to heal the hero. */
    public int baseHeal = 20;

    /** The updated damage for the affected piece. */
    public int newDamage;

    /** The piece id of the hero being healed. */
    public int heroId = -1;

    public HealHeroEffect ()
    {
    }

    @Override // documentation inherited
    public int getBonusPoints ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId, heroId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // who's our hero?
        Piece activator = bangobj.pieces.get(pieceId);
        if (activator == null) {
            return;
        }
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Unit && piece.owner == activator.owner &&
                    ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                heroId = piece.pieceId;
                newDamage = Math.max(0, piece.damage - baseHeal);
                break;
            }
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return heroId != -1;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(heroId);
        if (piece == null) {
            log.warning("Missing hero for hero heal effect", "id", heroId);
            return false;
        }

        piece.damage = newDamage;
        reportEffect(obs, piece, HEAL_HERO);
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(heroId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_heal_hero", piece.getName());
    }

    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return null;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new HealHeroHandler();
    }
}
