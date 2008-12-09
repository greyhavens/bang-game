//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.HeroInfluence;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Causes a player's hero to change levels.
 */
public class LevelEffect extends Effect
{
    /** The identifier for the level changing effect. */
    public static final String LEVEL_UP = "indian_post/level_up";

    /** The id of the hero that's level is changing. */
    public int pieceId = -1;

    /** The new level being assigned to the hero. */
    public byte level;

    /**
     * Creates a level affect for the player's hero.
     */
    public static LevelEffect changeLevel (BangObject bangobj, int player, byte level)
    {
        LevelEffect effect = new LevelEffect();
        effect.level = level;
        for (Piece piece : bangobj.pieces) {
            if (piece.owner == player && piece instanceof Unit &&
                    ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                effect.pieceId = piece.pieceId;
                return effect;
            }
        }
        return null;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing to do here
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Unit hero = (Unit)bangobj.pieces.get(pieceId);
        if (hero == null) {
            log.warning("Unable to find hero for level change", "pieceId", pieceId);
            return false;
        }
        Influence influence = hero.getInfluence(Unit.InfluenceType.SPECIAL);
        if (influence == null || !(influence instanceof HeroInfluence)) {
            hero.setInfluence(Unit.InfluenceType.SPECIAL, new HeroInfluence(level));
        } else {
            ((HeroInfluence)influence).setLevel(level);
        }
        if (level > 0) {
            reportEffect(obs, hero, LEVEL_UP);
        }
        return true;
    }
}
