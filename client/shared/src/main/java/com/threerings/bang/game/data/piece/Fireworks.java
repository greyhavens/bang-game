//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.RocketEffect;
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
        // don't do explosion effect
        return null;
    }

    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
        if (_wasDamaged) {
            ArrayList<Effect> effects = new ArrayList<Effect>();

            if (_count == -1) {
                // start countdown_shot.shooter
                _count = 3;
            } else {
                _count -= 1;
            }
            if (_count == 0) {
                effects.add(new DamageEffect(this, 100));
                effects.add(new RocketEffect(this, 60));
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
