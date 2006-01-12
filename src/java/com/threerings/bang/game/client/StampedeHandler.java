//
// $Id$

package com.threerings.bang.game.client;

import java.util.Iterator;

import com.samskivert.util.Interval;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.StampedeEffect;
import com.threerings.bang.game.data.effect.TrainEffect;

import com.threerings.bang.util.BangContext;

/**
 * Displays a stampede.
 */
public class StampedeHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        _stampede = (StampedeEffect)_effect;

        // set all the buffalo on the paths listed in the effect
        for (int i = 0; i < _stampede.paths.length; i++) {
            MobileSprite sprite = new MobileSprite("extras", "bison");
            sprite.init(_ctx, _view, _bangobj.board, _sounds,
                        new DummyPiece(), _bangobj.tick);
            _view.addSprite(sprite);
            sprite.addObserver(_remover);
            _buffalo++;
            sprite.move(_bangobj.board, _stampede.paths[i],
                StampedeEffect.BUFFALO_SPEED);
        }

        // activate each collision on its listed tick
        for (int i = 0, size = _stampede.collisions.size(); i < size; i++) {
            StampedeEffect.Collision collision =
                (StampedeEffect.Collision)_stampede.collisions.get(i);
            new CollisionInterval(collision).schedule();
        }

        return !isCompleted();
    }

    @Override // documentation inherited
    protected boolean isCompleted ()
    {
        return super.isCompleted() && (_buffalo == 0);
    }

    /** A dummy piece for the buffalo sprites. */
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
            schedule((long)(1000 * (_collision.step-1) /
                            StampedeEffect.BUFFALO_SPEED));
        }

        public void expired ()
        {
            // this may queue up 
            Effect.collide(_bangobj, StampedeHandler.this, _stampede.causer,
                           _collision.targetId, StampedeEffect.COLLISION_DAMAGE,
                           _collision.x, _collision.y, StampedeEffect.DAMAGED);
        }

        protected StampedeEffect.Collision _collision;
    }

    protected PathObserver _remover = new PathObserver() {
        public void pathCompleted (Sprite sprite, Path path) {
            _view.removeSprite(sprite);
            _buffalo--;
            maybeComplete(-1);
        }
        public void pathCancelled (Sprite sprite, Path path) {
            _view.removeSprite(sprite);
            _buffalo--;
            maybeComplete(-1);
        }
    };

    protected StampedeEffect _stampede;
    protected int _buffalo;
}
