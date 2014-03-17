//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;

import com.samskivert.util.Interval;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.WendigoSprite;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.WendigoEffect;
import com.threerings.bang.game.data.effect.WendigoEffect.Collision;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

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
        
        // fire off the snow storm effect
        if (BangPrefs.isHighDetail()) {
            _view.displayCameraParticles("indian_post/wendigo/snow_storm",
                CAMERA_EFFECT_DURATION);
        }
        
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
                    new ShardSprite(target).activate();
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

    /** Handles the exploding ice shard effect. */
    protected class ShardSprite extends Sprite
    {
        public ShardSprite (Piece target)
        {
            _target = target;
            _ctx.loadModel("extras", "indian_post/ice_shard",
                new ResultAttacher<Model>(this));
        }
        
        /**
         * Activates the shard effect.
         */
        public void activate ()
        {
            final PieceSprite sprite = _view.getPieceSprite(_target);
            if (sprite == null) {
                effectDamage();
                return;
            }
            sprite.getModelNode().pauseAnimation(true);
            sprite.attachChild(this);
            final int penderId = notePender();
            final MaterialState mstate = _ctx.getRenderer().createMaterialState();
            mstate.getAmbient().set(ColorRGBA.white);
            mstate.getDiffuse().set(1f, 1f, 1f, 0f);
            setRenderState(mstate);
            updateRenderState();
            addController(new Controller() {
                public void update (float time) {
                    if ((_elapsed += time) >=
                        SHARD_RISE_DURATION + SHARD_LINGER_DURATION) {
                        sprite.detachChild(ShardSprite.this);
                        sprite.displayParticles(
                            "indian_post/wendigo/ice_shard_explosion", true);
                        sprite.getModelNode().pauseAnimation(false);
                        effectDamage();
                        maybeComplete(penderId);
                    }
                    float alpha = Math.min(1f, _elapsed / SHARD_RISE_DURATION);
                    mstate.getDiffuse().a = alpha;
                    getLocalTranslation().set(0f, 0f, TILE_SIZE * (alpha - 1));
                    getLocalRotation().fromAngleNormalAxis(
                        FastMath.TWO_PI * alpha, Vector3f.UNIT_Z);
                }
                protected float _elapsed;
            });
        }
        
        protected void effectDamage ()
        {
            Effect.damage(_bangobj, WendigoHandler.this, -1, null,
                _target, 100, ShotEffect.DAMAGED);
        }
        
        protected Piece _target;
        protected PieceSprite _tsprite;
    }
    
    protected static final long WENDIGO_SPACING = 150;
    
    protected static final float SHARD_RISE_DURATION = 1.5f;
    
    protected static final float SHARD_LINGER_DURATION = 0.5f;
    
    protected static final String SOUND_PATH =
        "rsrc/extras/indian_post/wendigo/";
}
