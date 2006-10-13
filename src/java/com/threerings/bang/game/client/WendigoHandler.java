//
// $Id$

package com.threerings.bang.game.client;

import com.samskivert.util.Interval;

import com.threerings.openal.Sound;

import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.WendigoEffect;
import com.threerings.bang.game.data.effect.WendigoEffect.Collision;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Displays a wendigo.
 */
public class WendigoHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        WendigoEffect effect = (WendigoEffect)_effect;

        // play the flight sound
        _sounds.getSound(SOUND_PATH + "flight.ogg").play(true);
        
        long delay = 0;
        for (WendigoEffect.Movement m : effect.moves) {
            Piece piece = _bangobj.pieces.get(m.pieceId);
            if (piece != null) {
                _bangobj.board.clearShadow(piece);
                final int ox = piece.x, oy = piece.y;
                final int nx = m.nx, ny = m.ny;
                piece.position(m.nx, m.ny);
                final WendigoSprite sprite = 
                    (WendigoSprite)_view.getPieceSprite(piece);
                if (sprite != null) {
                    final int penderId = notePender();
                    new Interval(_ctx.getClient().getRunQueue()) {
                        public void expired () {
                            sprite.move(_bangobj.board, ox, oy, nx, ny, 
                                    WendigoEffect.WENDIGO_SPEED, 
                                    WendigoHandler.this, penderId);
                        }
                    }.schedule(delay);
                    if (!sprite.claw) {
                        delay += WENDIGO_SPACING;
                    }
                }
                _bangobj.removePieceDirect(piece);
            }
        }
        
        for (Collision collision : (effect).collisions) {
            new CollisionInterval(collision).schedule();
        }

        return !isCompleted();
    }

    @Override // documentation inherited
    public boolean isCompleted ()
    {
        boolean ret = super.isCompleted();
        if (ret) {
            _view.setWendigoAmbiance(
                    FadeBoardHandler.WENDIGO_FADE_DURATION, false);
        }
        return ret;
    }

    /**
     * Called to let the handler know the wendigo has finished moving.
     */
    public void pathCompleted (int penderId)
    {
        maybeComplete(penderId);
    }

    /**
     * Returns a reference to the bang object.
     */
    public BangObject getBangObject ()
    {
        return _bangobj;
    }

    /** An interval to activate collisions on their listed timesteps. */
    protected class CollisionInterval extends Interval
    {
        public CollisionInterval (Collision collision)
        {
            super(_ctx.getClient().getRunQueue());
            _collision = collision;
        }

        public void schedule ()
        {
            schedule((long)(1000 * (Math.max(0, _collision.step - 1)) /
                        WendigoEffect.WENDIGO_SPEED));
        }

        public void expired ()
        {
            Piece target = _bangobj.pieces.get(_collision.targetId);
            if (_collision.isKill()) {
                _sounds.getSound(SOUND_PATH + "kill.ogg").play(true);
                if (_collision.deathEffect != null) {
                    apply(_collision.deathEffect);
                }        
                if (target != null) {
                    Effect.damage(_bangobj, WendigoHandler.this, -1, target,
                        100, ShotEffect.DAMAGED);
                }
            } else {
                _sounds.getSound(SOUND_PATH + "defense.ogg").play(true);
                if (_collision.safe) {
                    pieceAffected(target, WendigoEffect.SAFE_PROTECT);
                }
                if (_collision.talisman) {
                    pieceAffected(target, WendigoEffect.TALISMAN_PROTECT);
                }
            }
        }

        protected Collision _collision;
    }

    protected static final long WENDIGO_SPACING = 150;
    
    protected static final String SOUND_PATH =
        "rsrc/extras/indian_post/wendigo/";
}
