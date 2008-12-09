//
// $Id$

package com.threerings.bang.game.client;

import com.jme.math.Vector3f;
import com.jme.scene.Spatial;

import com.threerings.util.StreamablePoint;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.OrientingBallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.openal.Sound;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.client.sprite.FireworksSprite;
import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.RocketEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;
import static com.threerings.bang.game.client.BallisticShotHandler.*;

/**
 * Waits for all sprites involved in a shot to stop moving and then
 * animates the fired shot.
 */
public class RocketHandler extends EffectHandler
{
    public class RocketPathObserver
        implements PathObserver
    { 
        RocketPathObserver(RocketHandler handler, Piece target, int penderId,
            BangContext ctx, BangBoardView view, BangObject bangobj, RocketEffect effect)
        {
            _handler = handler;
            _target = target;
            _penderId = penderId;
            _ctx = ctx;
            _view = view;
            _bangobj = bangobj;
            _effect = effect;
        }
        
        // documentation inherited from interface PathObserver
        public void pathCompleted (Sprite sprite, Path path)
        {
            Vector3f spriteTranslation = sprite.getLocalTranslation();
            sprite.removeObserver(this);
        
            // apply the effect and complete our handling if that did not
            // result in anything that needs waiting for
            if (_target != null) {
                _effect.apply(_bangobj, _handler, 0, _target, 0);
            } else {
                EffectViz viz = new ExplosionViz("boom_town/fireworks/fireworks_explosion", false);
                viz.init(_ctx, _view, spriteTranslation, null);
                viz.display();
            }

            maybeComplete(_penderId);
            _view.removeSprite(sprite);
        }

        // documentation inherited from interface PathObserver
        public void pathCancelled (Sprite sprite, Path path)
        {
            sprite.removeObserver(this);

            // apply the effect and complete our handling if that did not
            // result in anything that needs waiting for
            if (_target != null) {
                _effect.apply(_bangobj, _handler, -1, _target, 0);
            }

            maybeComplete(_penderId);
            _view.removeSprite(sprite);
        }
        
        protected RocketHandler _handler;
        protected Piece _target;
        protected int _penderId;
        protected BangContext _ctx;
        protected BangBoardView _view;
        protected BangObject _bangobj;
        protected RocketEffect _effect;
    }
    
    @Override // documentation inherited
    public boolean execute ()
    {
        _shot = (RocketEffect)_effect;
        if (_shot.shooter == null) {
            log.warning("Missing shooter?", "shot", _shot);
            // abandon ship, we're screwed
            return false;
        }

        _applying = true;
        fireShot();
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    protected void fireShot ()
    {
        // now fire the shot animations at pieces
        for (int piece : _shot.pieces) {
            Piece target = _bangobj.pieces.get(piece);
            fireShot(_shot.shooter, target.x, target.y, target);
        }

        // now fire the shot animations at non-pentrable tiles
        for (StreamablePoint point : _shot.affectedPoints) {
            fireShot(_shot.shooter, point.x, point.y, null);
        }
    }

    protected void fireShot (Piece shooter, int tx, int ty, Piece target)
    {
        int sx = shooter.x;
        int sy = shooter.y;
        
        // if the shooter sprite has a node configured as a ballistic
        // shot source, use its translation; otherwise, just use a
        // point one half tile above the ground
        Vector3f start = null;
        FireworksSprite usprite = (FireworksSprite)_view.getTargetableSprite(_shot.shooter);
        if (usprite != null) {
            Spatial src = usprite.getRocketSource();
            if (src != null) {
                start = new Vector3f(src.getWorldTranslation());
            }
        }
        float escale = _bangobj.board.getElevationScale(TILE_SIZE);
        if (start == null) {
            start = new Vector3f(
                sx * TILE_SIZE + TILE_SIZE/2, sy * TILE_SIZE + TILE_SIZE/2,
                _bangobj.board.getElevation(sx, sy) * escale + TILE_SIZE/2);
        }
        Vector3f end = new Vector3f(
            tx * TILE_SIZE + TILE_SIZE/2, ty * TILE_SIZE + TILE_SIZE/2,
            _bangobj.board.getElevation(tx, ty) * escale + TILE_SIZE/2);
        PathParams pparams;
        Vector3f gravity;
        Vector3f velocity = end.subtract(start);
        float length = velocity.length();
        pparams = new PathParams(
            velocity.normalizeLocal().mult(FLAT_TRAJECTORY_SPEED),
            length / FLAT_TRAJECTORY_SPEED);
        gravity = Vector3f.ZERO;

        final ShotSprite ssprite = new ShotSprite(_ctx, _shot.getShotType(),
            (usprite == null) ? null : usprite.getColorizations());

        ssprite.setLocalTranslation(start);
        ssprite.addObserver(new RocketPathObserver(this, target, notePender(), _ctx, _view, _bangobj, (RocketEffect)_effect));
        _view.addSprite(ssprite);

        final float delay = (usprite != null) ?
            usprite.getRocketDelay() : 0f;
        ssprite.setCullMode(Spatial.CULL_ALWAYS);
        ssprite.move(new OrientingBallisticPath(ssprite,
            new Vector3f(1, 0, 0), start, pparams.velocity, gravity, pparams.duration) {
            public void update (float time) {
                if ((_daccum += time) < delay) {
                    return;
                }
                ssprite.setCullMode(Spatial.CULL_DYNAMIC);
                super.update(time);
            }
            protected float _daccum;
        });

        // play the launch sound if we have one
        if (_launchSound != null) {
            _launchSound.play(false);
        }
    }

    /**
     * Returns the duration of the sprite's action, or zero if the sprite has
     * no such action.
     */
    protected float getActionDuration (MobileSprite sprite, String action)
    {
        Model.Animation anim = sprite.getAction(action);
        return (anim == null) ? 0f : anim.getDuration();
    }

    protected RocketEffect _shot;
    protected Sound _launchSound;
}
