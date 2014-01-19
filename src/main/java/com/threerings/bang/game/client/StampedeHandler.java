//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;

import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.client.sprite.BisonSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.StampedeEffect;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a stampede.
 */
public class StampedeHandler extends CollisionHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _stampede = (StampedeEffect)_effect;

        // set the bison on the path listed in the effect, spread out
        // in a circle around the herd center
        float angle = RandomUtil.getFloat(FastMath.TWO_PI);
        for (int ii = 0; ii < NUM_BISON; ii++) {
            final int penderId = notePender();
            BisonSprite sprite = new BisonSprite(
                angle, BISON_DISTANCE, _stampede.path) {
                public void pathCompleted () {
                    super.pathCompleted();
                    _view.removeSprite(this);
                    maybeComplete(penderId);
                }
            };
            angle += (FastMath.TWO_PI / NUM_BISON);
            sprite.init(_ctx, _view, _bangobj.board, _sounds,
                new DummyPiece(), _bangobj.tick);
            _view.addSprite(sprite);
        }
        
        // activate each collision on its listed tick
        for (int ii = 0; ii < _stampede.collisions.length; ii++) {
            new CollisionInterval(_stampede.collisions[ii]).schedule();
        }

        return !isCompleted();
    }

    /** A dummy piece for the bison sprites. */
    protected class DummyPiece extends Piece
    {
    }

    /** An interval to activate collisions on their listed timesteps. */
    protected class CollisionInterval extends Interval
    {
        public CollisionInterval (StampedeEffect.Collision collision)
        {
            super(_ctx.getClient().getRunQueue());
            _collision = collision;
        }

        public void schedule ()
        {
            schedule((long)(1000 * (Math.max(0, _collision.step-1)) /
                            StampedeEffect.BISON_SPEED));
        }

        public void expired ()
        {
            // this may queue up
            if (_collision.deathEffect != null) {
                apply(_collision.deathEffect);
            }
            Effect.collide(_bangobj, StampedeHandler.this, _stampede.causer,
                -1, _collision.targetId, StampedeEffect.COLLISION_DAMAGE,
                _collision.x, _collision.y, ShotEffect.DAMAGED);
        }

        protected StampedeEffect.Collision _collision;
    }

    protected StampedeEffect _stampede;
    
    /** The number of bison in the stampede. */
    protected static final int NUM_BISON = 3;
    
    /** The distance of each bison from the center of the herd. */
    protected static final float BISON_DISTANCE = TILE_SIZE / 2;
}
