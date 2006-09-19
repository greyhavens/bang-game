//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.HashSet;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Spatial;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;

import com.samskivert.util.ArrayIntSet;

import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.effect.DamageIconViz;
import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.effect.IconViz;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.client.effect.PlaySoundViz;
import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.effect.WreckViz;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.Targetable;
import com.threerings.bang.game.client.sprite.TreeBedSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;

import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.HighNoonEffect;
import com.threerings.bang.game.data.effect.HoldEffect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ResurrectEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;
import java.util.ArrayList;

/**
 * Handles the visual representation of a complex effect on the client.
 */
public class EffectHandler extends BoardView.BoardAction
    implements Effect.Observer
{
    /** Initializes the handler. */
    public void init (
        BangContext ctx, BangObject bangobj, int pidx, BangBoardView view,
        BangView bview, SoundGroup sounds, Effect effect)
    {
        _ctx = ctx;
        _tick = bangobj.tick;
        _bangobj = bangobj;
        _pidx = pidx;
        _view = view;
        _bview = bview;
        _sounds = sounds;
        _effect = effect;
        pieceIds = effect.getAffectedPieces();
        waiterIds = effect.getWaitPieces();
        moveIds = effect.getMovePieces();
        bounds = effect.getBounds(_bangobj);

        // if this is a move effect, note the pending move
        for (int id : moveIds) {
            _view.notePendingMove(id);
        }
    }

    @Override // documentation inherited
    public boolean canExecute (
            ArrayIntSet penders, HashSet<Rectangle> boundset)
    {
        if (!super.canExecute(penders, boundset)) {
            return false;
        }
        boolean can = true;
        if (_effect instanceof MoveEffect) {
            ArrayList<EffectHandler> movers = _view.getPendingMovers();
            MoveEffect meffect = (MoveEffect)_effect;
            Piece piece = _bangobj.pieces.get(meffect.pieceId);
            if (piece == null) {
                return true;
            }
            byte state = _bangobj.board.shadowPieceTemp(
                    piece, meffect.nx, meffect.ny);
            for (EffectHandler handler : movers) {
                if (!(handler._effect instanceof MoveEffect)) {
                    continue;
                }
                MoveEffect move = (MoveEffect)handler._effect;
                Piece pender = _bangobj.pieces.get(move.pieceId);
                if (piece == null) {
                    continue;
                }
                if (_bangobj.board.computePath(
                        piece.x, piece.y, move.nx, move.ny, piece) == null) {
                    can = false;
                    break;
                }
            }
            _bangobj.board.clearShadowTemp(state, meffect.nx, meffect.ny);
        }
        return can;
    }

    @Override // documentation inherited
    public boolean execute ()
    {
        // don't allow the effect to complete until we're done applying
        // everything
        _applying = true;

        // applying the effect may result in calls to pieceAffected() which may
        // trigger visualizations or animations which we will then notice and
        // report that the view should wait
        apply(_effect);
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAdded (Piece piece)
    {
        _view.createPieceSprite(piece, _tick);
        if (piece instanceof Bonus && _dropper != null) {
            flyDroppedBonus(_dropper, _view.getPieceSprite(piece), false);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAffected (Piece piece, String effect)
    {
        // clear the pending moves we set earlier
        if (Effect.UPDATED.equals(effect)) {
            for (int id : moveIds) {
                _view.clearPendingMove(id);
            }
        }

        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            log.warning("Missing sprite for effect [piece=" + piece +
                        ", effect=" + effect + "].");
            return;
        }

        // create the appropriate visual effect
        boolean wasDamaged = false;
        EffectViz effviz = null;
        if (effect.equals(ShotEffect.DAMAGED)) {
            wasDamaged = true;
        } else if (effect.equals(ShotEffect.EXPLODED)) {
            wasDamaged = true;
            effviz = new ExplosionViz();
        } else if (effect.equals(RepairEffect.REPAIRED) ||
                   effect.equals(NuggetEffect.NUGGET_ADDED)) {
            effviz = new RepairViz();
        }

        // if they were damaged, go ahead and clear any pending shot
        if (wasDamaged && sprite instanceof Targetable) {
            ((Targetable)sprite).setPendingShot(false);
        }

        // display the damage icon/amount
        if (effect.equals(TreeBedEffect.GREW)) {
            DamageIconViz.displayDamageIconViz(piece,
                (piece.damage <= 50) ? "grew" : "repaired",
                TreeBedSprite.STATUS_COLOR,
                TreeBedSprite.DARKER_STATUS_COLOR, // cyan
                -_effect.getBaseDamage(piece), _effect, _ctx, _view);
        } else if (effect.equals(ShotEffect.DAMAGED) &&
            piece instanceof TreeBed) {
            DamageIconViz.displayDamageIconViz(piece,
                new ColorRGBA(1f, 0f, 1f, 1f),
                new ColorRGBA(0.5f, 0f, 0.5f, 1f), // magenta
                _effect, _ctx, _view);
        } else if (wasDamaged || effect.equals(ShotEffect.DUDDED)) {
            DamageIconViz.displayDamageIconViz(piece, _effect, _ctx, _view);
        }

        // possibly fly a totem piece off if one was destroyed
        if (wasDamaged && piece instanceof TotemBase) {
            maybeFlyTotemPiece((TotemBase)piece);
        }
        
        // add wreck effect for steam-powered units
        if (wasDamaged && piece instanceof Unit &&
            ((Unit)piece).getConfig().make == UnitConfig.Make.STEAM &&
            (effviz instanceof ExplosionViz || !piece.isAlive())) {
            effviz = new WreckViz(effviz);
        }

        // drop the piece from the sky
        if (effect.equals(AddPieceEffect.DROPPED)) {
            dropPiece(piece);
        }
        
        // queue the effect up on the piece sprite
        if (effviz != null) {
            queueEffect(sprite, piece, effviz);

        } else if (effect.equals(ShotEffect.ROTATED)) {
            // if we're rotating someone in preparation for a shot; just update
            // their orientation immediately for now
            ((MobileSprite)sprite).faceTarget();

        } else if (effect.equals(ShotEffect.SHOT_NOMOVE)) {
            // if they shot without moving, we'll need to clear out any advance
            // order now because we normally do it after they move
            _view.clearAdvanceOrder(piece.pieceId);
            // and update them to reset their tick display
            sprite.updated(piece, _tick);

        } else {
            // since we're not displaying an effect, we update immediately
            sprite.updated(piece, _tick);
        }

        // remember the last unit to drop a bonus (and cancel its advance
        // order, if any)
        if (effect.equals(HoldEffect.DROPPED_BONUS)) {
            _dropper = piece;
            if (((UnitSprite)sprite).getAdvanceOrder() !=
                    UnitSprite.AdvanceOrder.NONE) {
                _bview.getController().cancelOrder(piece.pieceId);
            }
        }

        // queue reacting, dying, or generic effects for active sprites
        if (sprite instanceof ActiveSprite) {
            ActiveSprite asprite = (ActiveSprite)sprite;
            if (wasDamaged) {
                if (piece.isAlive()) {
                    queueAction(asprite, "reacting");
                } else if (asprite.hasAction("dying")) {
                    queueAction(asprite,  "dying");
                } else {
                    // units with no dying animation just switch to their
                    // dead model; the wreck viz or explosion should hide the
                    // sudden transition
                    queueAction(asprite, ActiveSprite.DEAD);
                }
            } else if (asprite.hasAction(effect)) {
                queueAction(asprite, effect);
            }
        }

        // perhaps show an icon animation indicating what happened
        IconViz iviz = IconViz.createIconViz(piece, effect);
        if (iviz != null) {
            iviz.init(_ctx, _view, piece, null);
            iviz.display(sprite);
        }

        // perhaps display a generic particle effect
        if (BangPrefs.isMediumDetail() &&
            _ctx.getEffectCache().haveEffect(effect)) {
            sprite.displayParticles(effect, true);
        }

        // perhaps play a sound to go with our visual effect
        String soundPath = getSoundPath(effect);
        if (soundPath != null) {
            new PlaySoundViz(_sounds, soundPath).display(sprite);
        }

        // report the effect to the view who will report it to the tutorial
        // controller if appropriate
        _view.pieceWasAffected(piece, effect);
    }

    // documentation inherited from interface Effect.Observer
    public void boardAffected (String effect)
    {
        if (_ctx.getEffectCache().haveEffect(effect)) {
            _view.displayCameraParticles(effect, CAMERA_EFFECT_DURATION);
        } else if (HighNoonEffect.HIGH_NOON.equals(effect)) {
            _view.setHighNoon(true);
        } else if (effect == null && _view.isHighNoon()) {
            _view.setHighNoon(false);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceMoved (Piece piece)
    {
        // clear the pending moves we set earlier
        for (int id : moveIds) {
            _view.clearPendingMove(id);
        }

        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }

        // update the sprite (TODO: is this needed?)
        sprite.updated(piece, _tick);

        // let the board view know that this piece is on the move
        _view.pieceDidMove(piece);

        // and do the actual move
        if (!sprite.updatePosition(_bangobj.board)) {
            return;
        }

        final int penderId = ++_nextPenderId;
        _penders.add(penderId);
        sprite.addObserver(new PathObserver() {
            public void pathCancelled (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
            public void pathCompleted (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
        });
    }

    // documentation inherited from interface Effect.Observer
    public void pieceKilled (Piece piece)
    {
        _view.pieceWasKilled(piece.pieceId);
        // Allow non-ActiveSprites to update themselves when they're killed
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite != null && !(sprite instanceof ActiveSprite)) {
            sprite.updated(piece, _tick);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceRemoved (Piece piece)
    {
        log.info("Piece removed by effect " + piece + ".");
        _view.removePieceSprite(piece.pieceId, "deathByEffect");
    }

    // documentation inherited from interface Effect.Observer
    public void cardAdded (Card card)
    {
        _bview.pstatus[card.owner].cardAdded(card, true);
    }
    
    // documentation inherited from interface Effect.Observer
    public void cardRemoved (Card card)
    {
        _bview.pstatus[card.owner].cardRemoved(card, true, false);
    }
    
    // documentation inherited from interface Effect.Observer
    public void cardPlayed (Card card, Object target)
    {
        _bview.pstatus[card.owner].cardRemoved(card, false, true);
        if (!card.shouldShowVisualization(_pidx)) {
            return;
        }
        IconViz iviz;
        switch (card.getPlacementMode()) {
            case VS_PIECE:
                Piece piece = _bangobj.pieces.get((Integer)target);
                if (piece == null) {
                    log.warning("Missing piece for card played effect " +
                        "[effect=" + _effect + ", pieceId=" + target + "].");
                    return;
                }
                PieceSprite sprite = _view.getPieceSprite(piece);
                if (sprite == null) {
                    log.warning("Missing sprite for card played effect " +
                        "[effect=" + _effect + ", piece=" + piece + "].");
                    return;
                }
                iviz = IconViz.createCardViz(card);
                iviz.init(_ctx, _view, piece, null);
                iviz.display(sprite);
                return;
                
            case VS_AREA:
                int[] coords = (int[])target;
                iviz = IconViz.createCardViz(card);
                iviz.init(_ctx, _view, coords[0], coords[1], null);
                iviz.display(null);
                return;
        
            case VS_PLAYER:
                _bview.showCardPlayed(card, (Integer)target);
                return;
        
            case VS_BOARD:
                _bview.showCardPlayed(card, -1);
                return;
        }
    }
    
    // documentation inherited from interface Effect.Observer
    public void tickDelayed (long extraTime)
    {
    }

    @Override // documentation inherited
    public String toString ()
    {
        return super.toString() + ":" + _effect;
    }

    protected void apply (Effect effect)
    {
        effect.apply(_bangobj, this);
        String desc = effect.getDescription(_bangobj, _pidx);
        if (desc != null) {
            _ctx.getChatDirector().displayInfo(GameCodes.GAME_MSGS, desc);
        }
    }

    protected void queueEffect (
        final PieceSprite sprite, final Piece piece, EffectViz viz)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this +
                     " [viz=" + viz + ", pid=" + penderId + "].");
        }
        viz.init(_ctx, _view, piece, new EffectViz.Observer() {
            public void effectDisplayed () {
                sprite.updated(piece, _tick);
                maybeComplete(penderId);
            }
        });
        viz.display(sprite);
    }

    /**
     * Checks for an activation sound for the specified effect in the various
     * places it might exist.
     */
    protected String getSoundPath (String effect)
    {
        for (int pp = 0; pp < SOUND_PREFIXES.length; pp++) {
            for (int ss = 0; ss < SOUND_SUFFIXES.length; ss++) {
                String path = SOUND_PREFIXES[pp] + effect + SOUND_SUFFIXES[ss];
                if (SoundUtil.haveSound(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * Queues up an action on a sprite and sets up the necessary observation to
     * ensure that we wait until the action is completed to complete our
     * effect.
     */
    protected void queueAction (ActiveSprite sprite, String action)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this +
                     " [action=" + action + ", pid=" + penderId + "].");
        }
        sprite.addObserver(new ActiveSprite.ActionObserver() {
            public void actionCompleted (Sprite sprite, String action) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
        });
        sprite.queueAction(action);
    }

    /**
     * Loads up the appropriate sound for the specified unit firing the
     * specified kind of shot.
     */
    protected Sound getShotSound (
        SoundGroup sounds, String unitType, int shotType)
    {
        // no sound for collateral damage shot; the main shot will produce a
        // sound
        if (shotType != ShotEffect.COLLATERAL_DAMAGE) {
            String path = "rsrc/units/" + unitType + "/" +
                ShotEffect.SHOT_ACTIONS[shotType] + ".wav";
            // TODO: fall back to a generic sound if we don't have a
            // special sound for this unit for this shot type
            if (SoundUtil.haveSound(path)) {
                return sounds.getSound(path);
            }
            // TODO: go back to complaining if we don't have shot sounds
        }
        return null;
    }

    /**
     * If a totem piece has been destroyed, flies it from the totem base onto
     * the board, bouncing it a few times before allowing it to sink into the
     * ground.
     */
    protected void maybeFlyTotemPiece (TotemBase base)
    {
        TotemBonus.Type type = base.getDestroyedType();
        if (type == null) {
            return;
        }
        PieceSprite bsprite = _view.getPieceSprite(base);
        if (bsprite == null) {
            return;
        }
        // create a dummy piece sprite at an occupiable spot
        Point spot = _bangobj.board.getOccupiableSpot(base.x, base.y, 3);
        if (spot == null) {
            return;
        }
        Bonus dummy = Bonus.createBonus(type.bonus());
        dummy.pieceId = -1;
        dummy.owner = base.getDestroyedOwner();
        dummy.position(spot.x, spot.y);
        PieceSprite sprite = dummy.createSprite();
        sprite.init(_ctx, _view, _bangobj.board, _sounds, dummy, _tick);
        _view.addSprite(sprite);
        
        // fly the sprite from the totem to its position
        final int penderId = notePender();
        Vector3f btrans = new Vector3f(bsprite.getWorldTranslation());
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(
                btrans, sprite.getWorldTranslation());
        sprite.move(new BallisticPath(sprite, btrans, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void wasRemoved () {
                super.wasRemoved();
                bounceSprite(_sprite, TILE_SIZE / 4);
                maybeComplete(penderId);
            }
        });
    }
    
    /**
     * Flies a dropped bonus from the unit that dropped it to its current
     * location (optionally fading it in), then bounces it a few times.
     */
    protected void flyDroppedBonus (
        Piece dropper, PieceSprite sprite, final boolean fadeIn)
    {
        flyDroppedBonus(getHoldingTranslation(dropper), sprite, fadeIn);
    }

    /**
     * Flies a dropped bonus from the unit that dropped it to its current
     * location (optionally fading it in), then bounces it a few times.
     */
    protected void flyDroppedBonus (
        Vector3f htrans, PieceSprite sprite, final boolean fadeIn)
    {
        if (sprite == null) {
            return;
        }
        final MaterialState mstate =
            (MaterialState)sprite.getRenderState(RenderState.RS_MATERIAL);
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(
                htrans, sprite.getWorldTranslation());
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, htrans, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void update (float time) {
                super.update(time);
                float alpha = Math.min(_accum / _duration, 1f);
                _sprite.setLocalScale(FastMath.LERP(alpha, 0.5f, 1f));
                if (fadeIn) {
                    mstate.getDiffuse().a = alpha;
                }
            }
            public void wasRemoved () {
                super.wasRemoved();
                bounceSprite(_sprite, TILE_SIZE / 4);
                maybeComplete(penderId);
            }
        });
        sprite.updateGeometricState(0f, true);
    }

    /**
     * Computes and returns the location where bonuses are held for the
     * specified piece, which is one tile length above its base.
     */
    protected Vector3f getHoldingTranslation (Piece piece)
    {
        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return new Vector3f();
        }
        Vector3f trans = new Vector3f(0f, 0f, TILE_SIZE);
        sprite.getLocalRotation().multLocal(trans);
        trans.addLocal(sprite.getWorldTranslation());
        return trans;
    }
    
    /**
     * Drops a piece from the sky to its starting location.
     */
    protected void dropPiece (final Piece piece)
    {
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }
        Vector3f start = new Vector3f(sprite.getWorldTranslation());
        start.z += PIECE_DROP_HEIGHT;
        float duration = FastMath.sqrt(
            -2f * PIECE_DROP_HEIGHT / BallisticShotHandler.GRAVITY);
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, start, new Vector3f(),
            BallisticShotHandler.GRAVITY_VECTOR, duration) {
            public void wasRemoved () {
                super.wasRemoved();
                sprite.setLocation(piece.x, piece.y, piece.computeElevation(
                    _bangobj.board, piece.x, piece.y));
                sprite.updateWorldVectors();
                if (BangPrefs.isHighDetail()) {
                    sprite.displayDustRing();
                }
                bounceSprite(_sprite, TILE_SIZE / 4);
                maybeComplete(penderId);
            }
        });
        if (piece instanceof LoggingRobot && sprite.getModelNode() != null) {
            // pause on the first frame of the unfolding animation
            sprite.getModelNode().startAnimation("unfolding");
            sprite.getModelNode().pauseAnimation(true);
        }
    }
    
    /**
     * Bounces a sprite up to the specified height and schedules another
     * bounce.
     */
    protected void bounceSprite (Sprite sprite, final float height)
    {
        Vector3f start = new Vector3f(sprite.getWorldTranslation());
        float duration = FastMath.sqrt(
            -2f * height / BallisticShotHandler.GRAVITY),
            vel = -BallisticShotHandler.GRAVITY * duration;
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, start, new Vector3f(0f, 0f, vel),
            BallisticShotHandler.GRAVITY_VECTOR, duration*2) {
            public void wasRemoved () {
                super.wasRemoved();
                if (height >= TILE_SIZE / 16) {
                    bounceSprite(_sprite, height / 2);
                } else {
                    spriteStoppedBouncing(_sprite);
                }
                maybeComplete(penderId);
            }
        }); 
    }
    
    /**
     * Called when the sprite finishes its final bounce.
     */
    protected void spriteStoppedBouncing (Sprite sprite)
    {
        // fade dummy bonuses into the ground and remove them after they finish
        // bouncing
        if (sprite instanceof BonusSprite) {
            BonusSprite bsprite = (BonusSprite)sprite;
            if (bsprite.getPiece().pieceId == -1) {
                queueAction(bsprite, ActiveSprite.REMOVED);
                bsprite.addObserver(new ActiveSprite.ActionObserver() {
                    public void actionCompleted (Sprite sprite, String action) {
                        _view.removeSprite(sprite);
                    }
                });
            }
        } else if (sprite instanceof UnitSprite &&
            ((UnitSprite)sprite).getPiece() instanceof LoggingRobot) {
            ((UnitSprite)sprite).queueAction("unfolding");   
        }
    }
     
    protected boolean isCompleted ()
    {
        return (!_applying && _penders.size() == 0);
    }

    protected int notePender ()
    {
        _penders.add(++_nextPenderId);
        return _nextPenderId;
    }

    protected void maybeComplete (int penderId)
    {
        _penders.remove(penderId);
        if (isCompleted()) {
            if (BoardView.ACTION_DEBUG) {
                log.info("Completing " + this);
            }
            _view.actionCompleted(this);
        } else {
            if (BoardView.ACTION_DEBUG) {
                log.info("Not completing " + this +
                         " [penders=" + _penders + "].");
            }
        }
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected int _pidx;
    protected BangBoardView _view;
    protected BangView _bview;
    protected short _tick;
    protected boolean _applying;

    protected SoundGroup _sounds;

    protected Effect _effect;

    protected ArrayIntSet _penders = new ArrayIntSet();
    protected int _nextPenderId;

    /** The last piece to drop a bonus. */
    protected Piece _dropper;

    /** The duration of particle effects activated on top of the camera. */
    protected static final float CAMERA_EFFECT_DURATION = 2f;
    
    /** The height from which to drop pieces onto the board. */
    protected static final float PIECE_DROP_HEIGHT = 150f;
    
    /** Used by {@link #getSoundPath}. */
    protected static final String[] SOUND_PREFIXES = {
        "rsrc/cards/", "rsrc/bonuses/", "rsrc/extras/", "rsrc/effects/"
    };

    /** Used by {@link #getSoundPath}. */
    protected static final String[] SOUND_SUFFIXES = {
        ".wav", "/activate.wav"
    };
}
