//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.Vector3f;

import com.threerings.jme.sprite.LineSegmentPath;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.data.effect.HealHeroEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Visualizes a ball of healing energy heading to the hero.
 */
public class HealHeroHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        HealHeroEffect heal = (HealHeroEffect)_effect;

        String soundPath = getSoundPath("frontier_town/half_repair");
        if (soundPath != null) {
            _sounds.getSound(soundPath).play(true);
        }

        if (heal.pieceId == heal.heroId) {
            return super.execute();
        }
        Piece source = _bangobj.pieces.get(heal.pieceId);
        if (source == null) {
            log.warning("Couldn't find source for heal hero effect", "pieceId", heal.pieceId);
            return false;
        }
        MobileSprite ssprite = (MobileSprite)_view.getPieceSprite(source);
        if (ssprite == null) {
            log.warning("Couldn't find source sprite for heal hero effect", "source", source);
            return false;
        }
        final Piece target = _bangobj.pieces.get(heal.heroId);
        if (target == null) {
            log.warning("Couldn't find target for heal hero effect", "heroId", heal.heroId);
            return false;
        }
        final MobileSprite tsprite = (MobileSprite)_view.getPieceSprite(target);
        if (tsprite == null) {
            log.warning("Couldn't find target sprite for heal hero effect", "target", target);
            return false;
        }

        // send the healing goodness to the hero
        Vector3f start = new Vector3f(ssprite.getWorldTranslation()),
                 end = new Vector3f(tsprite.getWorldTranslation()),
                 startup = ssprite.getWorldRotation().mult(Vector3f.UNIT_Z),
                 endup = tsprite.getWorldRotation().mult(Vector3f.UNIT_Z);
        start.scaleAdd(0.25f * TILE_SIZE, startup, start);
        end.scaleAdd(0.25f * TILE_SIZE, endup, end);
        final ShotSprite shot = new ShotSprite(
                _ctx, "effects/indian_post/heal_hero/travel", null);
        _view.addSprite(shot);
        shot.move(new LineSegmentPath(shot, Vector3f.UNIT_Z,
                    Vector3f.UNIT_X, new Vector3f[] { start, start, end, end },
                    new float[] { 0.05f, 0.7f, 0.1f }) {
            public void wasRemoved () {
                super.wasRemoved();
                _view.removeSprite(shot);
            }
        });

        return super.execute();
    }
}
