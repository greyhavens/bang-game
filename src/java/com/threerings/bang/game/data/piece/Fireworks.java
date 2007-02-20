//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import com.jmex.bui.util.Point;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.RocketEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.DamageEffect;
import com.threerings.bang.game.data.effect.UpdateEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.FireworksSprite;


/**
 * Handles fireworks pieces in Boom Town.
 */
public class Fireworks extends Breakable
    implements CounterInterface
{
    @Override // documentation inherited
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        return null;
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, Piece[] pieces)
    {
        if (damage > 1) {
            ArrayList<Effect> effects = new ArrayList<Effect>();

            if (_count == -1) {
                // start countdown_shot.shooter
                _count = 3;
            } else {
                _count -= 1;
            }

            if (_count == 0) {
                effects.add(new DamageEffect(this, 100));

                // shoot in a random direction
                //int dir = RandomUtil.getInt(Piece.DIRECTIONS.length);
                for (int dir : Piece.DIRECTIONS) {
                    Piece piece = bangobj.getFirstAvailableTarget(x, y, dir);
                    effects.add(new RocketEffect(this, piece, 60));
                }
            } else if (_count > 0) {
                effects.add(new UpdateEffect(this));
            }
            return effects;
        }

        return null;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new FireworksSprite("props", "boom_town/breakables/fireworks");
    }
}
